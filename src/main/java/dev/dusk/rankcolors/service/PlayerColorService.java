package dev.dusk.rankcolors.service;

import dev.dusk.rankcolors.color.ColorCategory;
import dev.dusk.rankcolors.api.DisplayContext;
import dev.dusk.rankcolors.color.ColorFormatter;
import dev.dusk.rankcolors.color.PlayerColorSelection;
import dev.dusk.rankcolors.color.PlayerColorSettings;
import dev.dusk.rankcolors.config.PluginConfiguration;
import dev.dusk.rankcolors.event.PlayerColorChangeEvent;
import dev.dusk.rankcolors.rank.RankDefinition;
import dev.dusk.rankcolors.rank.RankRegistry;
import dev.dusk.rankcolors.scheduler.SchedulerAdapter;
import dev.dusk.rankcolors.storage.PlayerColorRepository;
import dev.dusk.rankcolors.util.ComponentTemplate;
import dev.dusk.rankcolors.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PlayerColorService {
    private final Plugin plugin;
    private final PluginConfiguration configuration;
    private final PlayerColorRepository repository;
    private final ColorFormatter formatter;
    private final SelectionValidator validator;
    private final SchedulerAdapter scheduler;
    private final RankRegistry ranks;
    private final Map<UUID, PlayerColorSettings> cache = new ConcurrentHashMap<>();
    private final Map<UUID, RenderedPlayer> rendered = new ConcurrentHashMap<>();
    private volatile Object rankWatcherToken;

    public PlayerColorService(Plugin plugin, PluginConfiguration configuration, PlayerColorRepository repository,
                              ColorFormatter formatter, SelectionValidator validator, SchedulerAdapter scheduler,
                              RankRegistry ranks) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.repository = repository;
        this.formatter = formatter;
        this.validator = validator;
        this.scheduler = scheduler;
        this.ranks = ranks;
    }

    public void load(Player player) {
        scheduler.runForPlayer(player, () -> {
            RankDefinition rank = ranks.resolve(player);
            PlayerColorSettings rankDefaults = defaults(rank);
            PlayerColorSettings settings = repository.load(player, rankDefaults);
            settings = revalidate(player, settings);
            cache.put(player.getUniqueId(), settings);
            rebuild(player, settings, rank);
        });
    }

    public void reload(Player player) {
        scheduler.runForPlayer(player, () -> {
            RankDefinition rank = ranks.resolve(player);
            PlayerColorSettings settings = revalidate(player, repository.load(player, defaults(rank)));
            cache.put(player.getUniqueId(), settings);
            rebuild(player, settings, rank);
        });
    }

    /** Refreshes the cached rank only when the player's rank permission result changed. */
    public void synchronizeRank(Player player, Runnable callback) {
        scheduler.runForPlayer(player, () -> {
            synchronizeRankNow(player);
            callback.run();
        });
    }

    private void synchronizeRankNow(Player player) {
        RankDefinition rank = ranks.resolve(player);
        RenderedPlayer current = rendered.get(player.getUniqueId());
        if (current != null && current.rankId().equals(rank.id())) return;
        PlayerColorSettings settings = revalidate(player, repository.load(player, defaults(rank)));
        cache.put(player.getUniqueId(), settings);
        rebuild(player, settings, rank);
    }

    public void restartRankWatcher() {
        Object token = new Object();
        rankWatcherToken = token;
        scheduleRankWatcher(token);
    }

    private void scheduleRankWatcher(Object token) {
        int refreshTicks = configuration.rankRefreshTicks();
        if (refreshTicks <= 0) {
            if (rankWatcherToken == token) rankWatcherToken = null;
            return;
        }
        scheduler.runGlobalLater(refreshTicks, () -> {
            if (rankWatcherToken != token) return;
            Bukkit.getOnlinePlayers().forEach(player -> scheduler.runForPlayer(player, () -> {
                if (player.isOnline() && rankWatcherToken == token) synchronizeRankNow(player);
            }));
            scheduleRankWatcher(token);
        });
    }

    private PlayerColorSettings revalidate(Player player, PlayerColorSettings settings) {
        if (!configuration.fallbackInvalidSelection()) return settings;
        PlayerColorSettings current = settings;
        PlayerColorSettings effectiveDefaults = defaults(ranks.resolve(player));
        for (ColorCategory category : ColorCategory.values()) {
            boolean bypass = !configuration.validateStoredPermissions();
            if (current.selection(category).equals(effectiveDefaults.selection(category))) continue;
            if (validator.validate(player, category, current.selection(category), bypass) != SelectionValidator.Result.OK) {
                current = current.with(category, effectiveDefaults.selection(category));
                repository.reset(player, category);
            }
        }
        return current;
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
        rendered.remove(uuid);
    }

    public void clear() {
        rankWatcherToken = null;
        cache.clear();
        rendered.clear();
    }

    public int cachedPlayers() {
        return cache.size();
    }

    public PlayerColorSettings settings(UUID uuid) {
        return cache.getOrDefault(uuid, defaults());
    }

    public RenderedPlayer rendered(UUID uuid) {
        return rendered.get(uuid);
    }

    public void setSelection(Player player, ColorCategory category, PlayerColorSelection selection,
                             boolean bypassPermission, Consumer<SelectionValidator.Result> callback) {
        scheduler.runForPlayer(player, () -> {
            SelectionValidator.Result validation = validator.validate(player, category, selection, bypassPermission);
            if (validation != SelectionValidator.Result.OK) {
                callback.accept(validation);
                return;
            }
            RankDefinition rank = ranks.resolve(player);
            PlayerColorSettings oldSettings = cache.getOrDefault(player.getUniqueId(), defaults(rank));
            PlayerColorSelection oldSelection = oldSettings.selection(category);
            PlayerColorChangeEvent event = new PlayerColorChangeEvent(player, category, oldSelection, selection);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                callback.accept(SelectionValidator.Result.CANCELLED);
                return;
            }
            PlayerColorSettings newSettings = oldSettings.with(category, selection);
            repository.save(player, category, selection);
            cache.put(player.getUniqueId(), newSettings);
            rebuild(player, newSettings, rank);
            callback.accept(SelectionValidator.Result.OK);
        }, () -> callback.accept(SelectionValidator.Result.CANCELLED));
    }

    public void reset(Player player, ColorCategory category, Consumer<Boolean> callback) {
        scheduler.runForPlayer(player, () -> {
            RankDefinition rank = ranks.resolve(player);
            PlayerColorSettings oldSettings = cache.getOrDefault(player.getUniqueId(), defaults(rank));
            PlayerColorSelection replacement = defaults(rank).selection(category);
            PlayerColorChangeEvent event = new PlayerColorChangeEvent(player, category, oldSettings.selection(category), replacement);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                callback.accept(false);
                return;
            }
            repository.reset(player, category);
            PlayerColorSettings updated = oldSettings.with(category, replacement);
            cache.put(player.getUniqueId(), updated);
            rebuild(player, updated, rank);
            callback.accept(true);
        }, () -> callback.accept(false));
    }

    public void resetAll(Player player, Consumer<Boolean> callback) {
        scheduler.runForPlayer(player, () -> {
            RankDefinition rank = ranks.resolve(player);
            PlayerColorSettings oldSettings = cache.getOrDefault(player.getUniqueId(), defaults(rank));
            PlayerColorSettings settings = defaults(rank);
            for (ColorCategory category : ColorCategory.values()) {
                PlayerColorChangeEvent event = new PlayerColorChangeEvent(player, category,
                    oldSettings.selection(category), settings.selection(category));
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    callback.accept(false);
                    return;
                }
            }
            for (ColorCategory category : ColorCategory.values()) repository.reset(player, category);
            cache.put(player.getUniqueId(), settings);
            rebuild(player, settings, rank);
            callback.accept(true);
        }, () -> callback.accept(false));
    }

    private void rebuild(Player player, PlayerColorSettings settings, RankDefinition definition) {
        String rankText = definition.display();
        boolean hasRank = !rankText.isBlank();
        String wrappedRank = hasRank ? configuration.rankWrapper().replace("{rank}", rankText) : "";
        Component rank = hasRank
            ? formatter.format(wrappedRank, settings.selection(ColorCategory.RANK)) : Component.empty();
        Component plus = hasRank && definition.plus()
            ? formatter.format(configuration.plusSymbol(), settings.selection(ColorCategory.PLUS)) : Component.empty();
        if (definition.bold()) {
            rank = rank.decoration(TextDecoration.BOLD, true);
            plus = plus.decoration(TextDecoration.BOLD, true);
        }
        Component name = formatter.format(player.getName(), settings.selection(ColorCategory.NAME));
        Component full = compose(configuration.fullFormat(), hasRank, rank, plus, name);
        EnumMap<DisplayContext, Component> contexts = new EnumMap<>(DisplayContext.class);
        EnumMap<DisplayContext, String> contextLegacy = new EnumMap<>(DisplayContext.class);
        for (DisplayContext context : DisplayContext.values()) {
            Component renderedContext = compose(configuration.contextFormat(context), hasRank, rank, plus, name);
            contexts.put(context, renderedContext);
            contextLegacy.put(context, Text.section(renderedContext));
        }
        EnumMap<ColorCategory, ColorFormatter.SelectionView> views = new EnumMap<>(ColorCategory.class);
        for (ColorCategory category : ColorCategory.values()) views.put(category, formatter.describe(settings.selection(category)));
        RenderedPlayer value = new RenderedPlayer(settings, definition.id(), rankText, definition.permission(), definition.priority(),
            hasRank && definition.plus(), hasRank && definition.bold(),
            rank, plus, name, full,
            Text.section(rank), Text.section(plus), Text.section(name), Text.section(full), Map.copyOf(views),
            Map.copyOf(contexts), Map.copyOf(contextLegacy));
        rendered.put(player.getUniqueId(), value);
        if (configuration.applyDisplayName()) player.displayName(contexts.get(DisplayContext.CHAT));
        if (configuration.applyPlayerListName()) player.playerListName(contexts.get(DisplayContext.TAB));
    }

    private PlayerColorSettings defaults() {
        return new PlayerColorSettings(configuration.defaultSelection(ColorCategory.RANK),
            configuration.defaultSelection(ColorCategory.PLUS), configuration.defaultSelection(ColorCategory.NAME));
    }

    private PlayerColorSettings defaults(RankDefinition rank) {
        return new PlayerColorSettings(rank.rankSelection(), rank.plusSelection(),
            configuration.defaultSelection(ColorCategory.NAME));
    }

    private Component compose(String template, boolean hasRank, Component rank, Component plus, Component name) {
        String effective = hasRank ? template : template.replace("{rank}", "").replace("{plus}", "")
            .replaceAll("[ \\t]{2,}", " ").strip();
        return ComponentTemplate.compose(effective, Map.of("rank", rank, "plus", plus, "name", name));
    }
}
