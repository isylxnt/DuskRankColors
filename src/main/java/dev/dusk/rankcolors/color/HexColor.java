package dev.dusk.rankcolors.color;

import net.kyori.adventure.text.format.TextColor;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class HexColor {
    private static final Pattern HEX = Pattern.compile("^#?[A-Fa-f0-9]{6}$");

    private HexColor() {
    }

    public static Optional<String> normalize(String input) {
        if (input == null || !HEX.matcher(input.trim()).matches()) return Optional.empty();
        String value = input.trim();
        if (value.charAt(0) != '#') value = "#" + value;
        return Optional.of(value.toUpperCase(Locale.ROOT));
    }

    public static boolean isValid(String input) {
        return normalize(input).isPresent();
    }

    public static TextColor toTextColor(String normalizedHex) {
        String normalized = normalize(normalizedHex)
            .orElseThrow(() -> new IllegalArgumentException("Invalid HEX color: " + normalizedHex));
        TextColor color = TextColor.fromHexString(normalized);
        if (color == null) throw new IllegalArgumentException("Invalid HEX color: " + normalizedHex);
        return color;
    }

    public static String fromTextColor(TextColor color) {
        return String.format(Locale.ROOT, "#%06X", color.value());
    }
}
