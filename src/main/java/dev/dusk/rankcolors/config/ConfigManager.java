package dev.dusk.rankcolors.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ConfigManager {
    private final JavaPlugin plugin;
    private volatile YamlConfiguration config;
    private volatile YamlConfiguration colors;
    private volatile YamlConfiguration menus;
    private volatile YamlConfiguration messages;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        saveIfMissing("config.yml");
        saveIfMissing("colors.yml");
        saveIfMissing("menus.yml");
        saveIfMissing("messages.yml");
        config = loadFile("config.yml");
        colors = loadFile("colors.yml");
        menus = loadFile("menus.yml");
        messages = loadFile("messages.yml");
        if (config.getInt("config-version", 0) != 1) {
            plugin.getLogger().warning("Unknown config-version. Existing values were preserved; compare with the bundled config.yml.");
        }
    }

    private void saveIfMissing(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) plugin.saveResource(name, false);
    }

    private YamlConfiguration loadFile(String name) {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), name));
    }

    public YamlConfiguration config() { return config; }
    public YamlConfiguration colors() { return colors; }
    public YamlConfiguration menus() { return menus; }
    public YamlConfiguration messages() { return messages; }
}
