package com.playerpicked.treasure.menu;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.jetbrains.annotations.NotNull;

public final class MenuListener implements Listener {

    // Our own inventory: handle it even if another plugin (e.g. an item-drop blocker) cancelled the click
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(@NotNull InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof Menu menu)) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            menu.handleClick(event.getSlot(), event.getClick());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(@NotNull InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof Menu) event.setCancelled(true);
    }
}
