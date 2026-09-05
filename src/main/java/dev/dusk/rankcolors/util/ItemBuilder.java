package dev.dusk.rankcolors.util;

import dev.dusk.rankcolors.color.HexColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Map;

public final class ItemBuilder {
    private ItemBuilder() {
    }

    public static ItemStack configured(ConfigurationSection section, Material fallback, Map<String, String> replacements) {
        return configured(section, fallback, replacements, Map.of());
    }

    public static ItemStack configured(ConfigurationSection section, Material fallback, Map<String, String> replacements,
                                       Map<String, Component> componentReplacements) {
        String materialName = section == null ? null : section.getString("material");
        Material material = materialName == null ? fallback : Material.matchMaterial(materialName);
        if (material == null) material = fallback;
        String name = section == null ? "" : section.getString("name", "");
        List<String> lore = section == null ? List.of() : section.getStringList("lore");
        return create(material, replaceComponents(name, replacements, componentReplacements), lore.stream()
            .map(line -> replaceComponents(line, replacements, componentReplacements)).toList());
    }

    public static ItemStack create(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material == null || material.isAir() ? Material.PAPER : material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(withoutItalic(name));
        meta.lore(lore.stream().map(ItemBuilder::withoutItalic).toList());
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

    public static ItemStack tintFireworkStar(ItemStack item, String hex) {
        if (item.getType() != Material.FIREWORK_STAR || !(item.getItemMeta() instanceof FireworkEffectMeta meta)) {
            return item;
        }
        String normalized = HexColor.normalize(hex).orElse(null);
        if (normalized == null) return item;
        int rgb = Integer.parseInt(normalized.substring(1), 16);
        meta.setEffect(FireworkEffect.builder().withColor(Color.fromRGB(rgb)).build());
        meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
        item.setItemMeta(meta);
        return item;
    }

    private static Component withoutItalic(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    private static Component replaceComponents(String input, Map<String, String> stringReplacements,
                                               Map<String, Component> componentReplacements) {
        String value = Text.replace(input, stringReplacements);
        Component output = Component.empty();
        int cursor = 0;
        while (cursor < value.length()) {
            int nextIndex = -1;
            String nextKey = null;
            for (String key : componentReplacements.keySet()) {
                int index = value.indexOf("{" + key + "}", cursor);
                if (index >= 0 && (nextIndex < 0 || index < nextIndex)) {
                    nextIndex = index;
                    nextKey = key;
                }
            }
            if (nextKey == null) return output.append(Text.legacy(value.substring(cursor)));
            if (nextIndex > cursor) output = output.append(Text.legacy(value.substring(cursor, nextIndex)));
            output = output.append(componentReplacements.get(nextKey));
            cursor = nextIndex + nextKey.length() + 2;
        }
        return output;
    }
}
