package dev.dusk.rankcolors.color;

import net.kyori.adventure.text.format.TextColor;

import java.util.List;
import java.util.Objects;

public final class GradientDefinition {
    private final List<TextColor> colors;

    public GradientDefinition(List<TextColor> colors) {
        Objects.requireNonNull(colors, "colors");
        if (colors.size() < 2) throw new IllegalArgumentException("A gradient needs at least two colors");
        if (colors.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Gradient colors cannot be null");
        this.colors = List.copyOf(colors);
    }

    public static GradientDefinition fromHex(List<String> colors) {
        return new GradientDefinition(colors.stream().map(HexColor::toTextColor).toList());
    }

    public List<TextColor> colors() {
        return colors;
    }

    public List<String> hexColors() {
        return colors.stream().map(HexColor::fromTextColor).toList();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof GradientDefinition that && colors.equals(that.colors);
    }

    @Override
    public int hashCode() {
        return colors.hashCode();
    }
}
