package dev.dusk.rankcolors.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextTest {
    @Test
    void replacesKnownInternalPlaceholdersOnly() {
        assertEquals("Hello Dusk, #FF00FF {unknown}",
            Text.replace("Hello {player}, {hex} {unknown}", Map.of("player", "Dusk", "hex", "#FF00FF")));
    }
}
