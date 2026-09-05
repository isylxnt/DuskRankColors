package dev.dusk.rankcolors.scheduler;

import org.bukkit.entity.Player;

public interface SchedulerAdapter {
    void runGlobal(Runnable task);
    void runAsync(Runnable task);
    void runForPlayer(Player player, Runnable task);
    default void runForPlayer(Player player, Runnable task, Runnable retired) {
        runForPlayer(player, task);
    }
    void runGlobalLater(long delayTicks, Runnable task);
    void runForPlayerLater(Player player, long delayTicks, Runnable task);
    void shutdown();
    String platformName();
}
