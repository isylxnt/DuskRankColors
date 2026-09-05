package dev.dusk.rankcolors.rank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankColorCodeTest {
    @Test
    void acceptsLegacyAndHexFormats() {
        assertEquals("#55FF55", RankColorCode.normalize("&a").orElseThrow());
        assertEquals("#FFAA00", RankColorCode.normalize("&6").orElseThrow());
        assertEquals("#9863E7", RankColorCode.normalize("&#9863e7").orElseThrow());
        assertEquals("#9863E7", RankColorCode.normalize("#9863e7").orElseThrow());
    }

    @Test
    void rejectsFormattingCodesAndMalformedValues() {
        assertTrue(RankColorCode.normalize("&l").isEmpty());
        assertTrue(RankColorCode.normalize("green").isEmpty());
        assertTrue(RankColorCode.normalize("").isEmpty());
        assertTrue(RankColorCode.normalize(null).isEmpty());
    }
}
