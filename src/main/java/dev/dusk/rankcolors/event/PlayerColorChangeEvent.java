package dev.dusk.rankcolors.event;

import dev.dusk.rankcolors.color.ColorCategory;
import dev.dusk.rankcolors.color.PlayerColorSelection;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class PlayerColorChangeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final ColorCategory category;
    private final PlayerColorSelection oldSelection;
    private final PlayerColorSelection newSelection;
    private boolean cancelled;

    public PlayerColorChangeEvent(Player player, ColorCategory category, PlayerColorSelection oldSelection,
                                  PlayerColorSelection newSelection) {
        this.player = player;
        this.category = category;
        this.oldSelection = oldSelection;
        this.newSelection = newSelection;
    }

    public Player getPlayer() { return player; }
    public ColorCategory getCategory() { return category; }
    public PlayerColorSelection getOldSelection() { return oldSelection; }
    public PlayerColorSelection getNewSelection() { return newSelection; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
