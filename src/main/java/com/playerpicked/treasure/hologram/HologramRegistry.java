package com.playerpicked.treasure.hologram;

import com.playerpicked.treasure.config.HologramSettings;
import com.playerpicked.treasure.scheduler.ServerScheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class HologramRegistry {
    private final Plugin plugin;
    private final ServerScheduler scheduler;
    private final Map<String, PlayerHologram> holograms = new ConcurrentHashMap<>();

    public HologramRegistry(@NotNull Plugin plugin, @NotNull ServerScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    public void show(@NotNull String id, @NotNull Location anchor, @NotNull HologramSettings settings, @NotNull Function<Player, Component> textFor) {
        hide(id);

        PlayerHologram hologram = new PlayerHologram(plugin, anchor, settings, textFor);
        holograms.put(id, hologram);
        hologram.start(scheduler);
    }

    public void hide(@NotNull String id) {
        PlayerHologram hologram = holograms.remove(id);
        if (hologram != null) hologram.stop(scheduler);
    }

    public void shutdown() {
        holograms.values().forEach(PlayerHologram::stopNow);
        holograms.clear();
    }
}
