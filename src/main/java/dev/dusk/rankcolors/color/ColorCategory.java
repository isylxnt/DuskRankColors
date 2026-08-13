package dev.dusk.rankcolors.color;

import java.util.Locale;
import java.util.Optional;

public enum ColorCategory {
    RANK,
    PLUS,
    NAME;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String permission(String suffix) {
        return "duskrankcolors." + key() + "." + suffix;
    }

    public static Optional<ColorCategory> parse(String value) {
        if (value == null) return Optional.empty();
        try {
            return Optional.of(valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
