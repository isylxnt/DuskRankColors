package dev.dusk.rankcolors.listener;

import dev.dusk.rankcolors.input.InputManager;
import dev.dusk.rankcolors.menu.MenuHolder;
import dev.dusk.rankcolors.menu.MenuManager;
import dev.dusk.rankcolors.scheduler.SchedulerAdapter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class InventoryListener implements Listener {
    private final MenuManager menus;
    private final InputManager inputs;
    private final SchedulerAdapter scheduler;

    public InventoryListener(MenuManager menus, InputManager inputs, SchedulerAdapter scheduler) {
        this.menus = menus;
        this.inputs = inputs;
        this.scheduler = scheduler;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) return;
        menus.click(player, holder, rawSlot, event.getClick());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)
            || holder.type() != MenuHolder.MenuType.GRADIENT || !(event.getPlayer() instanceof Player player)) return;
        scheduler.runForPlayerLater(player, 1, () -> {
            if (!inputs.isPending(player.getUniqueId())
                && !(player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder)) {
                inputs.discardDraft(player.getUniqueId());
            }
        });
    }
}
