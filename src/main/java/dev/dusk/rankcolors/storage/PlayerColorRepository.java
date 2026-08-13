package dev.dusk.rankcolors.storage;

import dev.dusk.rankcolors.color.ColorCategory;
import dev.dusk.rankcolors.color.ColorMode;
import dev.dusk.rankcolors.color.PlayerColorSelection;
import dev.dusk.rankcolors.color.PlayerColorSettings;
import dev.dusk.rankcolors.config.PluginConfiguration;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public final class PlayerColorRepository {
    private final PluginConfiguration configuration;
    private final GradientSerializer gradients;
    private final Logger logger;
    private final Map<ColorCategory, NamespacedKey> modeKeys = new EnumMap<>(ColorCategory.class);
    private final Map<ColorCategory, NamespacedKey> valueKeys = new EnumMap<>(ColorCategory.class);
    private final Map<ColorCategory, NamespacedKey> legacyKeys = new EnumMap<>(ColorCategory.class);

    public PlayerColorRepository(Plugin plugin, PluginConfiguration configuration, GradientSerializer gradients) {
        this.configuration = configuration;
        this.gradients = gradients;
        this.logger = plugin.getLogger();
        for (ColorCategory category : ColorCategory.values()) {
            modeKeys.put(category, new NamespacedKey(plugin, category.key() + "_mode"));
            valueKeys.put(category, new NamespacedKey(plugin, category.key() + "_value"));
            legacyKeys.put(category, new NamespacedKey(plugin, category.key()));
        }
    }

    public PlayerColorSettings load(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        EnumMap<ColorCategory, PlayerColorSelection> loaded = new EnumMap<>(ColorCategory.class);
        for (ColorCategory category : ColorCategory.values()) {
            String modeValue = data.get(modeKeys.get(category), PersistentDataType.STRING);
            String rawValue = data.get(valueKeys.get(category), PersistentDataType.STRING);
            boolean migrated = false;
            if (modeValue == null || rawValue == null) {
                String legacy = data.get(legacyKeys.get(category), PersistentDataType.STRING);
                if (legacy != null && !legacy.isBlank()) {
                    modeValue = ColorMode.PRESET.name();
                    rawValue = legacy;
                    migrated = true;
                }
            }
            PlayerColorSelection selection = parse(modeValue, rawValue).orElse(configuration.defaultSelection(category));
            loaded.put(category, selection);
            if (migrated) {
                save(player, category, selection);
                data.remove(legacyKeys.get(category));
                logger.info("Migrated legacy " + category.key() + " color data for " + player.getUniqueId() + ".");
            }
        }
        return new PlayerColorSettings(loaded.get(ColorCategory.RANK), loaded.get(ColorCategory.PLUS), loaded.get(ColorCategory.NAME));
    }

    public Optional<PlayerColorSelection> parse(String modeValue, String rawValue) {
        Optional<ColorMode> mode = ColorMode.parse(modeValue);
        if (mode.isEmpty() || rawValue == null) return Optional.empty();
        try {
            return switch (mode.get()) {
                case PRESET -> Optional.of(PlayerColorSelection.preset(rawValue));
                case RGB -> Optional.of(PlayerColorSelection.rgb(rawValue));
                case GRADIENT -> gradients.deserialize(rawValue, configuration.minGradientColors(), configuration.maxGradientColors())
                    .map(PlayerColorSelection::gradient);
            };
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public void save(Player player, ColorCategory category, PlayerColorSelection selection) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.set(modeKeys.get(category), PersistentDataType.STRING, selection.mode().name());
        data.set(valueKeys.get(category), PersistentDataType.STRING, selection.rawValue());
    }

    public void reset(Player player, ColorCategory category) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.remove(modeKeys.get(category));
        data.remove(valueKeys.get(category));
        data.remove(legacyKeys.get(category));
    }
}
