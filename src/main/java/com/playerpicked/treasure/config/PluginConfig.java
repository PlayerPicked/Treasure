package com.playerpicked.treasure.config;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.entity.Display;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class PluginConfig {
    private static final List<String> DEFAULT_COMMAND_ALIASES = List.of("treasure");

    private final YamlFile file;

    public PluginConfig(@NotNull YamlFile file) {
        this.file = file;
    }

    public void reload() {
        file.reload();
    }

    public @NotNull List<String> commandAliases() {
        List<String> aliases = file.getStringList("command-aliases");
        return aliases.isEmpty() ? DEFAULT_COMMAND_ALIASES : aliases;
    }

    public boolean placeLootRandomly() {
        return file.getBoolean("loot.place-randomly", true);
    }

    public boolean toastEnabled() {
        return file.getBoolean("toast.enabled", true);
    }

    public @NotNull Material toastIcon() {
        return item(file.getString("toast.material", "CHEST"), Material.CHEST);
    }

    public boolean trackerEnabled() {
        return file.getBoolean("tracker.enabled", true);
    }

    public @NotNull Material trackerMaterial() {
        return item(file.getString("tracker.material", "COMPASS"), Material.COMPASS);
    }

    public @NotNull Duration promptTimeout() {
        return Duration.ofSeconds(Math.clamp(file.getLong("setup.prompt-timeout-seconds", 30), 5, 600));
    }

    public @NotNull String promptCancelWord() {
        return file.getString("setup.cancel-word", "cancel");
    }

    public @NotNull HologramSettings hologram() {
        return new HologramSettings(
                billboard(file.getString("hologram.billboard", "CENTER")),
                file.getBoolean("hologram.text-shadow", true),
                background(file.getString("hologram.background", "TRANSPARENT")),
                Math.clamp(file.getDouble("hologram.view-distance", 32), 1, 128),
                Math.clamp(file.getLong("hologram.update-interval-ticks", 20), 1, 1200)
        );
    }

    public @NotNull Particle particleFor(@NotNull Enum<?> shape) {
        String name = file.getString("particles." + ConfigNames.of(shape), "flame").toLowerCase(Locale.ROOT);
        NamespacedKey key = NamespacedKey.fromString(name);
        Particle particle = key != null ? Registry.PARTICLE_TYPE.get(key) : null;

        return particle != null && particle.getDataType() == Void.class ? particle : Particle.FLAME;
    }

    public @NotNull Optional<SoundSettings> sound(@NotNull Enum<?> effect) {
        String route = "sounds." + ConfigNames.of(effect);
        String key = file.getString(route + ".sound", "").trim();
        if (key.isEmpty()) return Optional.empty();

        return Optional.of(new SoundSettings(
                key,
                Math.clamp(file.getFloat(route + ".volume", 0.5f), 0f, 10f),
                Math.clamp(file.getFloat(route + ".pitch", 1f), 0.5f, 2f)
        ));
    }

    public @NotNull DatabaseSettings database() {
        DatabaseType type = file.getString("database.type", "h2").equalsIgnoreCase("mysql") ? DatabaseType.MYSQL : DatabaseType.H2;

        return new DatabaseSettings(
                type,
                file.getString("database.mysql.host", "localhost"),
                file.getInt("database.mysql.port", 3306),
                file.getString("database.mysql.database", "treasure"),
                file.getString("database.mysql.username", "root"),
                file.getString("database.mysql.password", ""),
                Math.max(2, file.getInt("database.mysql.pool-size", 10))
        );
    }

    private static @NotNull Material item(@NotNull String name, @NotNull Material fallback) {
        Material material = Material.matchMaterial(name);
        return material != null && material.isItem() ? material : fallback;
    }

    private static @NotNull Display.Billboard billboard(@NotNull String name) {
        try {
            return Display.Billboard.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Display.Billboard.CENTER;
        }
    }

    private static @Nullable Color background(@NotNull String text) {
        if (text.equalsIgnoreCase("DEFAULT")) return null;

        String hex = text.startsWith("#") ? text.substring(1) : text;
        try {
            if (hex.length() == 6) return Color.fromRGB(Integer.parseInt(hex, 16));
            if (hex.length() == 8) return Color.fromARGB((int) Long.parseLong(hex, 16));
        } catch (NumberFormatException ignored) { }

        return Color.fromARGB(0, 0, 0, 0);
    }
}
