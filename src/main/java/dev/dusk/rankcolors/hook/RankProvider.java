package dev.dusk.rankcolors.hook;

import java.util.UUID;

@FunctionalInterface
public interface RankProvider {
    String primaryGroup(UUID playerId);
}
