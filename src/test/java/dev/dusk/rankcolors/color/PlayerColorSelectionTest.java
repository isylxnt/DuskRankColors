package dev.dusk.rankcolors.color;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerColorSelectionTest {
    @Test
    void eachFactoryCreatesExactlyOneMode() {
        PlayerColorSelection preset = PlayerColorSelection.preset("ORANGE");
        assertEquals(ColorMode.PRESET, preset.mode());
        assertEquals("orange", preset.presetId());
        assertThrows(IllegalStateException.class, preset::rgbHex);

        PlayerColorSelection rgb = PlayerColorSelection.rgb("55ffff");
        assertEquals("#55FFFF", rgb.rawValue());
        assertThrows(IllegalStateException.class, rgb::presetId);

        PlayerColorSelection gradient = PlayerColorSelection.gradient(
            GradientDefinition.fromHex(List.of("#FF55FF", "#55FFFF")));
        assertEquals("#FF55FF,#55FFFF", gradient.rawValue());
        assertThrows(IllegalStateException.class, gradient::rgbHex);
    }

    @Test
    void rejectsEmptyPresetAndInvalidRgb() {
        assertThrows(IllegalArgumentException.class, () -> PlayerColorSelection.preset("  "));
        assertThrows(IllegalArgumentException.class, () -> PlayerColorSelection.rgb("#FFF"));
    }

    @Test
    void settingsUpdatesOnlyOneCategory() {
        PlayerColorSelection rank = PlayerColorSelection.preset("orange");
        PlayerColorSelection plus = PlayerColorSelection.preset("lime");
        PlayerColorSelection name = PlayerColorSelection.preset("white");
        PlayerColorSettings initial = new PlayerColorSettings(rank, plus, name);
        PlayerColorSelection replacement = PlayerColorSelection.rgb("#123456");
        PlayerColorSettings updated = initial.with(ColorCategory.NAME, replacement);
        assertEquals(rank, updated.selection(ColorCategory.RANK));
        assertEquals(plus, updated.selection(ColorCategory.PLUS));
        assertEquals(replacement, updated.selection(ColorCategory.NAME));
        assertEquals(name, initial.selection(ColorCategory.NAME));
    }
}
