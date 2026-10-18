package com.playerpicked.treasure.loot;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public sealed interface LootInventory extends InventoryHolder permits LootView, LootEditor, LootPreview {
    void onClick(@NotNull InventoryClickEvent event);

    default void onDrag(@NotNull InventoryDragEvent event) {
        event.setCancelled(true);
    }

    default void onClose() { }
}
