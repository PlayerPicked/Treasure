package com.playerpicked.treasure.loot;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.jetbrains.annotations.NotNull;

public final class LootInventoryListener implements Listener {

    // Our own inventory: handle it even if another plugin (e.g. an item-drop blocker) cancelled the click
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(@NotNull InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof LootInventory loot) loot.onClick(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(@NotNull InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof LootInventory loot) loot.onDrag(event);
    }

    @EventHandler
    public void onClose(@NotNull InventoryCloseEvent event) {
        if (event.getInventory().getHolder(false) instanceof LootInventory loot) loot.onClose();
    }
}
