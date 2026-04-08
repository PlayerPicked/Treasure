package com.mongenscave.mctreasure.gui.models;

import com.mongenscave.mctreasure.api.TreasureCloseEvent;
import com.mongenscave.mctreasure.data.MenuController;
import com.mongenscave.mctreasure.gui.Menu;
import com.mongenscave.mctreasure.identifiers.keys.ConfigKeys;
import com.mongenscave.mctreasure.identifiers.keys.MessageKeys;
import com.mongenscave.mctreasure.model.TreasureChest;
import com.mongenscave.mctreasure.processor.MessageProcessor;
import com.mongenscave.mctreasure.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TreasureInventoryMenu extends Menu {
    private final TreasureChest chest;
    private final List<ItemStack> availableItems;
    private final List<ItemStack> itemsTaken = Collections.synchronizedList(new ArrayList<>());
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private boolean itemsPlaced = false;
    private boolean cooldownRecorded = false;

    public TreasureInventoryMenu(@NotNull MenuController menuController, @NotNull TreasureChest chest) {
        super(menuController);
        this.chest = chest;
        this.availableItems = Collections.synchronizedList(new ArrayList<>(chest.getItems()));
        if (ConfigKeys.PLACE_RANDOM.getBoolean()) Collections.shuffle(this.availableItems);
    }

    @Override
    public void handleMenu(final @NotNull InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (event.getClickedInventory() != inventory) return;

        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        event.setCancelled(true);

        if (event.getClick().isLeftClick() || event.getClick().isRightClick()) {
            if (hasAvailableSlot(player)) {
                ItemStack takenItem = ItemUtils.removeTrackingData(clickedItem.clone());

                player.getInventory().addItem(takenItem);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
                inventory.setItem(event.getSlot(), new ItemStack(Material.AIR));

                itemsTaken.add(takenItem);

                synchronized (availableItems) {
                    for (int i = 0; i < availableItems.size(); i++) {
                        ItemStack availableItem = availableItems.get(i);
                        if (ItemUtils.removeTrackingData(availableItem).isSimilar(takenItem)) {
                            availableItems.remove(i);
                            break;
                        }
                    }
                }

                if (!MessageKeys.ITEM_OBTAINED.getMessage().isEmpty()) player.sendMessage(MessageKeys.ITEM_OBTAINED.getMessage());

                if (!cooldownRecorded) {
                    chest.recordPlayerOpen(player);
                    cooldownRecorded = true;

                    if (chest.isHologramEnabled() && chest.getHologramLines()
                            .stream()
                            .anyMatch(line -> line.contains("{time-left}"))) {
                        chest.setupHologram();
                    }
                }
            } else {
                player.sendMessage(MessageKeys.INVENTORY_FULL.getMessage());
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            }
        }
    }

    @Override
    public void setMenuItems() {
        if (!itemsPlaced) {
            placeItems();
            itemsPlaced = true;
        }
    }

    private void placeItems() {
        inventory.clear();

        synchronized (availableItems) {
            if (availableItems.isEmpty()) {
                ItemStack noItemsItem = new ItemStack(Material.PAPER);
                inventory.setItem(getSlots() / 2, noItemsItem);
                return;
            }

            if (ConfigKeys.PLACE_RANDOM.getBoolean()) {
                int itemsToShow = Math.min(availableItems.size(), getSlots());
                List<Integer> availableSlots = getRandomSlots(itemsToShow);

                for (int i = 0; i < itemsToShow; i++) {
                    inventory.setItem(availableSlots.get(i), availableItems.get(i).clone());
                }
            } else {
                int itemsToShow = Math.min(availableItems.size(), getSlots());

                for (int i = 0; i < itemsToShow; i++) {
                    inventory.setItem(i, availableItems.get(i).clone());
                }
            }
        }
    }

    @NotNull
    private List<Integer> getRandomSlots(int count) {
        List<Integer> slots = Collections.synchronizedList(new ArrayList<>());
        List<Integer> availableSlots = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < getSlots(); i++) {
            availableSlots.add(i);
        }

        Collections.shuffle(availableSlots, random);

        for (int i = 0; i < count; i++) {
            slots.add(availableSlots.get(i));
        }

        return slots;
    }

    private boolean hasAvailableSlot(@NotNull Player player) {
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) return true;
        }

        return false;
    }

    @Override
    public void close() {
        super.close();

        Player player = menuController.owner();
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);

        TreasureCloseEvent closeEvent = new TreasureCloseEvent(player, chest, itemsTaken);
        Bukkit.getPluginManager().callEvent(closeEvent);
    }

    @Override
    public String getMenuName() {
        return MessageProcessor.process(chest.getName());
    }

    @Override
    public int getSlots() {
        return chest.getSize();
    }

    @Override
    public int getMenuTick() {
        return 20;
    }
}