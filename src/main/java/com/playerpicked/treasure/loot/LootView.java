package com.playerpicked.treasure.loot;

import com.playerpicked.treasure.notification.SoundEffect;
import com.playerpicked.treasure.notification.SoundPlayer;
import com.playerpicked.treasure.text.MessageKey;
import com.playerpicked.treasure.text.Messages;
import com.playerpicked.treasure.treasure.TreasureChest;
import com.playerpicked.treasure.treasure.TreasureService;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public final class LootView implements LootInventory {
    private final Player viewer;
    private final TreasureChest chest;
    private final TreasureService treasures;
    private final Messages messages;
    private final SoundPlayer sounds;
    private final Inventory inventory;
    private boolean cooldownStarted;

    LootView(@NotNull Player viewer, @NotNull TreasureChest chest, @NotNull TreasureService treasures, @NotNull Messages messages,
             @NotNull SoundPlayer sounds, boolean placeRandomly) {
        this.viewer = viewer;
        this.chest = chest;
        this.treasures = treasures;
        this.messages = messages;
        this.sounds = sounds;
        this.inventory = Bukkit.createInventory(this, chest.size(), Messages.render(chest.name(), TagResolver.empty()));
        placeLoot(placeRandomly);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() != inventory) return;

        ItemStack item = inventory.getItem(event.getSlot());
        if (item == null || item.isEmpty()) return;

        take(event.getSlot(), item);
    }

    @Override
    public void onClose() {
        sounds.play(viewer, SoundEffect.CLOSE);
    }

    private void take(int slot, @NotNull ItemStack item) {
        Map<Integer, ItemStack> leftover = viewer.getInventory().addItem(item.clone());
        ItemStack notTaken = leftover.isEmpty() ? null : leftover.values().iterator().next();

        if (notTaken != null && notTaken.getAmount() == item.getAmount()) {
            messages.send(viewer, MessageKey.LOOT_INVENTORY_FULL);
            sounds.play(viewer, SoundEffect.DENIED);
            return;
        }

        inventory.setItem(slot, notTaken);
        messages.send(viewer, notTaken == null ? MessageKey.LOOT_ITEM_TAKEN : MessageKey.LOOT_INVENTORY_FULL);
        sounds.play(viewer, SoundEffect.PICKUP);
        startCooldownOnce();
    }

    private void startCooldownOnce() {
        if (cooldownStarted) return;
        cooldownStarted = true;
        treasures.recordLooted(viewer, chest);
    }

    private void placeLoot(boolean placeRandomly) {
        List<ItemStack> loot = chest.lootCopy();
        List<Integer> slots = new ArrayList<>(IntStream.range(0, inventory.getSize()).boxed().toList());
        if (placeRandomly) Collections.shuffle(slots);

        for (int index = 0; index < Math.min(loot.size(), slots.size()); index++) {
            inventory.setItem(slots.get(index), loot.get(index));
        }
    }
}
