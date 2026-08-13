package dev.dusk.rankcolors.config;

import dev.dusk.rankcolors.color.ColorCategory;
import dev.dusk.rankcolors.color.ColorMode;
import dev.dusk.rankcolors.color.GradientDefinition;
import dev.dusk.rankcolors.color.PlayerColorSelection;
import dev.dusk.rankcolors.storage.GradientSerializer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class PluginConfiguration {
    private final ConfigManager configs;
    private final GradientSerializer gradientSerializer;
    private volatile Snapshot snapshot;

    public PluginConfiguration(ConfigManager configs, GradientSerializer gradientSerializer) {
        this.configs = configs;
        this.gradientSerializer = gradientSerializer;
    }

    public void reload() {
        int min = Math.max(2, configs.config().getInt("custom-colors.gradient.min-colors", 2));
        int max = Math.max(min, Math.min(16, configs.config().getInt("custom-colors.gradient.max-colors", 5)));
        EnumMap<ColorCategory, PlayerColorSelection> defaults = new EnumMap<>(ColorCategory.class);
        for (ColorCategory category : ColorCategory.values()) {
            String root = "defaults." + category.key();
            ColorMode mode = ColorMode.parse(configs.config().getString(root + ".mode")).orElse(ColorMode.PRESET);
            String value = configs.config().getString(root + ".value", category == ColorCategory.NAME ? "white" : "orange");
            defaults.put(category, parseSelection(mode, value, min, max));
        }
        snapshot = new Snapshot(Map.copyOf(defaults), min, max,
            configs.config().getBoolean("settings.validate-permissions", true),
            configs.config().getBoolean("permissions.validate-selected-color", true),
            configs.config().getBoolean("permissions.invalid-selection.fallback-to-default", true),
            Math.max(5, configs.config().getInt("custom-colors.input.timeout-seconds", 60)),
            Math.max(0, configs.config().getLong("gui.interaction-cooldown-ms", 250)),
            configs.config().getString("format.plus-symbol", "+"),
            configs.config().getString("format.full", "{rank} {plus} {name}"),
            configs.config().getString("format.rank-wrapper", "{rank}"),
            configs.config().getBoolean("apply.player-display-name.enabled", false),
            configs.config().getBoolean("apply.player-list-name.enabled", false));
    }

    private PlayerColorSelection parseSelection(ColorMode mode, String value, int min, int max) {
        try {
            return switch (mode) {
                case PRESET -> PlayerColorSelection.preset(value);
                case RGB -> PlayerColorSelection.rgb(value);
                case GRADIENT -> PlayerColorSelection.gradient(gradientSerializer.deserialize(value, min, max)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid default gradient")));
            };
        } catch (RuntimeException ignored) {
            return PlayerColorSelection.preset("white");
        }
    }

    public PlayerColorSelection defaultSelection(ColorCategory category) { return snapshot.defaults.get(category); }
    public int minGradientColors() { return snapshot.minGradientColors; }
    public int maxGradientColors() { return snapshot.maxGradientColors; }
    public int inputTimeoutSeconds() { return snapshot.inputTimeoutSeconds; }
    public long guiCooldownMillis() { return snapshot.guiCooldownMillis; }
    public boolean validatePermissions() { return snapshot.validatePermissions; }
    public boolean validateStoredPermissions() { return snapshot.validateStoredPermissions; }
    public boolean fallbackInvalidSelection() { return snapshot.fallbackInvalidSelection; }
    public String plusSymbol() { return snapshot.plusSymbol; }
    public String fullFormat() { return snapshot.fullFormat; }
    public String rankWrapper() { return snapshot.rankWrapper; }
    public boolean applyDisplayName() { return snapshot.applyDisplayName; }
    public boolean applyPlayerListName() { return snapshot.applyPlayerListName; }

    public boolean modeEnabled(ColorCategory category, ColorMode mode) {
        if (mode == ColorMode.PRESET) return true;
        String root = "custom-colors." + mode.name().toLowerCase(Locale.ROOT);
        return configs.config().getBoolean(root + ".enabled", true)
            && configs.config().getBoolean(root + ".categories." + category.key(), true);
    }

    public boolean showLockedMode(ColorMode mode) {
        return configs.config().getBoolean("custom-colors." + mode.name().toLowerCase(Locale.ROOT) + ".show-when-locked", true);
    }

    public String rankText(String group) {
        String source = configs.config().getString("rank.source", "STATIC");
        if (!"LUCKPERMS_PRIMARY_GROUP".equalsIgnoreCase(source)) {
            return configs.config().getString("rank.static-text", "RANK");
        }
        if (group == null || group.isBlank()) return configs.config().getString("rank.static-text", "RANK");
        String configured = configs.config().getString("rank.groups." + group.toLowerCase(Locale.ROOT));
        if (configured != null) return configured;
        if (!configs.config().getBoolean("rank.fallback.use-group-name", true)) {
            return configs.config().getString("rank.static-text", "RANK");
        }
        return configs.config().getBoolean("rank.fallback.uppercase", true)
            ? group.toUpperCase(Locale.ROOT) : group;
    }

    public boolean integrationEnabled(String name) {
        return configs.config().getBoolean("integrations." + name.toLowerCase(Locale.ROOT), true);
    }

    public boolean debug() { return configs.config().getBoolean("debug.enabled", false); }

    private record Snapshot(Map<ColorCategory, PlayerColorSelection> defaults, int minGradientColors,
                            int maxGradientColors, boolean validatePermissions, boolean validateStoredPermissions,
                            boolean fallbackInvalidSelection, int inputTimeoutSeconds, long guiCooldownMillis,
                            String plusSymbol, String fullFormat, String rankWrapper, boolean applyDisplayName,
                            boolean applyPlayerListName) {
    }
}
