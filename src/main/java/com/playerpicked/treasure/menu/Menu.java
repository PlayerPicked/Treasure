package com.playerpicked.treasure.menu;

import com.playerpicked.treasure.menu.layout.MenuItemTemplate;
import com.playerpicked.treasure.menu.layout.MenuLayout;
import com.playerpicked.treasure.text.MessageKey;
import com.playerpicked.treasure.text.Messages;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class Menu implements InventoryHolder {
    protected final Player viewer;
    protected final MenuContext context;

    private final Map<Integer, ClickAction> clickActions = new HashMap<>();
    private Inventory inventory;

    protected Menu(@NotNull Player viewer, @NotNull MenuContext context) {
        this.viewer = viewer;
        this.context = context;
    }

    protected abstract @NotNull String layoutKey();
    protected abstract @NotNull CompletableFuture<Void> loadData();
    protected abstract @NotNull TagResolver menuPlaceholders();
    protected abstract @Nullable ClickAction buttonAction(@NotNull String itemKey);
    protected void drawContent(@NotNull MenuLayout layout, @NotNull List<Integer> contentSlots) {}
    protected @Nullable String variantOf(@NotNull String itemKey) {
        return null;
    }
    protected void customizeItem(@NotNull String itemKey, @NotNull ItemStack item) {}
    public final void open() {
        reload(true);
    }
    protected final void refresh() {
        reload(false);
    }
    protected final void redraw() {
        drawAndShow(false);
    }

    public final void handleClick(int slot, @NotNull ClickType clickType) {
        ClickAction action = clickActions.get(slot);
        if (action != null) action.onClick(clickType);
    }

    @Override
    public final @NotNull Inventory getInventory() {
        return inventory;
    }

    protected final void place(int slot, @NotNull ItemStack item, @Nullable ClickAction action) {
        inventory.setItem(slot, item);
        if (action != null) clickActions.put(slot, action);
        else clickActions.remove(slot);
    }

    protected final <T> void afterAction(@NotNull CompletableFuture<T> action, @NotNull Consumer<T> onSuccess) {
        action.thenAccept(result -> {
            onSuccess.accept(result);
            refresh();
        }).exceptionally(error -> {
            context.logger().error("Menu action failed for {}", viewer.getName(), error);
            context.messages().send(viewer, MessageKey.ACTION_FAILED);
            return null;
        });
    }

    private void reload(boolean openAfterDrawing) {
        loadData()
                .thenRun(() -> context.scheduler().runForPlayer(viewer, () -> drawAndShow(openAfterDrawing)))
                .exceptionally(error -> {
                    context.logger().error("Could not load the {} menu for {}", layoutKey(), viewer.getName(), error);
                    context.messages().send(viewer, MessageKey.ACTION_FAILED);
                    return null;
                });
    }

    private void drawAndShow(boolean openAfterDrawing) {
        boolean stillViewing = viewer.getOpenInventory().getTopInventory().getHolder(false) == this;
        if (!openAfterDrawing && !stillViewing) return;

        MenuLayout layout = MenuLayout.read(context.guis(), layoutKey());
        TagResolver placeholders = menuPlaceholders();

        if (inventory == null || inventory.getSize() != layout.size()) {
            inventory = Bukkit.createInventory(this, layout.size(), Messages.render(layout.title(), TagResolver.empty()));
        }

        inventory.clear();
        clickActions.clear();
        drawLayoutItems(layout, placeholders);
        drawContent(layout, layout.resolveContentSlots());
        drawFillItems(layout, placeholders);

        if (!stillViewing) viewer.openInventory(inventory);
    }

    private void drawLayoutItems(@NotNull MenuLayout layout, @NotNull TagResolver placeholders) {
        layout.items().entrySet().stream()
                .filter(entry -> entry.getValue().hasFixedSlots())
                .sorted(Comparator.comparingInt(entry -> entry.getValue().priority()))
                .forEach(entry -> {
                    String itemKey = entry.getKey();
                    MenuItemTemplate template = entry.getValue().variant(variantOf(itemKey));
                    ItemStack item = template.render(placeholders);
                    customizeItem(itemKey, item);

                    ClickAction action = buttonAction(itemKey);
                    for (int slot : template.resolveSlots(layout.size())) place(slot, item.clone(), action);
                });
    }

    private void drawFillItems(@NotNull MenuLayout layout, @NotNull TagResolver placeholders) {
        layout.items().values().stream()
                .filter(MenuItemTemplate::fillsEmptySlots)
                .forEach(template -> {
                    ItemStack filler = template.render(placeholders);
                    for (int slot = 0; slot < inventory.getSize(); slot++) {
                        if (inventory.getItem(slot) == null) inventory.setItem(slot, filler.clone());
                    }
                });
    }
}
