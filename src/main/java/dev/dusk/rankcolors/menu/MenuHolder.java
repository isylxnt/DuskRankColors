package dev.dusk.rankcolors.menu;

import dev.dusk.rankcolors.color.ColorCategory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class MenuHolder implements InventoryHolder {
    private final MenuType type;
    private final ColorCategory category;
    private final int page;
    private final String value;
    private Inventory inventory;

    public MenuHolder(MenuType type, ColorCategory category, int page, String value) {
        this.type = type;
        this.category = category;
        this.page = page;
        this.value = value;
    }

    public void inventory(Inventory inventory) { this.inventory = inventory; }
    public MenuType type() { return type; }
    public ColorCategory category() { return category; }
    public int page() { return page; }
    public String value() { return value; }
    @Override public @NotNull Inventory getInventory() { return inventory; }

    public enum MenuType { MAIN, TYPE, PRESETS, RGB_CONFIRMATION, GRADIENT }
}
