package com.playerpicked.treasure.notification;

import com.playerpicked.treasure.config.PluginConfig;
import com.playerpicked.treasure.config.SoundSettings;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Optional;

public final class SoundPlayer {
    private final PluginConfig config;
    private final Logger logger;

    public SoundPlayer(@NotNull PluginConfig config, @NotNull Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public void play(@NotNull Player player, @NotNull SoundEffect effect) {
        Optional<SoundSettings> settings = config.sound(effect);
        if (settings.isEmpty()) return;

        @Subst("minecraft:block.chest.open") String key = settings.get().key();
        try {
            player.playSound(Sound.sound(Key.key(key), Sound.Source.MASTER, settings.get().volume(), settings.get().pitch()));
        } catch (InvalidKeyException exception) {
            logger.warn("Invalid sound '{}' for {} in config.yml", key, effect);
        }
    }
}
