package com.playerpicked.treasure.particle;

import com.playerpicked.treasure.scheduler.ServerScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ParticleEffects {
    private final ServerScheduler scheduler;
    private final Map<String, ScheduledTask> effects = new ConcurrentHashMap<>();

    public ParticleEffects(@NotNull ServerScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void show(@NotNull String id, @NotNull Location center, @NotNull ParticleShape shape, @NotNull Particle particle) {
        hide(id);
        effects.put(id, scheduler.runAtTimer(center, 1, 1, new Animation(center.clone(), shape, particle)));
    }

    public void hide(@NotNull String id) {
        ScheduledTask task = effects.remove(id);
        if (task != null) task.cancel();
    }

    public void shutdown() {
        effects.values().forEach(ScheduledTask::cancel);
        effects.clear();
    }

    private static final class Animation implements Runnable {
        private final Location center;
        private final ParticleShape shape;
        private final Particle particle;
        private long tick;

        private Animation(@NotNull Location center, @NotNull ParticleShape shape, @NotNull Particle particle) {
            this.center = center;
            this.shape = shape;
            this.particle = particle;
        }

        @Override
        public void run() {
            World world = center.getWorld();
            long currentTick = tick++;
            if (!world.isChunkLoaded(center.getBlockX() >> 4, center.getBlockZ() >> 4) || world.getPlayers().isEmpty()) return;

            for (Vector offset : shape.pointsAt(currentTick)) {
                world.spawnParticle(particle, center.getX() + offset.getX(), center.getY() + offset.getY(), center.getZ() + offset.getZ(), 1, 0, 0, 0, 0);
            }
        }
    }
}
