package dev.dusk.rankcolors.rank;

import dev.dusk.rankcolors.config.ConfigManager;
import dev.dusk.rankcolors.color.ColorCategory;
import dev.dusk.rankcolors.color.ColorMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;

/** Config-driven rank registry. Assignment is permission-based and does not read permission-plugin groups. */
public final class RankRegistry {
    private static final RankDefinition NONE = new RankDefinition(
        "", "", "", Integer.MIN_VALUE, "#FFFFFF", false, "#FFFFFF", false, Map.of(), Map.of());

    private final Plugin plugin;
    private final ConfigManager configs;
    private final Logger logger;
    private final Map<String, Permission> registeredPermissions = new HashMap<>();
    private volatile List<RankDefinition> ranks = List.of();

    public RankRegistry(Plugin plugin, ConfigManager configs, Logger logger) {
        this.plugin = plugin;
        this.configs = configs;
        this.logger = logger;
    }

    public void reload() {
        unregisterPermissions();
        ConfigurationSection section = configs.config().getConfigurationSection("ranks");
        if (section == null) {
            logger.warning("config.yml has no 'ranks' section; players without a configured rank will show only their name.");
            ranks = List.of();
            return;
        }

        List<RankDefinition> loaded = new ArrayList<>();
        int order = 0;
        for (String rawId : section.getKeys(false)) {
            String root = "ranks." + rawId;
            String id = rawId.toLowerCase(Locale.ROOT);
            String rankColor = color(root + ".color", "#FFFFFF", id);
            String plusColor = color(root + ".plus-color", rankColor, id);
            String configuredPermission = configs.config().getString(root + ".permission");
            String permission = configuredPermission == null || configuredPermission.isBlank()
                ? "duskrankcolors.ranks." + id : configuredPermission.trim();
            int priority = configs.config().contains(root + ".priority", true)
                ? configs.config().getInt(root + ".priority")
                : configs.config().getInt(root + ".weight", order);
            loaded.add(new RankDefinition(id,
                configs.config().getString(root + ".display", rawId),
                permission,
                priority,
                rankColor,
                configs.config().getBoolean(root + ".plus", false),
                plusColor,
                configs.config().getBoolean(root + ".bold", false),
                allowedModes(root),
                allowedColors(root)));
            order++;
        }
        ranks = ordered(loaded);
        registerPermissions(ranks);
    }

    public RankDefinition resolve(Player player) {
        return resolve(ranks, player::hasPermission);
    }

    public int size() {
        return ranks.size();
    }

    public List<RankDefinition> all() {
        return ranks;
    }

    public void shutdown() {
        unregisterPermissions();
    }

    static List<RankDefinition> ordered(List<RankDefinition> definitions) {
        return definitions.stream().sorted(Comparator.comparingInt(RankDefinition::priority).reversed()).toList();
    }

    static RankDefinition resolve(List<RankDefinition> definitions, Predicate<String> hasPermission) {
        for (RankDefinition rank : definitions) {
            if (rank.permission().isBlank() || hasPermission.test(rank.permission())) return rank;
        }
        return NONE;
    }

    private void registerPermissions(List<RankDefinition> definitions) {
        for (RankDefinition rank : definitions) {
            String node = rank.permission();
            if (node.isBlank() || registeredPermissions.containsKey(node)
                || plugin.getServer().getPluginManager().getPermission(node) != null) continue;
            Permission permission = new Permission(node,
                "Selects the DuskRankColors rank '" + rank.id() + "'.", PermissionDefault.FALSE);
            plugin.getServer().getPluginManager().addPermission(permission);
            registeredPermissions.put(node, permission);
        }
    }

    private void unregisterPermissions() {
        registeredPermissions.forEach((node, permission) -> {
            if (plugin.getServer().getPluginManager().getPermission(node) == permission) {
                plugin.getServer().getPluginManager().removePermission(permission);
            }
        });
        registeredPermissions.clear();
    }

    private String color(String path, String fallback, String rankId) {
        String raw = configs.config().getString(path);
        if (raw == null || raw.isBlank()) return fallback;
        return RankColorCode.normalize(raw).orElseGet(() -> {
            logger.warning("Invalid color '" + raw + "' for rank '" + rankId + "' at " + path + "; using " + fallback + ".");
            return fallback;
        });
    }

    private Map<ColorCategory, Set<ColorMode>> allowedModes(String root) {
        EnumMap<ColorCategory, Set<ColorMode>> result = new EnumMap<>(ColorCategory.class);
        for (ColorCategory category : ColorCategory.values()) {
            String path = root + ".allowed-modes." + category.key();
            if (!configs.config().isList(path)) continue;
            EnumSet<ColorMode> modes = EnumSet.noneOf(ColorMode.class);
            for (String value : configs.config().getStringList(path)) {
                ColorMode.parse(value).ifPresentOrElse(modes::add,
                    () -> logger.warning("Unknown color mode '" + value + "' at " + path + "."));
            }
            result.put(category, modes);
        }
        return result;
    }

    private Map<ColorCategory, Set<String>> allowedColors(String root) {
        EnumMap<ColorCategory, Set<String>> result = new EnumMap<>(ColorCategory.class);
        for (ColorCategory category : ColorCategory.values()) {
            String path = root + ".allowed-colors." + category.key();
            if (!configs.config().isList(path)) continue;
            Set<String> colors = new java.util.LinkedHashSet<>();
            for (String value : configs.config().getStringList(path)) {
                if (!value.isBlank()) colors.add(value.toLowerCase(Locale.ROOT));
            }
            result.put(category, colors);
        }
        return result;
    }
}
