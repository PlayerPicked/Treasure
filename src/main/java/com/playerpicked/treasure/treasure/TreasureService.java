package com.playerpicked.treasure.treasure;

import com.playerpicked.treasure.cooldown.CooldownService;
import com.playerpicked.treasure.scheduler.AsyncExecutor;
import com.playerpicked.treasure.world.BlockPosition;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

public final class TreasureService {
    private static final int ID_LENGTH = 8;

    private final TreasureStore store;
    private final TreasureVisuals visuals;
    private final CooldownService cooldowns;
    private final AsyncExecutor async;
    private final Logger logger;
    private final Map<String, TreasureChest> treasures = new ConcurrentHashMap<>();

    public TreasureService(@NotNull TreasureStore store, @NotNull TreasureVisuals visuals, @NotNull CooldownService cooldowns,
                           @NotNull AsyncExecutor async, @NotNull Logger logger) {
        this.store = store;
        this.visuals = visuals;
        this.cooldowns = cooldowns;
        this.async = async;
        this.logger = logger;
    }

    public void loadAll() {
        store.loadAll().forEach(chest -> treasures.put(chest.id(), chest));
        showAll();
        logger.info("Loaded {} treasure(s)", treasures.size());
    }

    public void showAll() {
        treasures.values().forEach(visuals::show);
    }

    public @NotNull @Unmodifiable List<TreasureChest> all() {
        return treasures.values().stream().sorted(Comparator.comparing(TreasureChest::id)).toList();
    }

    public @NotNull Optional<TreasureChest> find(@NotNull String id) {
        return Optional.ofNullable(treasures.get(id));
    }

    public @NotNull Optional<TreasureChest> findAt(@NotNull BlockPosition position) {
        return treasures.values().stream().filter(chest -> chest.isAt(position)).findFirst();
    }

    public @NotNull TreasureChest create() {
        String id;
        do {
            id = UUID.randomUUID().toString().replace("-", "").substring(0, ID_LENGTH);
        } while (treasures.containsKey(id));

        TreasureChest chest = TreasureChest.createDefault(id);
        treasures.put(id, chest);
        persist();
        return chest;
    }

    public boolean delete(@NotNull String id) {
        if (treasures.remove(id) == null) return false;

        visuals.hide(id);
        cooldowns.forgetTreasure(id);
        persist();
        return true;
    }

    public @NotNull Optional<TreasureChest> update(@NotNull String id, @NotNull UnaryOperator<TreasureChest> change) {
        TreasureChest updated = treasures.computeIfPresent(id, (ignored, chest) -> change.apply(chest));
        if (updated == null) return Optional.empty();

        visuals.show(updated);
        persist();
        return Optional.of(updated);
    }

    public @NotNull MoveResult moveTo(@NotNull String id, @NotNull BlockPosition position) {
        Optional<TreasureChest> occupant = findAt(position);
        if (occupant.isPresent() && !occupant.get().id().equals(id)) return MoveResult.OCCUPIED;

        return update(id, chest -> chest.withPosition(position)).isPresent() ? MoveResult.MOVED : MoveResult.TREASURE_GONE;
    }

    public @NotNull AccessResult checkAccess(@NotNull Player player, @NotNull TreasureChest chest) {
        if (chest.requiresPermission() && !player.hasPermission(chest.permission())) return AccessResult.NO_PERMISSION;

        Optional<Duration> remaining = cooldowns.remaining(player.getUniqueId(), chest);
        if (remaining.isEmpty()) return AccessResult.NOT_LOADED;
        return remaining.get().isPositive() ? AccessResult.ON_COOLDOWN : AccessResult.ALLOWED;
    }

    public @NotNull Duration remainingCooldown(@NotNull Player player, @NotNull TreasureChest chest) {
        return cooldowns.remaining(player.getUniqueId(), chest).orElse(Duration.ZERO);
    }

    public void recordLooted(@NotNull Player player, @NotNull TreasureChest chest) {
        if (chest.hasCooldown()) cooldowns.recordOpen(player.getUniqueId(), chest.id());
    }

    private void persist() {
        async.run(() -> store.saveAll(treasures::values)).exceptionally(error -> {
            logger.error("Could not save treasures.yml", error);
            return null;
        });
    }
}
