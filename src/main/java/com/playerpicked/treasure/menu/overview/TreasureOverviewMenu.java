package com.playerpicked.treasure.menu.overview;

import com.playerpicked.treasure.menu.ClickAction;
import com.playerpicked.treasure.menu.MenuContext;
import com.playerpicked.treasure.menu.MenuNavigator;
import com.playerpicked.treasure.menu.PaginatedMenu;
import com.playerpicked.treasure.menu.TreasureSummary;
import com.playerpicked.treasure.notification.SoundEffect;
import com.playerpicked.treasure.text.MessageKey;
import com.playerpicked.treasure.treasure.TreasureChest;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class TreasureOverviewMenu extends PaginatedMenu<TreasureChest> {
    private final MenuNavigator navigator;
    private volatile List<TreasureChest> treasures = List.of();

    public TreasureOverviewMenu(@NotNull Player viewer, @NotNull MenuContext context, @NotNull MenuNavigator navigator) {
        super(viewer, context);
        this.navigator = navigator;
    }

    @Override
    protected @NotNull String layoutKey() {
        return "overview";
    }

    @Override
    protected @NotNull String entryItemKey() {
        return "treasure";
    }

    @Override
    protected @NotNull CompletableFuture<Void> loadData() {
        treasures = context.treasures().all();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected @NotNull List<TreasureChest> entries() {
        return treasures;
    }

    @Override
    protected @NotNull TagResolver menuPlaceholders() {
        return TagResolver.resolver(Placeholder.unparsed("count", String.valueOf(treasures.size())), pagePlaceholders());
    }

    @Override
    protected @NotNull TagResolver entryPlaceholders(@NotNull TreasureChest chest) {
        return TreasureSummary.placeholders(chest, context.messages());
    }

    @Override
    protected void onEntryClicked(@NotNull TreasureChest chest, @NotNull ClickType clickType) {
        switch (clickType) {
            case LEFT, SHIFT_LEFT -> {
                context.sounds().play(viewer, SoundEffect.CLICK);
                navigator.openEditor(viewer, chest.id());
            }
            case RIGHT, SHIFT_RIGHT -> {
                context.sounds().play(viewer, SoundEffect.CLICK);
                context.loot().openPreview(viewer, chest);
            }
            case DROP, CONTROL_DROP -> {
                if (!context.treasures().delete(chest.id())) return;
                context.messages().send(viewer, MessageKey.TREASURE_DELETED, Placeholder.unparsed("id", chest.id()));
                context.sounds().play(viewer, SoundEffect.DELETED);
                refresh();
            }
            default -> { }
        }
    }

    @Override
    protected @Nullable ClickAction buttonAction(@NotNull String itemKey) {
        return switch (itemKey) {
            case "previous-page" -> previousPageButton();
            case "next-page" -> nextPageButton();
            case "create" -> clickType -> {
                TreasureChest created = context.treasures().create();
                context.messages().send(viewer, MessageKey.TREASURE_CREATED, Placeholder.unparsed("id", created.id()));
                context.sounds().play(viewer, SoundEffect.SAVED);
                navigator.openEditor(viewer, created.id());
            };
            case "close" -> clickType -> viewer.closeInventory();
            default -> null;
        };
    }
}
