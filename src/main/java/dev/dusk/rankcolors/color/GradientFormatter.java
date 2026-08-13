package dev.dusk.rankcolors.color;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.List;

public final class GradientFormatter {
    public Component format(String text, GradientDefinition gradient) {
        return format(text, gradient, Style.empty());
    }

    public Component format(String text, GradientDefinition gradient, Style baseStyle) {
        if (text == null || text.isEmpty()) return Component.empty();
        int[] codePoints = text.codePoints().toArray();
        List<TextColor> colors = interpolate(gradient.colors(), codePoints.length);
        Component result = Component.empty();
        for (int index = 0; index < codePoints.length; index++) {
            String character = new String(Character.toChars(codePoints[index]));
            result = result.append(Component.text(character).style(baseStyle.color(colors.get(index))));
        }
        return result;
    }

    public List<TextColor> interpolate(List<TextColor> stops, int length) {
        if (length <= 0) return List.of();
        if (stops == null || stops.isEmpty()) throw new IllegalArgumentException("At least one color stop is required");
        if (length == 1 || stops.size() == 1) return List.of(stops.get(0));
        List<TextColor> result = new ArrayList<>(length);
        int segments = stops.size() - 1;
        for (int index = 0; index < length; index++) {
            double progress = index / (double) (length - 1);
            double scaled = progress * segments;
            int segment = Math.min((int) Math.floor(scaled), segments - 1);
            double local = segment == segments - 1 && progress == 1.0 ? 1.0 : scaled - segment;
            result.add(interpolate(stops.get(segment), stops.get(segment + 1), local));
        }
        return List.copyOf(result);
    }

    private TextColor interpolate(TextColor start, TextColor end, double progress) {
        int startValue = start.value();
        int endValue = end.value();
        int red = channel(startValue >> 16, endValue >> 16, progress);
        int green = channel(startValue >> 8, endValue >> 8, progress);
        int blue = channel(startValue, endValue, progress);
        return TextColor.color(red, green, blue);
    }

    private int channel(int start, int end, double progress) {
        return (int) Math.round((start & 0xFF) + ((end & 0xFF) - (start & 0xFF)) * progress);
    }
}
