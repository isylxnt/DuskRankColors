package dev.dusk.rankcolors.color;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GradientFormatterTest {
    private final GradientFormatter formatter = new GradientFormatter();

    @Test
    void interpolatesTwoStopsIncludingEndpoints() {
        List<TextColor> result = formatter.interpolate(List.of(TextColor.color(0xFF0000), TextColor.color(0x00FF00)), 3);
        assertEquals(List.of(0xFF0000, 0x808000, 0x00FF00), result.stream().map(TextColor::value).toList());
    }

    @Test
    void interpolatesMultipleStopsBySegments() {
        List<TextColor> result = formatter.interpolate(List.of(
            TextColor.color(0xFF0000), TextColor.color(0xFFFF00), TextColor.color(0x00FF00)), 5);
        assertEquals(List.of(0xFF0000, 0xFF8000, 0xFFFF00, 0x80FF00, 0x00FF00),
            result.stream().map(TextColor::value).toList());
    }

    @Test
    void handlesEmptySingleCharacterAndMoreStopsThanCharacters() {
        GradientDefinition gradient = GradientDefinition.fromHex(List.of("#FF0000", "#FFFF00", "#00FF00"));
        assertEquals(Component.empty(), formatter.format("", gradient));
        assertEquals(List.of(0xFF0000), formatter.interpolate(gradient.colors(), 1).stream().map(TextColor::value).toList());
        assertEquals(List.of(0xFF0000, 0x00FF00), formatter.interpolate(gradient.colors(), 2).stream().map(TextColor::value).toList());
    }

    @Test
    void operatesOnUnicodeCodePoints() {
        GradientDefinition gradient = GradientDefinition.fromHex(List.of("#FF0000", "#0000FF"));
        Component component = formatter.format("A😀B", gradient);
        assertEquals("A😀B", PlainTextComponentSerializer.plainText().serialize(component));
        assertEquals(3, component.children().size());
    }

    @Test
    void oneCharacterUsesFirstStop() {
        List<TextColor> colors = formatter.interpolate(List.of(TextColor.color(0x123456), TextColor.color(0xFFFFFF)), 1);
        assertEquals(0x123456, colors.get(0).value());
    }
}
