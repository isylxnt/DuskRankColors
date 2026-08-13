package dev.dusk.rankcolors.storage;

import dev.dusk.rankcolors.color.GradientDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GradientSerializerTest {
    private final GradientSerializer serializer = new GradientSerializer();

    @Test
    void serializesAndDeserializesNormalizedStops() {
        GradientDefinition original = GradientDefinition.fromHex(List.of("ff0000", "#ffff00", "00ff00"));
        String value = serializer.serialize(original);
        assertEquals("#FF0000,#FFFF00,#00FF00", value);
        assertEquals(original, serializer.deserialize(value, 2, 5).orElseThrow());
    }

    @Test
    void enforcesBoundsAndRejectsCorruption() {
        assertTrue(serializer.deserialize("#FF0000", 2, 5).isEmpty());
        assertTrue(serializer.deserialize("#FF0000,#00FF00,#0000FF", 2, 2).isEmpty());
        assertTrue(serializer.deserialize("#FF0000,#GGHHII", 2, 5).isEmpty());
        assertTrue(serializer.deserialize("#FF0000,", 2, 5).isEmpty());
        assertTrue(serializer.deserialize(null, 2, 5).isEmpty());
    }
}
