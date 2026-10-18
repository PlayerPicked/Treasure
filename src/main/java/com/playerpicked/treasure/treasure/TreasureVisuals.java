package com.playerpicked.treasure.treasure;

import com.playerpicked.treasure.config.PluginConfig;
import com.playerpicked.treasure.hologram.HologramRegistry;
import com.playerpicked.treasure.hologram.HologramText;
import com.playerpicked.treasure.particle.ParticleEffects;
import com.playerpicked.treasure.particle.ParticleShape;
import org.jetbrains.annotations.NotNull;

public final class TreasureVisuals {
    private static final double HOLOGRAM_HEIGHT_ABOVE_BLOCK = 1.2;
    private static final double PARTICLE_HEIGHT_ABOVE_BLOCK = 0.5;

    private final HologramRegistry holograms;
    private final ParticleEffects particles;
    private final HologramText hologramText;
    private final PluginConfig config;

    public TreasureVisuals(@NotNull HologramRegistry holograms, @NotNull ParticleEffects particles,
                           @NotNull HologramText hologramText, @NotNull PluginConfig config) {
        this.holograms = holograms;
        this.particles = particles;
        this.hologramText = hologramText;
        this.config = config;
    }

    public void show(@NotNull TreasureChest chest) {
        hide(chest.id());

        if (chest.hologram().isVisible()) {
            chest.location()
                    .flatMap(position -> position.centerAbove(HOLOGRAM_HEIGHT_ABOVE_BLOCK + chest.hologram().height()))
                    .ifPresent(anchor -> holograms.show(chest.id(), anchor, config.hologram(), viewer -> hologramText.render(viewer, chest)));
        }

        if (chest.particles().enabled()) {
            ParticleShape shape = chest.particles().shape();
            chest.location()
                    .flatMap(position -> position.centerAbove(PARTICLE_HEIGHT_ABOVE_BLOCK))
                    .ifPresent(center -> particles.show(chest.id(), center, shape, config.particleFor(shape)));
        }
    }

    public void hide(@NotNull String treasureId) {
        holograms.hide(treasureId);
        particles.hide(treasureId);
    }

    public void shutdown() {
        holograms.shutdown();
        particles.shutdown();
    }
}
