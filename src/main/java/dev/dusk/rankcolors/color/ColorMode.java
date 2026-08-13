package dev.dusk.rankcolors.color;

import java.util.Locale;
import java.util.Optional;

public enum ColorMode {
    PRESET,
    RGB,
    GRADIENT;

    public static Optional<ColorMode> parse(String value) {
        if (value == null) return Optional.empty();
        try {
            return Optional.of(valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
