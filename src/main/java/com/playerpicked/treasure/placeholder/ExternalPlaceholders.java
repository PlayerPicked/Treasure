package com.playerpicked.treasure.placeholder;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ExternalPlaceholders {
    ExternalPlaceholders NONE = (player, miniMessage) -> new PreparedText(miniMessage, TagResolver.empty());

    @NotNull PreparedText prepare(@NotNull Player player, @NotNull String miniMessage);

    record PreparedText(@NotNull String miniMessage, @NotNull TagResolver placeholders) {
    }
}
