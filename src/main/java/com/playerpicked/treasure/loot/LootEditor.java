package com.playerpicked.treasure.loot;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class LootEditor implements LootInventory {
    private final Inventory inventory;
    private final Consumer<List<ItemStack>> onSave;

    LootEditor(@NotNull Component title, int size, @NotNull List<ItemStack> loot, @NotNull Consumer<List<ItemStack>> onSave) {
        this.inventory = Bukkit.createInventory(this, size, title);
        this.onSave = onSave;
        for (int slot = 0; slot < Math.min(loot.size(), size); slot++) inventory.setItem(slot, loot.get(slot));
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event) { }

    @Override
    public void onDrag(@NotNull InventoryDragEvent event) { }

    @Override
    public void onClose() {
        onSave.accept(Arrays.stream(inventory.getContents())
                .filter(Objects::nonNull)
                .filter(item -> !item.isEmpty())
                .map(ItemStack::clone)
                .toList());
    }
}
