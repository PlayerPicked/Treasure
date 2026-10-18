package com.playerpicked.treasure.setup;

import com.playerpicked.treasure.config.PluginConfig;
import com.playerpicked.treasure.scheduler.ServerScheduler;
import com.playerpicked.treasure.text.MessageKey;
import com.playerpicked.treasure.text.Messages;
import com.playerpicked.treasure.world.BlockPosition;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class LocationPicker implements Listener {
    private final PluginConfig config;
    private final Messages messages;
    private final ServerScheduler scheduler;
    private final Map<UUID, Selection> pending = new ConcurrentHashMap<>();

    public LocationPicker(@NotNull PluginConfig config, @NotNull Messages messages, @NotNull ServerScheduler scheduler) {
        this.config = config;
        this.messages = messages;
        this.scheduler = scheduler;
    }

    public void pick(@NotNull Player player, @NotNull Consumer<BlockPosition> onPicked) {
        Selection selection = new Selection(onPicked);
        pending.put(player.getUniqueId(), selection);

        player.closeInventory();
        messages.send(player, MessageKey.LOCATION_PICK_START);

        scheduler.runForPlayerLater(player, config.promptTimeout().toSeconds() * 20, () -> {
            if (pending.remove(player.getUniqueId(), selection)) messages.send(player, MessageKey.PROMPT_EXPIRED);
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(@NotNull BlockBreakEvent event) {
        Selection selection = pending.remove(event.getPlayer().getUniqueId());
        if (selection == null) return;

        event.setCancelled(true);
        selection.onPicked().accept(BlockPosition.of(event.getBlock()));
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }

    // A class, not a record: an expired timeout must only remove its own selection, so equality is identity
    private static final class Selection {
        private final Consumer<BlockPosition> onPicked;

        private Selection(@NotNull Consumer<BlockPosition> onPicked) {
            this.onPicked = onPicked;
        }

        @NotNull Consumer<BlockPosition> onPicked() {
            return onPicked;
        }
    }
}
