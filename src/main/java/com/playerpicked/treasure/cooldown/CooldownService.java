package com.playerpicked.treasure.cooldown;

import com.playerpicked.treasure.scheduler.AsyncExecutor;
import com.playerpicked.treasure.storage.CooldownRepository;
import com.playerpicked.treasure.treasure.TreasureChest;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownService {
    private final CooldownRepository repository;
    private final AsyncExecutor async;
    private final Logger logger;
    private final Map<UUID, Map<String, Instant>> openTimesByPlayer = new ConcurrentHashMap<>();

    public CooldownService(@NotNull CooldownRepository repository, @NotNull AsyncExecutor async, @NotNull Logger logger) {
        this.repository = repository;
        this.async = async;
        this.logger = logger;
    }

    public void loadNow(@NotNull UUID playerUuid) {
        openTimesByPlayer.put(playerUuid, new ConcurrentHashMap<>(repository.findOpenTimesOf(playerUuid)));
    }

    public @NotNull CompletableFuture<Void> load(@NotNull UUID playerUuid) {
        return async.run(() -> loadNow(playerUuid));
    }

    public void unload(@NotNull UUID playerUuid) {
        openTimesByPlayer.remove(playerUuid);
    }

    public @NotNull Optional<Duration> remaining(@NotNull UUID playerUuid, @NotNull TreasureChest chest) {
        Map<String, Instant> openTimes = openTimesByPlayer.get(playerUuid);
        if (openTimes == null) return Optional.empty();
        if (!chest.hasCooldown()) return Optional.of(Duration.ZERO);

        Instant openedAt = openTimes.get(chest.id());
        if (openedAt == null) return Optional.of(Duration.ZERO);

        Duration left = Duration.between(Instant.now(), openedAt.plus(chest.cooldown()));
        return Optional.of(left.isNegative() ? Duration.ZERO : left);
    }

    public void recordOpen(@NotNull UUID playerUuid, @NotNull String treasureId) {
        Instant now = Instant.now();
        Map<String, Instant> openTimes = openTimesByPlayer.get(playerUuid);
        if (openTimes != null) openTimes.put(treasureId, now);

        async.run(() -> repository.recordOpen(treasureId, playerUuid, now)).exceptionally(error -> {
            logger.error("Could not store the cooldown of {} for treasure {}", playerUuid, treasureId, error);
            return null;
        });
    }

    public void forgetTreasure(@NotNull String treasureId) {
        openTimesByPlayer.values().forEach(openTimes -> openTimes.remove(treasureId));

        async.run(() -> repository.forgetTreasure(treasureId)).exceptionally(error -> {
            logger.error("Could not delete the cooldowns of treasure {}", treasureId, error);
            return null;
        });
    }
}
