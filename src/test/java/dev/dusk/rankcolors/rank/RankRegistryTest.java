package dev.dusk.rankcolors.rank;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankRegistryTest {
    private final RankDefinition vip = rank("vip", "duskrankcolors.ranks.vip", 10, false);
    private final RankDefinition vipPlus = rank("vip+", "duskrankcolors.ranks.vip+", 20, true);

    @Test
    void highestPriorityGrantedRankWins() {
        List<RankDefinition> ordered = RankRegistry.ordered(List.of(vip, vipPlus));
        RankDefinition result = RankRegistry.resolve(ordered,
            Set.of("duskrankcolors.ranks.vip", "duskrankcolors.ranks.vip+")::contains);
        assertEquals("vip+", result.id());
        assertTrue(result.plus());
    }

    @Test
    void playersWithoutRankPermissionsHaveNoVisibleRank() {
        RankDefinition result = RankRegistry.resolve(RankRegistry.ordered(List.of(vip, vipPlus)), ignored -> false);
        assertEquals("", result.id());
        assertEquals("", result.display());
        assertFalse(result.plus());
    }

    @Test
    void rankDefinitionExposesConfiguredInitialColors() {
        assertEquals("#55FF55", vipPlus.rankSelection().rgbHex());
        assertEquals("#FFAA00", vipPlus.plusSelection().rgbHex());
    }

    private RankDefinition rank(String id, String permission, int priority, boolean plus) {
        return new RankDefinition(id, id.equals("vip+") ? "VIP" : id, permission, priority,
            "#55FF55", plus, "#FFAA00", true, Map.of(), Map.of());
    }
}
