package dev.dusk.rankcolors.menu;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradientSlotLayoutTest {
    @Test
    void usesConfiguredLayoutWhenItCanFitEveryStop() {
        assertEquals(List.of(1, 4, 7), GradientSlotLayout.resolve(3, List.of(1, 4, 7, 8)));
    }

    @Test
    void preservesTheDesignedTwoToFiveStopLayouts() {
        assertEquals(List.of(10, 16), GradientSlotLayout.resolve(2, List.of()));
        assertEquals(List.of(10, 13, 16), GradientSlotLayout.resolve(3, List.of()));
        assertEquals(List.of(10, 12, 14, 16), GradientSlotLayout.resolve(4, List.of()));
        assertEquals(List.of(10, 11, 13, 15, 16), GradientSlotLayout.resolve(5, List.of()));
    }

    @Test
    void generatesUniqueValidSlotsUpToTheConfiguredMaximum() {
        for (int count = 6; count <= 16; count++) {
            List<Integer> slots = GradientSlotLayout.resolve(count, List.of());
            assertEquals(count, slots.size());
            assertEquals(count, new HashSet<>(slots).size());
            assertTrue(slots.stream().allMatch(slot -> slot >= 9 && slot <= 26));
        }
    }
}
