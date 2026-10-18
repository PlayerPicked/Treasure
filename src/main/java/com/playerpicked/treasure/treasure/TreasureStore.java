package com.playerpicked.treasure.treasure;

import com.playerpicked.treasure.particle.ParticleShape;
import com.playerpicked.treasure.world.BlockPosition;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class TreasureStore {
    private static final String ROOT = "treasures";

    private final File file;
    private final Logger logger;

    public TreasureStore(@NotNull File file, @NotNull Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    public synchronized @NotNull List<TreasureChest> loadAll() {
        YamlDocument document = open();
        Section treasures = document.getSection(ROOT);
        if (treasures == null) return List.of();

        List<TreasureChest> loaded = new ArrayList<>();
        for (Object id : treasures.getKeys()) {
            Section section = treasures.getSection(id.toString());
            if (section == null) continue;

            try {
                loaded.add(read(id.toString(), section));
            } catch (RuntimeException exception) {
                logger.warn("Skipping treasure '{}' in {}: {}", id, file.getName(), exception.getMessage());
            }
        }
        return loaded;
    }

    public synchronized void saveAll(@NotNull Supplier<Collection<TreasureChest>> currentTreasures) {
        YamlDocument document = open();
        document.remove(ROOT);
        document.createSection(ROOT);

        for (TreasureChest chest : currentTreasures.get()) write(document, ROOT + "." + chest.id(), chest);

        try {
            document.save();
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not save " + file.getName(), exception);
        }
    }

    private @NotNull YamlDocument open() {
        try {
            return YamlDocument.create(file);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read " + file.getName(), exception);
        }
    }

    private static @NotNull TreasureChest read(@NotNull String id, @NotNull Section section) {
        return new TreasureChest(
                id,
                section.getString("name", id),
                BlockPosition.parse(section.getString("location", "")).orElse(null),
                section.getString("permission", ""),
                Duration.ofSeconds(Math.max(0, section.getLong("cooldown-seconds", 0L))),
                section.getInt("size", 27),
                readLoot(section.getStringList("loot")),
                new TreasureChest.Pushback(section.getBoolean("pushback.enabled", true), section.getDouble("pushback.strength", 1.0)),
                new TreasureChest.HologramText(section.getBoolean("hologram.enabled", false), section.getStringList("hologram.lines"), section.getDouble("hologram.height", 0.0)),
                new TreasureChest.ParticleDisplay(section.getBoolean("particles.enabled", false), readShape(section.getString("particles.shape", "helix")))
        );
    }

    private static void write(@NotNull YamlDocument document, @NotNull String route, @NotNull TreasureChest chest) {
        document.set(route + ".name", chest.name());
        document.set(route + ".location", chest.location().map(BlockPosition::serialize).orElse(""));
        document.set(route + ".permission", chest.permission());
        document.set(route + ".cooldown-seconds", chest.cooldown().toSeconds());
        document.set(route + ".size", chest.size());
        document.set(route + ".pushback.enabled", chest.pushback().enabled());
        document.set(route + ".pushback.strength", chest.pushback().strength());
        document.set(route + ".hologram.enabled", chest.hologram().enabled());
        document.set(route + ".hologram.height", chest.hologram().height());
        document.set(route + ".hologram.lines", chest.hologram().lines());
        document.set(route + ".particles.enabled", chest.particles().enabled());
        document.set(route + ".particles.shape", chest.particles().shape().name().toLowerCase(Locale.ROOT));
        document.set(route + ".loot", chest.loot().stream().map(TreasureStore::encode).toList());
    }

    private static @NotNull List<ItemStack> readLoot(@NotNull List<String> encodedItems) {
        return encodedItems.stream().map(TreasureStore::decode).toList();
    }

    private static @NotNull String encode(@NotNull ItemStack item) {
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    private static @NotNull ItemStack decode(@NotNull String encoded) {
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
    }

    private static @NotNull ParticleShape readShape(@NotNull String name) {
        return ParticleShape.byName(name).orElse(ParticleShape.HELIX);
    }
}
