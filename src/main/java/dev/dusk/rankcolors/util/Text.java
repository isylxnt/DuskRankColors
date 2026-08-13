package dev.dusk.rankcolors.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;

public final class Text {
    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.builder()
        .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private Text() {
    }

    public static Component legacy(String value) {
        return AMPERSAND.deserialize(value == null ? "" : value);
    }

    public static String section(Component value) {
        return SECTION.serialize(value);
    }

    public static String plain(Component value) {
        return PLAIN.serialize(value);
    }

    public static String replace(String input, Map<String, String> replacements) {
        String output = input == null ? "" : input;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            output = output.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return output;
    }
}
