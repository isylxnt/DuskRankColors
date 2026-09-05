package dev.dusk.rankcolors.menu;

import dev.dusk.rankcolors.config.ConfigManager;
import dev.dusk.rankcolors.util.ItemBuilder;
import dev.dusk.rankcolors.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

final class MenuLayout {
    private final ConfigManager configs;

    MenuLayout(ConfigManager configs) {
        this.configs = configs;
    }

    Inventory inventory(String path, MenuHolder holder) {
        int rows = Math.max(1, Math.min(6,
            configs.menus().getInt(path + ".rows", path.equals("gradient-menu") ? 6 : 5)));
        Component title = Text.legacy(configs.menus().getString(path + ".title", "&8DuskRankColors"));
        Inventory inventory = createInventory(holder, rows * 9, Text.section(title));
        holder.inventory(inventory);
        return inventory;
    }

    @SuppressWarnings("deprecation")
    private Inventory createInventory(MenuHolder holder, int size, String title) {
        // The legacy-title overload is supported by every target Paper version and by MockBukkit 1.20.
        return Bukkit.createInventory(holder, size, title);
    }

    void decorate(Inventory inventory, String path) {
        if (!configs.menus().getBoolean(path + ".decoration.enabled",
            configs.menus().getBoolean("main.decoration.enabled", true))) return;
        Material material = material(path + ".decoration.material",
            material("main.decoration.material", Material.GRAY_STAINED_GLASS_PANE));
        ItemStack pane = ItemBuilder.create(material, Component.empty(), List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, pane);
    }

    void addNavigation(Inventory inventory, String path, boolean visible) {
        if (!visible) return;
        putConfigured(inventory, path, path.endsWith("previous") ? 45 : path.endsWith("next") ? 53 : 49,
            path.endsWith("back") ? Material.BARRIER : Material.ARROW, Map.of());
    }

    void putConfigured(Inventory inventory, String path, int fallbackSlot, Material fallbackMaterial,
                       Map<String, String> replacements) {
        ConfigurationSection section = configs.menus().getConfigurationSection(path);
        inventory.setItem(safeSlot(section, fallbackSlot, inventory),
            ItemBuilder.configured(section, fallbackMaterial, replacements));
    }

    int slot(String path, int fallback) {
        return configs.menus().getInt(path + ".slot", fallback);
    }

    int safeSlot(ConfigurationSection section, int fallback, Inventory inventory) {
        int value = section == null ? fallback : section.getInt("slot", fallback);
        return value < 0 || value >= inventory.getSize() ? Math.min(fallback, inventory.getSize() - 1) : value;
    }

    Material material(String path, Material fallback) {
        String value = configs.menus().getString(path);
        Material material = value == null ? null : Material.matchMaterial(value);
        return material == null ? fallback : material;
    }
}
