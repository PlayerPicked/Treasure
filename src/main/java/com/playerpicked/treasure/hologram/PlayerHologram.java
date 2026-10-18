package com.playerpicked.treasure.hologram;

import com.playerpicked.treasure.config.HologramSettings;
import com.playerpicked.treasure.scheduler.ServerScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

final class PlayerHologram {
    private final Plugin plugin;
    private final Location anchor;
    private final HologramSettings settings;
    private final Function<Player, Component> textFor;
    private final Map<UUID, Viewer> viewers = new ConcurrentHashMap<>();

    private ScheduledTask refreshTask;
    private volatile boolean stopped;

    PlayerHologram(@NotNull Plugin plugin, @NotNull Location anchor, @NotNull HologramSettings settings,
                     @NotNull Function<Player, Component> textFor) {
        this.plugin = plugin;
        this.anchor = anchor;
        this.settings = settings;
        this.textFor = textFor;
    }

    void start(@NotNull ServerScheduler scheduler) {
        refreshTask = scheduler.runAtTimer(anchor, 1, settings.updateIntervalTicks(), this::refresh);
    }

    void stop(@NotNull ServerScheduler scheduler) {
        stopped = true;
        refreshTask.cancel();
        scheduler.runAt(anchor, this::despawnAll);
    }

    // During shutdown no task can be scheduled; entities are not persistent, so skipping foreign regions is safe
    void stopNow() {
        stopped = true;
        refreshTask.cancel();
        if (Bukkit.isOwnedByCurrentRegion(anchor)) despawnAll();
    }

    private void refresh() {
        if (stopped) return;

        World world = anchor.getWorld();
        if (!world.isChunkLoaded(anchor.getBlockX() >> 4, anchor.getBlockZ() >> 4)) {
            despawnAll();
            return;
        }

        double maxDistanceSquared = settings.viewDistance() * settings.viewDistance();
        Set<UUID> inRange = new HashSet<>();

        for (Player player : world.getPlayers()) {
            if (!Bukkit.isOwnedByCurrentRegion(player)) continue;
            if (player.getLocation().distanceSquared(anchor) > maxDistanceSquared) continue;

            inRange.add(player.getUniqueId());
            showTo(player);
        }

        viewers.entrySet().removeIf(entry -> {
            if (inRange.contains(entry.getKey())) return false;
            entry.getValue().display().remove();
            return true;
        });
    }

    private void showTo(@NotNull Player player) {
        Viewer viewer = viewers.get(player.getUniqueId());

        if (viewer == null || viewer.player() != player || !viewer.display().isValid()) {
            if (viewer != null) viewer.display().remove();
            viewer = new Viewer(player, spawnFor(player));
            viewers.put(player.getUniqueId(), viewer);
        }

        Component text = textFor.apply(player);
        if (!text.equals(viewer.display().text())) viewer.display().text(text);
    }

    private @NotNull TextDisplay spawnFor(@NotNull Player player) {
        TextDisplay display = anchor.getWorld().spawn(anchor, TextDisplay.class, entity -> {
            entity.setPersistent(false);
            entity.setVisibleByDefault(false);
            entity.setBillboard(settings.billboard());
            entity.setShadowed(settings.textShadow());
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            applyBackground(entity);
        });

        player.showEntity(plugin, display);
        return display;
    }

    private void applyBackground(@NotNull TextDisplay display) {
        Color background = settings.background();
        display.setDefaultBackground(background == null);
        if (background != null) display.setBackgroundColor(background);
    }

    private void despawnAll() {
        viewers.values().forEach(viewer -> viewer.display().remove());
        viewers.clear();
    }

    private record Viewer(@NotNull Player player, @NotNull TextDisplay display) {
    }
}
