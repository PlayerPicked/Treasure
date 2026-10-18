package com.playerpicked.treasure.config;

import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record HologramSettings(
        @NotNull Display.Billboard billboard,
        boolean textShadow,
        @Nullable Color background,
        double viewDistance,
        long updateIntervalTicks
) {
}
