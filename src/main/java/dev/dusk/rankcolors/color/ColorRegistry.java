package dev.dusk.rankcolors.color;

import dev.dusk.rankcolors.config.ConfigManager;
import dev.dusk.rankcolors.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public final class ColorRegistry {
    private final ConfigManager configs;
    private final Logger logger;
    private volatile Map<String, ColorDefinition> colors = Map.of();

    public ColorRegistry(ConfigManager configs, Logger logger) {
        this.configs = configs;
        this.logger = logger;
    }

    public void reload() {
        ConfigurationSection section = configs.colors().getConfigurationSection("colors");
        if (section == null) {
            logger.warning("colors.yml has no 'colors' section; no presets were loaded.");
            colors = Map.of();
            return;
        }
        Map<String, ColorDefinition> loaded = new LinkedHashMap<>();
        for (String rawId : section.getKeys(false)) {
            String path = "colors." + rawId;
            if (!configs.colors().getBoolean(path + ".enabled", true)) continue;
            try {
                String id = rawId.toLowerCase(Locale.ROOT);
                String hex = configs.colors().getString(path + ".hex", "");
                if (!HexColor.isValid(hex)) {
                    logger.warning("Invalid color '" + rawId + "': HEX '" + hex + "' is invalid. Color skipped.");
                    continue;
                }
                String materialName = configs.colors().getString(path + ".material", "PAPER");
                Material material = Material.matchMaterial(materialName == null ? "PAPER" : materialName);
                if (material == null) {
                    material = Material.PAPER;
                    logger.warning("Invalid material '" + materialName + "' in colors.yml for '" + rawId + "'. Using PAPER.");
                }
                EnumSet<ColorCategory> allowed = EnumSet.noneOf(ColorCategory.class);
                EnumMap<ColorCategory, String> permissions = new EnumMap<>(ColorCategory.class);
                for (ColorCategory category : ColorCategory.values()) {
                    if (configs.colors().getBoolean(path + ".allowed." + category.key(), true)) allowed.add(category);
                    String permission = configs.colors().getString(path + ".permissions." + category.key());
                    if (permission != null && !permission.isBlank()) permissions.put(category, permission.trim());
                }
                loaded.put(id, new ColorDefinition(id,
                    Text.legacy(configs.colors().getString(path + ".display-name", rawId)), hex,
                    configs.colors().getString(path + ".legacy", "&f"), material, allowed, permissions));
            } catch (RuntimeException exception) {
                logger.warning("Could not load color '" + rawId + "': " + exception.getMessage() + ". Color skipped.");
            }
        }
        colors = Collections.unmodifiableMap(loaded);
    }

    public Optional<ColorDefinition> get(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(colors.get(id.toLowerCase(Locale.ROOT)));
    }

    public List<ColorDefinition> all() {
        return new ArrayList<>(colors.values());
    }

    public List<ColorDefinition> forCategory(ColorCategory category) {
        List<ColorDefinition> result = new ArrayList<>();
        for (ColorDefinition color : colors.values()) if (color.allows(category)) result.add(color);
        return result;
    }

    public int size() {
        return colors.size();
    }
}
