package dev.dusk.rankcolors.color;

import dev.dusk.rankcolors.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.List;
import java.util.Locale;

public final class ColorFormatter {
    private static final List<NamedLegacy> LEGACY_COLORS = List.of(
        new NamedLegacy(NamedTextColor.BLACK, "&0"), new NamedLegacy(NamedTextColor.DARK_BLUE, "&1"),
        new NamedLegacy(NamedTextColor.DARK_GREEN, "&2"), new NamedLegacy(NamedTextColor.DARK_AQUA, "&3"),
        new NamedLegacy(NamedTextColor.DARK_RED, "&4"), new NamedLegacy(NamedTextColor.DARK_PURPLE, "&5"),
        new NamedLegacy(NamedTextColor.GOLD, "&6"), new NamedLegacy(NamedTextColor.GRAY, "&7"),
        new NamedLegacy(NamedTextColor.DARK_GRAY, "&8"), new NamedLegacy(NamedTextColor.BLUE, "&9"),
        new NamedLegacy(NamedTextColor.GREEN, "&a"), new NamedLegacy(NamedTextColor.AQUA, "&b"),
        new NamedLegacy(NamedTextColor.RED, "&c"), new NamedLegacy(NamedTextColor.LIGHT_PURPLE, "&d"),
        new NamedLegacy(NamedTextColor.YELLOW, "&e"), new NamedLegacy(NamedTextColor.WHITE, "&f"));

    private final ColorRegistry registry;
    private final GradientFormatter gradients;

    public ColorFormatter(ColorRegistry registry, GradientFormatter gradients) {
        this.registry = registry;
        this.gradients = gradients;
    }

    public Component format(String text, PlayerColorSelection selection) {
        return switch (selection.mode()) {
            case PRESET -> Component.text(text).color(resolvePreset(selection).textColor());
            case RGB -> Component.text(text).color(HexColor.toTextColor(selection.rgbHex()));
            case GRADIENT -> gradients.format(text, selection.gradient());
        };
    }

    public SelectionView describe(PlayerColorSelection selection) {
        return switch (selection.mode()) {
            case PRESET -> {
                ColorDefinition color = resolvePreset(selection);
                yield new SelectionView(color.hex(), color.legacy(), color.hex(), "<#" + color.hex().substring(1) + ">", "");
            }
            case RGB -> {
                String hex = selection.rgbHex();
                yield new SelectionView(hex, closestLegacy(HexColor.toTextColor(hex)), hex, "<" + hex + ">", "");
            }
            case GRADIENT -> {
                List<String> stops = selection.gradient().hexColors();
                String raw = String.join(",", stops);
                StringBuilder mini = new StringBuilder("<gradient");
                for (String stop : stops) mini.append(':').append(stop);
                mini.append('>');
                yield new SelectionView(raw, closestLegacy(selection.gradient().colors().get(0)), stops.get(0), mini.toString(), raw);
            }
        };
    }

    public String displayValue(PlayerColorSelection selection) {
        return switch (selection.mode()) {
            case PRESET -> Text.plain(resolvePreset(selection).displayName());
            case RGB -> selection.rgbHex();
            case GRADIENT -> String.join(" → ", selection.gradient().hexColors());
        };
    }

    private ColorDefinition resolvePreset(PlayerColorSelection selection) {
        return registry.get(selection.presetId()).orElseGet(() -> new ColorDefinition(selection.presetId(),
            Component.text(selection.presetId()), "#FFFFFF", "&f", org.bukkit.Material.PAPER,
            java.util.EnumSet.allOf(ColorCategory.class), java.util.Map.of()));
    }

    private String closestLegacy(TextColor color) {
        long bestDistance = Long.MAX_VALUE;
        String result = "&f";
        int value = color.value();
        int red = value >> 16 & 0xFF;
        int green = value >> 8 & 0xFF;
        int blue = value & 0xFF;
        for (NamedLegacy candidate : LEGACY_COLORS) {
            int candidateValue = candidate.color.value();
            long dr = red - (candidateValue >> 16 & 0xFF);
            long dg = green - (candidateValue >> 8 & 0xFF);
            long db = blue - (candidateValue & 0xFF);
            long distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                result = candidate.code;
            }
        }
        return result.toLowerCase(Locale.ROOT);
    }

    public record SelectionView(String color, String legacy, String hex, String miniMessage, String gradient) {
    }

    private record NamedLegacy(TextColor color, String code) {
    }
}
