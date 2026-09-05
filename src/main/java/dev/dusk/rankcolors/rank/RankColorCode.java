package dev.dusk.rankcolors.rank;

import dev.dusk.rankcolors.color.HexColor;

import java.util.Map;
import java.util.Optional;

/** Converts legacy and modern configuration color codes to normalized RGB HEX. */
public final class RankColorCode {
    private static final Map<Character, String> LEGACY = Map.ofEntries(
        Map.entry('0', "#000000"), Map.entry('1', "#0000AA"), Map.entry('2', "#00AA00"),
        Map.entry('3', "#00AAAA"), Map.entry('4', "#AA0000"), Map.entry('5', "#AA00AA"),
        Map.entry('6', "#FFAA00"), Map.entry('7', "#AAAAAA"), Map.entry('8', "#555555"),
        Map.entry('9', "#5555FF"), Map.entry('a', "#55FF55"), Map.entry('b', "#55FFFF"),
        Map.entry('c', "#FF5555"), Map.entry('d', "#FF55FF"), Map.entry('e', "#FFFF55"),
        Map.entry('f', "#FFFFFF"));

    private RankColorCode() {
    }

    public static Optional<String> normalize(String input) {
        if (input == null) return Optional.empty();
        String value = input.trim();
        if (value.matches("(?i)^&#[0-9a-f]{6}$")) value = value.substring(1);
        Optional<String> hex = HexColor.normalize(value);
        if (hex.isPresent()) return hex;
        if (value.matches("(?i)^&[0-9a-f]$")) {
            return Optional.ofNullable(LEGACY.get(Character.toLowerCase(value.charAt(1))));
        }
        return Optional.empty();
    }
}
