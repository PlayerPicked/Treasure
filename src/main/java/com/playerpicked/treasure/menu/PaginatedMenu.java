package com.playerpicked.treasure.menu;

import com.playerpicked.treasure.menu.layout.MenuItemTemplate;
import com.playerpicked.treasure.menu.layout.MenuLayout;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class PaginatedMenu<T> extends Menu {
    private int pageIndex;

    protected PaginatedMenu(@NotNull Player viewer, @NotNull MenuContext context) {
        super(viewer, context);
    }
    protected abstract @NotNull List<T> entries();
    protected abstract @NotNull String entryItemKey();
    protected abstract @NotNull TagResolver entryPlaceholders(@NotNull T entry);
    protected abstract void onEntryClicked(@NotNull T entry, @NotNull ClickType clickType);
    protected void customizeEntry(@NotNull T entry, @NotNull ItemStack item) {}

    @Override
    protected final void drawContent(@NotNull MenuLayout layout, @NotNull List<Integer> contentSlots) {
        MenuItemTemplate entryTemplate = layout.requireItem(entryItemKey());
        List<T> visibleEntries = new Pagination<>(entries(), contentSlots.size(), pageIndex).visibleEntries();

        for (int index = 0; index < visibleEntries.size(); index++) {
            T entry = visibleEntries.get(index);
            ItemStack item = entryTemplate.render(entryPlaceholders(entry));
            customizeEntry(entry, item);
            place(contentSlots.get(index), item, clickType -> onEntryClicked(entry, clickType));
        }
    }

    protected final @NotNull TagResolver pagePlaceholders() {
        Pagination<T> pagination = currentPagination();
        return TagResolver.resolver(
                Placeholder.unparsed("page", String.valueOf(pagination.pageIndex() + 1)),
                Placeholder.unparsed("pages", String.valueOf(pagination.pageCount()))
        );
    }

    protected final @NotNull ClickAction previousPageButton() {
        return clickType -> {
            Pagination<T> pagination = currentPagination();
            if (!pagination.hasPreviousPage()) return;
            pageIndex = pagination.pageIndex() - 1;
            redraw();
        };
    }

    protected final @NotNull ClickAction nextPageButton() {
        return clickType -> {
            Pagination<T> pagination = currentPagination();
            if (!pagination.hasNextPage()) return;
            pageIndex = pagination.pageIndex() + 1;
            redraw();
        };
    }

    protected final void showFirstPage() {
        pageIndex = 0;
        redraw();
    }

    private @NotNull Pagination<T> currentPagination() {
        int pageSize = MenuLayout.read(context.guis(), layoutKey()).resolveContentSlots().size();
        return new Pagination<>(entries(), pageSize, pageIndex);
    }
}
