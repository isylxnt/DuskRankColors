package dev.dusk.rankcolors.color;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColorCategoryTest {
    @Test
    void parsingIsCaseInsensitiveAndSafe() {
        assertEquals(ColorCategory.NAME, ColorCategory.parse("NaMe").orElseThrow());
        assertTrue(ColorCategory.parse("other").isEmpty());
        assertTrue(ColorCategory.parse(null).isEmpty());
    }
}
