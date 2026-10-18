package com.playerpicked.treasure.menu;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record Pagination<T>(@NotNull List<T> entries, int pageSize, int pageIndex) {

    public Pagination {
        pageSize = Math.max(1, pageSize);
        pageIndex = Math.clamp(pageIndex, 0, Math.max(0, (entries.size() - 1) / pageSize));
    }

    public @NotNull List<T> visibleEntries() {
        int start = pageIndex * pageSize;
        return entries.subList(Math.min(start, entries.size()), Math.min(start + pageSize, entries.size()));
    }

    public int pageCount() {
        return Math.max(1, (entries.size() + pageSize - 1) / pageSize);
    }

    public boolean hasPreviousPage() {
        return pageIndex > 0;
    }

    public boolean hasNextPage() {
        return pageIndex < pageCount() - 1;
    }
}
