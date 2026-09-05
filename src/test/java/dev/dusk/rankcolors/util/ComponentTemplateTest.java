package dev.dusk.rankcolors.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentTemplateTest {
    @Test
    void composesCompactRankPlusFormatWithoutAddingWhitespace() {
        Component result = ComponentTemplate.compose("{rank}{plus} {name}", Map.of(
            "rank", Component.text("VIP", NamedTextColor.GOLD),
            "plus", Component.text("+", NamedTextColor.GREEN),
            "name", Component.text("Alex", NamedTextColor.AQUA)));

        assertEquals("VIP+ Alex", PlainTextComponentSerializer.plainText().serialize(result));
    }

    @Test
    void preservesUnknownTokensLiterally() {
        Component result = ComponentTemplate.compose("{rank} {unknown}",
            Map.of("rank", Component.text("MVP")));
        assertEquals("MVP {unknown}", PlainTextComponentSerializer.plainText().serialize(result));
    }

    @Test
    void supportsRepeatedTokens() {
        Component result = ComponentTemplate.compose("{name}/{name}", Map.of("name", Component.text("Dusk")));
        assertEquals("Dusk/Dusk", PlainTextComponentSerializer.plainText().serialize(result));
    }
}
