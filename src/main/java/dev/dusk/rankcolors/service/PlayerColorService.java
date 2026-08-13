package dev.dusk.rankcolors.service;

import dev.dusk.rankcolors.color.ColorCategory;
import dev.dusk.rankcolors.color.ColorFormatter;
import dev.dusk.rankcolors.color.PlayerColorSelection;
import dev.dusk.rankcolors.color.PlayerColorSettings;
import dev.dusk.rankcolors.config.PluginConfiguration;
import dev.dusk.rankcolors.event.PlayerColorChangeEvent;
import dev.dusk.rankcolors.hook.RankProvider;
import dev.dusk.rankcolors.scheduler.SchedulerAdapter;
import dev.dusk.rankcolors.storage.PlayerColorRepository;
import dev.dusk.rankcolors.util.Text;
import net.kyori.adventure.text.Component;
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
    private final RankProvider rankProvider;
    private final Map<UUID, PlayerColorSettings> cache = new ConcurrentHashMap<>();
    private final Map<UUID, RenderedPlayer> rendered = new ConcurrentHashMap<>();

    public PlayerColorService(Plugin plugin, PluginConfiguration configuration, PlayerColorRepository repository,
                              ColorFormatter formatter, SelectionValidator validator, SchedulerAdapter scheduler,
                              RankProvider rankProvider) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.repository = repository;
        this.formatter = formatter;
        this.validator = validator;
        this.scheduler = scheduler;
        this.rankProvider = rankProvider;
    }

    public void load(Player player) {
        scheduler.runForPlayer(player, () -> {
            PlayerColorSettings settings = repository.load(player);
            settings = revalidate(player, settings);
            cache.put(player.getUniqueId(), settings);
            rebuild(player, settings);
        });
    }

    public void reload(Player player) {
        scheduler.runForPlayer(player, () -> {
            PlayerColorSettings settings = revalidate(player, cache.getOrDefault(player.getUniqueId(), defaults()));
            cache.put(player.getUniqueId(), settings);
            rebuild(player, settings);
        });
    }

    private PlayerColorSettings revalidate(Player player, PlayerColorSettings settings) {
        if (!configuration.fallbackInvalidSelection()) return settings;
        PlayerColorSettings current = settings;
        for (ColorCategory category : ColorCategory.values()) {
            boolean bypass = !configuration.validateStoredPermissions();
            if (validator.validate(player, category, current.selection(category), bypass) != SelectionValidator.Result.OK) {
                current = current.with(category, configuration.defaultSelection(category));
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
            PlayerColorSettings oldSettings = cache.getOrDefault(player.getUniqueId(), defaults());
            PlayerColorSelection oldSelection = oldSettings.selection(category);
            PlayerColorChangeEvent event = new PlayerColorChangeEvent(player, category, oldSelection, selection);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                callback.accept(SelectionValidator.Result.INVALID);
                return;
            }
            PlayerColorSettings newSettings = oldSettings.with(category, selection);
            repository.save(player, category, selection);
            cache.put(player.getUniqueId(), newSettings);
            rebuild(player, newSettings);
            callback.accept(SelectionValidator.Result.OK);
        });
    }

    public void reset(Player player, ColorCategory category, Consumer<Boolean> callback) {
        scheduler.runForPlayer(player, () -> {
            PlayerColorSettings oldSettings = cache.getOrDefault(player.getUniqueId(), defaults());
            PlayerColorSelection replacement = configuration.defaultSelection(category);
            PlayerColorChangeEvent event = new PlayerColorChangeEvent(player, category, oldSettings.selection(category), replacement);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                callback.accept(false);
                return;
            }
            repository.reset(player, category);
            PlayerColorSettings updated = oldSettings.with(category, replacement);
            cache.put(player.getUniqueId(), updated);
            rebuild(player, updated);
            callback.accept(true);
        });
    }

    public void resetAll(Player player, Consumer<Boolean> callback) {
        scheduler.runForPlayer(player, () -> {
            for (ColorCategory category : ColorCategory.values()) repository.reset(player, category);
            PlayerColorSettings settings = defaults();
            cache.put(player.getUniqueId(), settings);
            rebuild(player, settings);
            callback.accept(true);
        });
    }

    private void rebuild(Player player, PlayerColorSettings settings) {
        String group = rankProvider.primaryGroup(player.getUniqueId());
        String rankText = configuration.rankText(group);
        String wrappedRank = configuration.rankWrapper().replace("{rank}", rankText);
        Component rank = formatter.format(wrappedRank, settings.selection(ColorCategory.RANK));
        Component plus = formatter.format(configuration.plusSymbol(), settings.selection(ColorCategory.PLUS));
        Component name = formatter.format(player.getName(), settings.selection(ColorCategory.NAME));
        Component full = compose(configuration.fullFormat(), rank, plus, name);
        EnumMap<ColorCategory, ColorFormatter.SelectionView> views = new EnumMap<>(ColorCategory.class);
        for (ColorCategory category : ColorCategory.values()) views.put(category, formatter.describe(settings.selection(category)));
        RenderedPlayer value = new RenderedPlayer(settings, rankText, rank, plus, name, full,
            Text.section(rank), Text.section(plus), Text.section(name), Text.section(full), Map.copyOf(views));
        rendered.put(player.getUniqueId(), value);
        if (configuration.applyDisplayName()) player.displayName(name);
        if (configuration.applyPlayerListName()) player.playerListName(name);
    }

    private Component compose(String template, Component rank, Component plus, Component name) {
        Component result = Component.empty();
        int cursor = 0;
        while (cursor < template.length()) {
            int next = template.indexOf('{', cursor);
            if (next < 0) return result.append(Component.text(template.substring(cursor)));
            if (next > cursor) result = result.append(Component.text(template.substring(cursor, next)));
            if (template.startsWith("{rank}", next)) {
                result = result.append(rank);
                cursor = next + 6;
            } else if (template.startsWith("{plus}", next)) {
                result = result.append(plus);
                cursor = next + 6;
            } else if (template.startsWith("{name}", next)) {
                result = result.append(name);
                cursor = next + 6;
            } else {
                result = result.append(Component.text("{"));
                cursor = next + 1;
            }
        }
        return result;
    }

    private PlayerColorSettings defaults() {
        return new PlayerColorSettings(configuration.defaultSelection(ColorCategory.RANK),
            configuration.defaultSelection(ColorCategory.PLUS), configuration.defaultSelection(ColorCategory.NAME));
    }
}
