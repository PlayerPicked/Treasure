package com.playerpicked.treasure.loot;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class LootPreview implements LootInventory {
    private final Inventory inventory;

    LootPreview(@NotNull Component title, int size, @NotNull List<ItemStack> loot) {
        this.inventory = Bukkit.createInventory(this, size, title);
        for (int slot = 0; slot < Math.min(loot.size(), size); slot++) inventory.setItem(slot, loot.get(slot));
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
