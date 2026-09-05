package dev.dusk.rankcolors.config;

import dev.dusk.rankcolors.color.ColorCategory;
import dev.dusk.rankcolors.color.ColorMode;
import dev.dusk.rankcolors.color.HexColor;
import dev.dusk.rankcolors.rank.RankColorCode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConfigValidator {
    private static final Pattern FORMAT_TOKEN = Pattern.compile("\\{([^{}]+)}");
    private final ConfigManager configs;

    public ConfigValidator(ConfigManager configs) {
        this.configs = configs;
    }

    public Report validate() {
        List<Issue> issues = new ArrayList<>();
        validateRanks(issues);
        validateColors(issues);
        validateDefaults(issues);
        validateFormats(issues);
        validateMaterials(issues);
        validateMenuLayouts(issues);
        validateSounds(issues);
        return new Report(List.copyOf(issues));
    }

    private void validateRanks(List<Issue> issues) {
        ConfigurationSection ranks = configs.config().getConfigurationSection("ranks");
        if (ranks == null || ranks.getKeys(false).isEmpty()) {
            issues.add(new Issue(Severity.WARNING, "config.yml:ranks", "No ranks are configured."));
            return;
        }
        Set<String> ids = new HashSet<>();
        Map<Integer, String> priorities = new HashMap<>();
        Map<String, String> permissions = new HashMap<>();
        Set<String> presetIds = colorIds();
        Set<String> enabledPresetIds = enabledColorIds();
        int order = 0;
        for (String rawId : ranks.getKeys(false)) {
            String id = rawId.toLowerCase(Locale.ROOT);
            String root = "ranks." + rawId;
            if (!ids.add(id)) issue(issues, Severity.ERROR, root, "Duplicate rank ID ignoring letter case.");
            if (configs.config().getString(root + ".display", "").isBlank()) {
                issue(issues, Severity.ERROR, root + ".display", "Rank display cannot be empty.");
            }
            Object configuredPriority = configs.config().get(root + ".priority");
            if (configuredPriority != null && (!(configuredPriority instanceof Number number)
                || number.doubleValue() != number.intValue())) {
                issue(issues, Severity.ERROR, root + ".priority", "Priority must be an integer.");
            }
            int priority = configuredPriority instanceof Number number ? number.intValue()
                : configs.config().getInt(root + ".weight", order);
            order++;
            String previous = priorities.putIfAbsent(priority, rawId);
            if (previous != null) {
                issue(issues, Severity.WARNING, root + ".priority",
                    "Priority " + priority + " is already used by rank '" + previous + "'.");
            }
            validateRankColor(issues, root + ".color", false);
            validateRankColor(issues, root + ".plus-color", true);
            if (configs.config().contains(root + ".permission", true)
                && configs.config().getString(root + ".permission", "").isBlank()) {
                issue(issues, Severity.WARNING, root + ".permission",
                    "Empty permission; the automatic duskrankcolors.ranks." + id + " node will be used.");
            }
            String permission = configs.config().getString(root + ".permission", "").trim();
            if (permission.isBlank()) permission = "duskrankcolors.ranks." + id;
            String permissionOwner = permissions.putIfAbsent(permission.toLowerCase(Locale.ROOT), rawId);
            if (permissionOwner != null) {
                issue(issues, Severity.WARNING, root + ".permission",
                    "This permission is also used by rank '" + permissionOwner + "'; the lower-priority rank may never win.");
            }
            for (ColorCategory category : ColorCategory.values()) {
                String modesPath = root + ".allowed-modes." + category.key();
                if (configs.config().contains(modesPath, true) && !configs.config().isList(modesPath)) {
                    issue(issues, Severity.ERROR, modesPath, "Expected a YAML list.");
                } else {
                    for (String mode : configs.config().getStringList(modesPath)) {
                        if (ColorMode.parse(mode).isEmpty()) issue(issues, Severity.ERROR, modesPath, "Unknown mode '" + mode + "'.");
                    }
                }
                String colorsPath = root + ".allowed-colors." + category.key();
                if (configs.config().contains(colorsPath, true) && !configs.config().isList(colorsPath)) {
                    issue(issues, Severity.ERROR, colorsPath, "Expected a YAML list.");
                } else {
                    for (String color : configs.config().getStringList(colorsPath)) {
                        if (!color.equals("*") && !presetIds.contains(color.toLowerCase(Locale.ROOT))) {
                            issue(issues, Severity.ERROR, colorsPath, "Unknown preset color '" + color + "'.");
                        } else if (!color.equals("*") && !enabledPresetIds.contains(color.toLowerCase(Locale.ROOT))) {
                            issue(issues, Severity.WARNING, colorsPath, "Preset color '" + color + "' is disabled.");
                        }
                    }
                }
            }
        }
    }

    private void validateRankColor(List<Issue> issues, String path, boolean mayBeEmpty) {
        String value = configs.config().getString(path, "");
        if (mayBeEmpty && value.isBlank()) return;
        if (RankColorCode.normalize(value).isEmpty()) {
            issue(issues, Severity.ERROR, path, "Expected &0-&f, #RRGGBB, or &#RRGGBB.");
        }
    }

    private void validateColors(List<Issue> issues) {
        ConfigurationSection colors = configs.colors().getConfigurationSection("colors");
        if (colors == null) return;
        Set<String> ids = new HashSet<>();
        for (String rawId : colors.getKeys(false)) {
            String root = "colors." + rawId;
            if (!ids.add(rawId.toLowerCase(Locale.ROOT))) {
                issue(issues, Severity.ERROR, "colors.yml:" + root, "Duplicate color ID ignoring letter case.");
            }
            if (!HexColor.isValid(configs.colors().getString(root + ".hex", ""))) {
                issue(issues, Severity.ERROR, "colors.yml:" + root + ".hex", "Expected a #RRGGBB color.");
            }
            for (ColorCategory category : ColorCategory.values()) {
                String path = root + ".permissions." + category.key();
                if (configs.colors().contains(path, true) && configs.colors().getString(path, "").isBlank()) {
                    issue(issues, Severity.WARNING, "colors.yml:" + path,
                        "Empty permission; the automatic category permission will be used.");
                }
            }
        }
    }

    private void validateDefaults(List<Issue> issues) {
        Set<String> enabled = enabledColorIds();
        int min = Math.max(2, configs.config().getInt("custom-colors.gradient.min-colors", 2));
        int max = Math.max(min, Math.min(16, configs.config().getInt("custom-colors.gradient.max-colors", 5)));
        for (ColorCategory category : ColorCategory.values()) {
            String root = "defaults." + category.key();
            String rawMode = configs.config().getString(root + ".mode", "");
            ColorMode mode = ColorMode.parse(rawMode).orElse(null);
            if (mode == null) {
                issue(issues, Severity.ERROR, root + ".mode", "Unknown mode '" + rawMode + "'.");
                continue;
            }
            String value = configs.config().getString(root + ".value", "");
            switch (mode) {
                case PRESET -> {
                    if (!enabled.contains(value.toLowerCase(Locale.ROOT))) {
                        issue(issues, Severity.ERROR, root + ".value", "Preset '" + value + "' does not exist or is disabled.");
                    }
                }
                case RGB -> {
                    if (!HexColor.isValid(value)) issue(issues, Severity.ERROR, root + ".value", "Expected a #RRGGBB color.");
                }
                case GRADIENT -> {
                    String[] stops = value.split(",", -1);
                    if (stops.length < min || stops.length > max) {
                        issue(issues, Severity.ERROR, root + ".value",
                            "Gradient needs between " + min + " and " + max + " colors.");
                    }
                    for (String stop : stops) {
                        if (!HexColor.isValid(stop.trim())) {
                            issue(issues, Severity.ERROR, root + ".value", "Invalid gradient color '" + stop + "'.");
                        }
                    }
                }
            }
        }
    }

    private void validateFormats(List<Issue> issues) {
        validateFormat(issues, "format.full", Set.of("rank", "plus", "name"), false);
        validateFormat(issues, "format.rank-wrapper", Set.of("rank"), true);
        for (String context : List.of("chat", "tab", "nametag", "scoreboard")) {
            validateFormat(issues, "format.contexts." + context, Set.of("rank", "plus", "name"), false);
        }
    }

    private void validateFormat(List<Issue> issues, String path, Set<String> allowed, boolean rankRequired) {
        String value = configs.config().getString(path, "");
        Matcher matcher = FORMAT_TOKEN.matcher(value);
        while (matcher.find()) {
            if (!allowed.contains(matcher.group(1))) {
                issue(issues, Severity.ERROR, path, "Unknown format token '{" + matcher.group(1) + "}'.");
            }
        }
        if (rankRequired && !value.contains("{rank}")) {
            issue(issues, Severity.WARNING, path, "The rank wrapper does not contain {rank}.");
        }
    }

    private void validateMaterials(List<Issue> issues) {
        validateMaterials(configs.colors(), "colors.yml", issues);
        validateMaterials(configs.menus(), "menus.yml", issues);
    }

    private void validateMaterials(ConfigurationSection section, String file, List<Issue> issues) {
        section.getValues(true).forEach((path, value) -> {
            if (!path.endsWith(".material") || !(value instanceof String material)) return;
            if (Material.matchMaterial(material) == null) {
                issue(issues, Severity.ERROR, file + ":" + path, "Unknown material '" + material + "'.");
            }
        });
    }

    private void validateMenuLayouts(List<Issue> issues) {
        for (String menu : List.of("main", "selection-type", "preset-menu", "gradient-menu", "confirmation")) {
            int rows = configs.menus().getInt(menu + ".rows", 0);
            if (rows < 1 || rows > 6) {
                issue(issues, Severity.ERROR, "menus.yml:" + menu + ".rows", "Rows must be between 1 and 6.");
            }
            int size = Math.max(1, Math.min(6, rows)) * 9;
            ConfigurationSection section = configs.menus().getConfigurationSection(menu);
            if (section == null) continue;
            Map<Integer, String> occupied = new HashMap<>();
            section.getValues(true).forEach((path, value) -> {
                if (!path.endsWith(".slot")) return;
                String fullPath = "menus.yml:" + menu + "." + path;
                if (!(value instanceof Number number)) {
                    issue(issues, Severity.ERROR, fullPath, "Slot must be an integer.");
                    return;
                }
                int slot = number.intValue();
                if (number.doubleValue() != slot || slot < 0 || slot >= size) {
                    issue(issues, Severity.ERROR, fullPath, "Slot must be between 0 and " + (size - 1) + ".");
                    return;
                }
                String previous = occupied.putIfAbsent(slot, path);
                if (previous != null) {
                    issue(issues, Severity.WARNING, fullPath,
                        "Slot " + slot + " is already used by '" + previous + "'.");
                }
            });
        }
        validateSlotList(issues, "preset-menu.color-slots",
            configs.menus().getInt("preset-menu.rows", 6) * 9);
        ConfigurationSection gradientSlots = configs.menus().getConfigurationSection("gradient-menu.color-slots");
        if (gradientSlots != null) {
            int size = configs.menus().getInt("gradient-menu.rows", 4) * 9;
            for (String count : gradientSlots.getKeys(false)) {
                validateSlotList(issues, "gradient-menu.color-slots." + count, size);
            }
        }
    }

    private void validateSlotList(List<Issue> issues, String path, int size) {
        if (!configs.menus().isList(path)) {
            issue(issues, Severity.ERROR, "menus.yml:" + path, "Expected a YAML list of slots.");
            return;
        }
        Set<Integer> used = new HashSet<>();
        for (Integer slot : configs.menus().getIntegerList(path)) {
            if (slot < 0 || slot >= size) {
                issue(issues, Severity.ERROR, "menus.yml:" + path,
                    "Slot " + slot + " is outside this inventory.");
            } else if (!used.add(slot)) {
                issue(issues, Severity.WARNING, "menus.yml:" + path, "Slot " + slot + " is repeated.");
            }
        }
    }

    private void validateSounds(List<Issue> issues) {
        ConfigurationSection sounds = configs.menus().getConfigurationSection("sounds");
        if (sounds == null) return;
        for (String id : sounds.getKeys(false)) {
            String path = "sounds." + id + ".sound";
            String value = configs.menus().getString(path, "");
            if (value.isBlank()) continue;
            try {
                Sound.valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                issue(issues, Severity.ERROR, "menus.yml:" + path, "Unknown sound '" + value + "'.");
            }
        }
    }

    private Set<String> colorIds() {
        ConfigurationSection colors = configs.colors().getConfigurationSection("colors");
        if (colors == null) return Set.of();
        Set<String> result = new HashSet<>();
        colors.getKeys(false).forEach(id -> result.add(id.toLowerCase(Locale.ROOT)));
        return result;
    }

    private Set<String> enabledColorIds() {
        ConfigurationSection colors = configs.colors().getConfigurationSection("colors");
        if (colors == null) return Set.of();
        Set<String> result = new HashSet<>();
        for (String id : colors.getKeys(false)) {
            if (configs.colors().getBoolean("colors." + id + ".enabled", true)) {
                result.add(id.toLowerCase(Locale.ROOT));
            }
        }
        return result;
    }

    private void issue(List<Issue> issues, Severity severity, String path, String message) {
        issues.add(new Issue(severity, path, message));
    }

    public enum Severity { ERROR, WARNING }

    public record Issue(Severity severity, String path, String message) {
    }

    public record Report(List<Issue> issues) {
        public long errors() { return issues.stream().filter(issue -> issue.severity() == Severity.ERROR).count(); }
        public long warnings() { return issues.stream().filter(issue -> issue.severity() == Severity.WARNING).count(); }
        public boolean valid() { return errors() == 0; }
    }
}
