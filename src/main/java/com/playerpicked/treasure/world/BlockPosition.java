package com.playerpicked.treasure.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record BlockPosition(@NotNull String world, int x, int y, int z) {

    public static @NotNull BlockPosition of(@NotNull Block block) {
        return new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public static @NotNull Optional<BlockPosition> parse(@NotNull String text) {
        String[] parts = text.split(",");
        if (parts.length != 4) return Optional.empty();

        try {
            return Optional.of(new BlockPosition(parts[0], Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()), Integer.parseInt(parts[3].trim())));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public @NotNull String serialize() {
        return world + "," + x + "," + y + "," + z;
    }

    public @NotNull Optional<World> findWorld() {
        return Optional.ofNullable(Bukkit.getWorld(world));
    }

    public @NotNull Optional<Location> centerAbove(double heightAboveBlock) {
        return findWorld().map(loaded -> new Location(loaded, x + 0.5, y + heightAboveBlock, z + 0.5));
    }

    public boolean isIn(@NotNull World other) {
        return other.getName().equals(world);
    }

    public double distanceSquared(@NotNull Location location) {
        double dx = location.getX() - (x + 0.5);
        double dy = location.getY() - (y + 0.5);
        double dz = location.getZ() - (z + 0.5);
        return dx * dx + dy * dy + dz * dz;
    }
}
