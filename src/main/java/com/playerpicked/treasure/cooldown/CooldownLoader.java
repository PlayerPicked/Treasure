package com.playerpicked.treasure.cooldown;

import com.playerpicked.treasure.storage.DataAccessException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public final class CooldownLoader implements Listener {
    private final CooldownService cooldowns;
    private final Logger logger;

    public CooldownLoader(@NotNull CooldownService cooldowns, @NotNull Logger logger) {
        this.cooldowns = cooldowns;
        this.logger = logger;
    }

    // Pre-login already runs off the server thread, so blocking here guarantees the data exists on join
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        try {
            cooldowns.loadNow(event.getUniqueId());
        } catch (DataAccessException exception) {
            logger.error("Could not load the treasure cooldowns of {}", event.getName(), exception);
        }
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        cooldowns.unload(event.getPlayer().getUniqueId());
    }
}
