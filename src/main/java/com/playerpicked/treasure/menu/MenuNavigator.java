package com.playerpicked.treasure.menu;

import com.playerpicked.treasure.menu.editor.HologramEditorMenu;
import com.playerpicked.treasure.menu.editor.TreasureEditorMenu;
import com.playerpicked.treasure.menu.overview.TreasureOverviewMenu;
import com.playerpicked.treasure.text.MessageKey;
import com.playerpicked.treasure.treasure.TreasureChest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class MenuNavigator {
    private final MenuContext context;

    public MenuNavigator(@NotNull MenuContext context) {
        this.context = context;
    }

    public void openOverview(@NotNull Player player) {
        new TreasureOverviewMenu(player, context, this).open();
    }

    public void openEditor(@NotNull Player player, @NotNull String treasureId) {
        findOrTell(player, treasureId).ifPresent(chest -> new TreasureEditorMenu(player, context, this, chest).open());
    }

    public void openHologramEditor(@NotNull Player player, @NotNull String treasureId) {
        findOrTell(player, treasureId).ifPresent(chest -> new HologramEditorMenu(player, context, this, chest).open());
    }

    public void closeAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            context.scheduler().runForPlayer(player, () -> {
                if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof Menu) player.closeInventory();
            });
        }
    }

    private @NotNull Optional<TreasureChest> findOrTell(@NotNull Player player, @NotNull String treasureId) {
        Optional<TreasureChest> chest = context.treasures().find(treasureId);
        if (chest.isEmpty()) context.messages().send(player, MessageKey.TREASURE_GONE);
        return chest;
    }
}
