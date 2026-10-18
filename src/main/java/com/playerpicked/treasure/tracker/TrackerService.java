package com.playerpicked.treasure.tracker;

import com.playerpicked.treasure.config.PluginConfig;
import com.playerpicked.treasure.scheduler.ServerScheduler;
import com.playerpicked.treasure.text.MessageKey;
import com.playerpicked.treasure.text.Messages;
import com.playerpicked.treasure.treasure.TreasureChest;
import com.playerpicked.treasure.treasure.TreasureService;
import com.playerpicked.treasure.world.BlockPosition;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

public final class TrackerService {
    private static final long UPDATE_INTERVAL_TICKS = 10;
    private static final double FACING_THRESHOLD = 0.7;

    private final NamespacedKey trackerKey;
    private final TreasureService treasures;
    private final PluginConfig config;
    private final Messages messages;
    private final ServerScheduler scheduler;

    public TrackerService(@NotNull Plugin plugin, @NotNull TreasureService treasures, @NotNull PluginConfig config,
                          @NotNull Messages messages, @NotNull ServerScheduler scheduler) {
        this.trackerKey = new NamespacedKey(plugin, "tracker");
        this.treasures = treasures;
        this.config = config;
        this.messages = messages;
        this.scheduler = scheduler;
    }

    public void start() {
        scheduler.runGlobalTimer(UPDATE_INTERVAL_TICKS, UPDATE_INTERVAL_TICKS, () -> {
            if (!config.trackerEnabled()) return;
            for (Player player : Bukkit.getOnlinePlayers()) scheduler.runForPlayer(player, () -> showDirection(player));
        });
    }

    public @NotNull ItemStack createTracker() {
        ItemStack tracker = ItemStack.of(config.trackerMaterial());
        tracker.editMeta(meta -> {
            meta.displayName(messages.single(MessageKey.TRACKER_ITEM_NAME).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            meta.lore(messages.lines(MessageKey.TRACKER_ITEM_LORE).stream()
                    .map(line -> line.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                    .toList());
            meta.getPersistentDataContainer().set(trackerKey, PersistentDataType.BOOLEAN, true);
        });
        return tracker;
    }

    private void showDirection(@NotNull Player player) {
        if (!isHoldingTracker(player)) return;

        Location playerLocation = player.getLocation();
        findNearest(playerLocation).ifPresent(position -> {
            double distance = Math.sqrt(position.distanceSquared(playerLocation));
            player.sendActionBar(messages.single(MessageKey.TRACKER_ACTION_BAR, TagResolver.resolver(
                    Placeholder.unparsed("distance", String.format(Locale.ROOT, "%.1f", distance)),
                    Placeholder.component("direction", messages.single(directionTowards(playerLocation, position).messageKey()))
            )));
        });
    }

    private boolean isHoldingTracker(@NotNull Player player) {
        return isTracker(player.getInventory().getItemInMainHand()) || isTracker(player.getInventory().getItemInOffHand());
    }

    private boolean isTracker(@NotNull ItemStack item) {
        return !item.isEmpty() && item.getPersistentDataContainer().has(trackerKey);
    }

    private @NotNull Optional<BlockPosition> findNearest(@NotNull Location from) {
        return treasures.all().stream()
                .map(TreasureChest::location)
                .flatMap(Optional::stream)
                .filter(position -> position.isIn(from.getWorld()))
                .min(Comparator.comparingDouble(position -> position.distanceSquared(from)));
    }

    private static @NotNull Direction directionTowards(@NotNull Location from, @NotNull BlockPosition target) {
        Vector facing = from.getDirection().setY(0);
        Vector towards = new Vector(target.x() + 0.5 - from.getX(), 0, target.z() + 0.5 - from.getZ());
        if (facing.lengthSquared() < 1.0E-4 || towards.lengthSquared() < 1.0E-4) return Direction.FORWARD;

        facing.normalize();
        towards.normalize();

        double dot = facing.dot(towards);
        if (dot > FACING_THRESHOLD) return Direction.FORWARD;
        if (dot < -FACING_THRESHOLD) return Direction.BACKWARD;

        double cross = facing.getX() * towards.getZ() - facing.getZ() * towards.getX();
        return cross > 0 ? Direction.RIGHT : Direction.LEFT;
    }

    private enum Direction {
        FORWARD(MessageKey.TRACKER_FORWARD),
        BACKWARD(MessageKey.TRACKER_BACKWARD),
        LEFT(MessageKey.TRACKER_LEFT),
        RIGHT(MessageKey.TRACKER_RIGHT);

        private final MessageKey messageKey;

        Direction(@NotNull MessageKey messageKey) {
            this.messageKey = messageKey;
        }

        @NotNull MessageKey messageKey() {
            return messageKey;
        }
    }
}
