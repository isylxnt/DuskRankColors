package dev.dusk.rankcolors.color;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HexColorTest {
    @Test
    void acceptsAndNormalizesSixDigitHex() {
        assertEquals("#FFB224", HexColor.normalize("#FFB224").orElseThrow());
        assertEquals("#FFB224", HexColor.normalize("FFB224").orElseThrow());
        assertEquals("#FFB224", HexColor.normalize("#ffb224").orElseThrow());
        assertEquals("#000000", HexColor.normalize("000000").orElseThrow());
        assertEquals("#FFFFFF", HexColor.normalize("ffffff").orElseThrow());
    }

    @Test
    void rejectsInvalidInputs() {
        for (String invalid : new String[]{"#FFF", "#GGHHII", "rgb(1,2,3)", "orange", "", "#1234567"}) {
            assertFalse(HexColor.isValid(invalid), invalid);
        }
        assertFalse(HexColor.isValid(null));
    }

    @Test
    void roundTripsAdventureColor() {
        assertEquals("#42D9FF", HexColor.fromTextColor(HexColor.toTextColor("42d9ff")));
    }
}
