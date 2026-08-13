package dev.dusk.rankcolors.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class PaperSchedulerAdapter implements SchedulerAdapter {
    private final Plugin plugin;

    public PaperSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runGlobal(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runForPlayer(Player player, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runForPlayerLater(Player player, long delayTicks, Runnable task) {
        Bukkit.getScheduler().runTaskLater(plugin, task, Math.max(1, delayTicks));
    }

    @Override
    public void shutdown() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    @Override
    public String platformName() {
        return "Paper";
    }
}
