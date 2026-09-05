package dev.dusk.rankcolors.hook;

import dev.dusk.rankcolors.config.PluginConfiguration;
import dev.dusk.rankcolors.service.PlayerColorService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class HookManager {
    private final Plugin plugin;
    private final PluginConfiguration configuration;
    private volatile boolean placeholderHooked;
    private DuskPlaceholderExpansion expansion;

    public HookManager(Plugin plugin, PluginConfiguration configuration) {
        this.plugin = plugin;
        this.configuration = configuration;
    }

    public void reloadIntegrations(PlayerColorService colors) {
        if (expansion != null) {
            expansion.unregister();
            expansion = null;
        }
        placeholderHooked = false;
        if (!configuration.integrationEnabled("placeholderapi")
            || Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return;
        expansion = new DuskPlaceholderExpansion(plugin.getPluginMeta().getVersion(), colors);
        placeholderHooked = expansion.register();
    }

    public void shutdown() {
        if (expansion != null) expansion.unregister();
        expansion = null;
        placeholderHooked = false;
    }

    public boolean placeholderHooked() { return placeholderHooked; }
}
