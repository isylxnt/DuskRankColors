package dev.dusk.rankcolors.listener;

import dev.dusk.rankcolors.menu.MenuManager;
import dev.dusk.rankcolors.service.PlayerColorService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener implements Listener {
    private final PlayerColorService colors;
    private final MenuManager menus;

    public PlayerListener(PlayerColorService colors, MenuManager menus) {
        this.colors = colors;
        this.menus = menus;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        colors.load(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        colors.unload(event.getPlayer().getUniqueId());
        menus.clearPlayer(event.getPlayer().getUniqueId());
    }
}
