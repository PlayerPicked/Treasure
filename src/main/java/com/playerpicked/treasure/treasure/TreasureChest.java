package com.playerpicked.treasure.treasure;

import com.playerpicked.treasure.particle.ParticleShape;
import com.playerpicked.treasure.world.BlockPosition;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public record TreasureChest(
        @NotNull String id,
        @NotNull String name,
        @Nullable BlockPosition position,
        @NotNull String permission,
        @NotNull Duration cooldown,
        int size,
        @NotNull @Unmodifiable List<ItemStack> loot,
        @NotNull Pushback pushback,
        @NotNull HologramText hologram,
        @NotNull ParticleDisplay particles
) {
    public static final int MIN_SIZE = 9;
    public static final int MAX_SIZE = 54;

    public TreasureChest {
        loot = loot.stream().map(ItemStack::clone).toList();
        size = Math.clamp(size / 9 * 9, MIN_SIZE, MAX_SIZE);
    }

    public static @NotNull TreasureChest createDefault(@NotNull String id) {
        return new TreasureChest(
                id,
                "<#E2287B><bold>Treasure Chest",
                null,
                "",
                Duration.ofHours(1),
                27,
                List.of(),
                new Pushback(true, 1.0),
                new HologramText(false, List.of("<name>", "<time-left>"), 0.0),
                new ParticleDisplay(false, ParticleShape.HELIX)
        );
    }

    public static boolean isValidSize(int size) {
        return size % 9 == 0 && size >= MIN_SIZE && size <= MAX_SIZE;
    }

    public @NotNull Optional<BlockPosition> location() {
        return Optional.ofNullable(position);
    }

    public boolean isAt(@NotNull BlockPosition other) {
        return other.equals(position);
    }

    public boolean requiresPermission() {
        return !permission.isBlank();
    }

    public boolean hasCooldown() {
        return cooldown.isPositive();
    }

    public @NotNull @Unmodifiable List<ItemStack> lootCopy() {
        return loot.stream().map(ItemStack::clone).toList();
    }

    public @NotNull TreasureChest withName(@NotNull String newName) {
        return new TreasureChest(id, newName, position, permission, cooldown, size, loot, pushback, hologram, particles);
    }

    public @NotNull TreasureChest withPosition(@NotNull BlockPosition newPosition) {
        return new TreasureChest(id, name, newPosition, permission, cooldown, size, loot, pushback, hologram, particles);
    }

    public @NotNull TreasureChest withPermission(@NotNull String newPermission) {
        return new TreasureChest(id, name, position, newPermission, cooldown, size, loot, pushback, hologram, particles);
    }

    public @NotNull TreasureChest withCooldown(@NotNull Duration newCooldown) {
        return new TreasureChest(id, name, position, permission, newCooldown, size, loot, pushback, hologram, particles);
    }

    public @NotNull TreasureChest withSize(int newSize) {
        return new TreasureChest(id, name, position, permission, cooldown, newSize, loot, pushback, hologram, particles);
    }

    public @NotNull TreasureChest withLoot(@NotNull List<ItemStack> newLoot) {
        return new TreasureChest(id, name, position, permission, cooldown, size, newLoot, pushback, hologram, particles);
    }

    public @NotNull TreasureChest withPushback(@NotNull Pushback newPushback) {
        return new TreasureChest(id, name, position, permission, cooldown, size, loot, newPushback, hologram, particles);
    }

    public @NotNull TreasureChest withHologram(@NotNull HologramText newHologram) {
        return new TreasureChest(id, name, position, permission, cooldown, size, loot, pushback, newHologram, particles);
    }

    public @NotNull TreasureChest withParticles(@NotNull ParticleDisplay newParticles) {
        return new TreasureChest(id, name, position, permission, cooldown, size, loot, pushback, hologram, newParticles);
    }

    public record Pushback(boolean enabled, double strength) {
        public static final double MAX_STRENGTH = 5.0;

        public Pushback {
            strength = Math.clamp(strength, 0.0, MAX_STRENGTH);
        }

        public @NotNull Pushback toggled() {
            return new Pushback(!enabled, strength);
        }

        public @NotNull Pushback withStrength(double newStrength) {
            return new Pushback(enabled, newStrength);
        }
    }

    public record HologramText(boolean enabled, @NotNull @Unmodifiable List<String> lines, double height) {
        public static final int MAX_LINES = 20;

        public HologramText {
            lines = List.copyOf(lines);
            height = Math.round(Math.clamp(height, -5.0, 10.0) * 10.0) / 10.0;
        }

        public @NotNull HologramText toggled() {
            return new HologramText(!enabled, lines, height);
        }

        public @NotNull HologramText withLines(@NotNull List<String> newLines) {
            return new HologramText(enabled, newLines, height);
        }

        public @NotNull HologramText raisedBy(double amount) {
            return new HologramText(enabled, lines, height + amount);
        }

        public boolean isVisible() {
            return enabled && !lines.isEmpty();
        }
    }

    public record ParticleDisplay(boolean enabled, @NotNull ParticleShape shape) {
        public @NotNull ParticleDisplay toggled() {
            return new ParticleDisplay(!enabled, shape);
        }

        public @NotNull ParticleDisplay withShape(@NotNull ParticleShape newShape) {
            return new ParticleDisplay(enabled, newShape);
        }
    }
}
