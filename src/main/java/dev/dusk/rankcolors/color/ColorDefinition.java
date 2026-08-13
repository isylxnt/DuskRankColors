package dev.dusk.rankcolors.color;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ColorDefinition {
    private final String id;
    private final Component displayName;
    private final String hex;
    private final TextColor textColor;
    private final String legacy;
    private final Material material;
    private final Set<ColorCategory> allowedCategories;
    private final Map<ColorCategory, String> permissions;

    public ColorDefinition(String id, Component displayName, String hex, String legacy, Material material,
                           Set<ColorCategory> allowedCategories, Map<ColorCategory, String> permissions) {
        this.id = Objects.requireNonNull(id, "id").toLowerCase(Locale.ROOT);
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.hex = HexColor.normalize(hex).orElseThrow(() -> new IllegalArgumentException("Invalid HEX: " + hex));
        this.textColor = HexColor.toTextColor(this.hex);
        this.legacy = Objects.requireNonNull(legacy, "legacy");
        this.material = Objects.requireNonNull(material, "material");
        this.allowedCategories = Set.copyOf(allowedCategories.isEmpty()
            ? EnumSet.noneOf(ColorCategory.class) : allowedCategories);
        EnumMap<ColorCategory, String> copy = new EnumMap<>(ColorCategory.class);
        copy.putAll(permissions);
        this.permissions = Map.copyOf(copy);
    }

    public String id() { return id; }
    public Component displayName() { return displayName; }
    public String hex() { return hex; }
    public TextColor textColor() { return textColor; }
    public String legacy() { return legacy; }
    public Material material() { return material; }
    public boolean allows(ColorCategory category) { return allowedCategories.contains(category); }

    public String permission(ColorCategory category) {
        return permissions.getOrDefault(category, category.permission(id));
    }
}
