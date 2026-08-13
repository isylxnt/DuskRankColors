package dev.dusk.rankcolors.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ItemBuilder {
    private ItemBuilder() {
    }

    public static ItemStack configured(ConfigurationSection section, Material fallback, Map<String, String> replacements) {
        String materialName = section == null ? null : section.getString("material");
        Material material = materialName == null ? fallback : Material.matchMaterial(materialName);
        if (material == null) material = fallback;
        String name = section == null ? "" : section.getString("name", "");
        List<String> lore = section == null ? List.of() : section.getStringList("lore");
        return create(material, Text.legacy(Text.replace(name, replacements)), lore.stream()
            .map(line -> Text.legacy(Text.replace(line, replacements))).toList());
    }

    public static ItemStack create(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material == null || material.isAir() ? Material.PAPER : material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        meta.lore(new ArrayList<>(lore));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack playerHead(Player player, Component name, List<Component> lore) {
        ItemStack item = create(Material.PLAYER_HEAD, name, lore);
        if (item.getItemMeta() instanceof SkullMeta skull) {
            skull.setOwningPlayer(player);
            item.setItemMeta(skull);
        }
        return item;
    }
}
