package dev.dusk.rankcolors.scheduler;

import org.bukkit.plugin.Plugin;

public final class SchedulerFactory {
    private SchedulerFactory() {
    }

    public static SchedulerAdapter create(Plugin plugin) {
        return FoliaSchedulerAdapter.isAvailable()
            ? new FoliaSchedulerAdapter(plugin)
            : new PaperSchedulerAdapter(plugin);
    }
}
