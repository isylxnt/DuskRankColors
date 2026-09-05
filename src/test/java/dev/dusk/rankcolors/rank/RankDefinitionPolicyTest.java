package dev.dusk.rankcolors.rank;

import dev.dusk.rankcolors.color.ColorCategory;
import dev.dusk.rankcolors.color.ColorMode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankDefinitionPolicyTest {
    @Test
    void omittedPoliciesAllowEveryModeAndPreset() {
        RankDefinition rank = rank(Map.of(), Map.of());
        assertTrue(rank.allowsMode(ColorCategory.NAME, ColorMode.GRADIENT));
        assertTrue(rank.allowsColor(ColorCategory.NAME, "orange"));
    }

    @Test
    void configuredPoliciesRestrictOnlyTheirCategory() {
        RankDefinition rank = rank(
            Map.of(ColorCategory.RANK, Set.of(ColorMode.PRESET, ColorMode.RGB)),
            Map.of(ColorCategory.RANK, Set.of("orange", "lime")));
        assertFalse(rank.allowsMode(ColorCategory.RANK, ColorMode.GRADIENT));
        assertTrue(rank.allowsMode(ColorCategory.NAME, ColorMode.GRADIENT));
        assertTrue(rank.allowsColor(ColorCategory.RANK, "ORANGE"));
        assertFalse(rank.allowsColor(ColorCategory.RANK, "red"));
    }

    @Test
    void wildcardAllowsEveryPreset() {
        RankDefinition rank = rank(Map.of(), Map.of(ColorCategory.PLUS, Set.of("*")));
        assertTrue(rank.allowsColor(ColorCategory.PLUS, "any_future_color"));
    }

    private RankDefinition rank(Map<ColorCategory, Set<ColorMode>> modes,
                                Map<ColorCategory, Set<String>> colors) {
        return new RankDefinition("vip", "VIP", "duskrankcolors.ranks.vip", 10,
            "#55FF55", false, "#FFAA00", true, modes, colors);
    }
}
