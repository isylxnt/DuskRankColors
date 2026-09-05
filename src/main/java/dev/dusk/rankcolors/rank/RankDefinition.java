package dev.dusk.rankcolors.rank;

import dev.dusk.rankcolors.color.ColorCategory;
import dev.dusk.rankcolors.color.ColorMode;
import dev.dusk.rankcolors.color.PlayerColorSelection;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record RankDefinition(String id, String display, String permission, int priority,
                             String color, boolean plus, String plusColor, boolean bold,
                             Map<ColorCategory, Set<ColorMode>> allowedModes,
                             Map<ColorCategory, Set<String>> allowedColors) {
    public RankDefinition {
        EnumMap<ColorCategory, Set<ColorMode>> modes = new EnumMap<>(ColorCategory.class);
        allowedModes.forEach((category, values) -> modes.put(category, Set.copyOf(values)));
        allowedModes = Map.copyOf(modes);
        EnumMap<ColorCategory, Set<String>> colors = new EnumMap<>(ColorCategory.class);
        allowedColors.forEach((category, values) -> {
            Set<String> normalized = new HashSet<>();
            values.forEach(value -> normalized.add(value.toLowerCase(Locale.ROOT)));
            colors.put(category, Set.copyOf(normalized));
        });
        allowedColors = Map.copyOf(colors);
    }

    public PlayerColorSelection rankSelection() {
        return PlayerColorSelection.rgb(color);
    }

    public PlayerColorSelection plusSelection() {
        return PlayerColorSelection.rgb(plusColor);
    }

    public boolean allowsMode(ColorCategory category, ColorMode mode) {
        return !allowedModes.containsKey(category) || allowedModes.get(category).contains(mode);
    }

    public boolean allowsColor(ColorCategory category, String colorId) {
        if (!allowedColors.containsKey(category)) return true;
        Set<String> colors = allowedColors.get(category);
        return colors.contains("*") || colors.contains(colorId.toLowerCase(Locale.ROOT));
    }
}
