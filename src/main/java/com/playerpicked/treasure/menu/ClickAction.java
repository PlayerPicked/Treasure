package com.playerpicked.treasure.menu;

import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ClickAction {
    void onClick(@NotNull ClickType clickType);
}
