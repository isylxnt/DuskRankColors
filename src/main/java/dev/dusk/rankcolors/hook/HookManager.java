package dev.dusk.rankcolors.hook;

import dev.dusk.rankcolors.config.PluginConfiguration;
import dev.dusk.rankcolors.service.PlayerColorService;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class HookManager {
    private final Plugin plugin;
    private final PluginConfiguration configuration;
    private boolean luckPermsHooked;
    private boolean placeholderHooked;
    private DuskPlaceholderExpansion expansion;

    public HookManager(Plugin plugin, PluginConfiguration configuration) {
        this.plugin = plugin;
        this.configuration = configuration;
    }

    public RankProvider discoverRankProvider() {
        if (!configuration.integrationEnabled("luckperms") || Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return uuid -> null;
        }
        LuckPerms api = Bukkit.getServicesManager().load(LuckPerms.class);
        if (api == null) return uuid -> null;
        luckPermsHooked = true;
        return new LuckPermsHook(api);
    }

    public void hookPlaceholderApi(PlayerColorService colors) {
        if (!configuration.integrationEnabled("placeholderapi")
            || Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return;
        expansion = new DuskPlaceholderExpansion(plugin.getPluginMeta().getVersion(), colors);
        placeholderHooked = expansion.register();
    }

    public void shutdown() {
        if (expansion != null) expansion.unregister();
        placeholderHooked = false;
    }

    public boolean luckPermsHooked() { return luckPermsHooked; }
    public boolean placeholderHooked() { return placeholderHooked; }
}
