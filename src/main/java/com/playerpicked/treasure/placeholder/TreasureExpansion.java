package com.playerpicked.treasure.placeholder;

import com.playerpicked.treasure.cooldown.CooldownService;
import com.playerpicked.treasure.text.DurationText;
import com.playerpicked.treasure.text.MessageKey;
import com.playerpicked.treasure.text.Messages;
import com.playerpicked.treasure.treasure.TreasureChest;
import com.playerpicked.treasure.treasure.TreasureService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public final class TreasureExpansion extends PlaceholderExpansion {
    private static final String TIME_LEFT = "time_left_";
    private static final String REMAINING = "remaining_";
    private static final String OPENED = "opened_";
    private static final String ALL = "all_";

    private final String version;
    private final TreasureService treasures;
    private final CooldownService cooldowns;
    private final Messages messages;

    public TreasureExpansion(@NotNull String version, @NotNull TreasureService treasures, @NotNull CooldownService cooldowns, @NotNull Messages messages) {
        this.version = version;
        this.treasures = treasures;
        this.cooldowns = cooldowns;
        this.messages = messages;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "treasure";
    }

    @Override
    public @NotNull String getAuthor() {
        return "coma112";
    }

    @Override
    public @NotNull String getVersion() {
        return version;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer offlinePlayer, @NotNull String params) {
        if (params.startsWith(ALL)) return String.valueOf(treasuresIn(params.substring(ALL.length())).size());
        if (!(offlinePlayer instanceof Player player)) return null;

        if (params.startsWith(TIME_LEFT)) return timeLeft(player, params.substring(TIME_LEFT.length()));
        if (params.startsWith(REMAINING)) return String.valueOf(countByCooldown(player, params.substring(REMAINING.length()), false));
        if (params.startsWith(OPENED)) return String.valueOf(countByCooldown(player, params.substring(OPENED.length()), true));
        return null;
    }

    private @NotNull String timeLeft(@NotNull Player player, @NotNull String treasureId) {
        Optional<TreasureChest> chest = treasures.find(treasureId);
        if (chest.isEmpty()) return "";

        Duration remaining = cooldowns.remaining(player.getUniqueId(), chest.get()).orElse(Duration.ZERO);
        if (!remaining.isPositive()) return legacy(messages.single(MessageKey.HOLOGRAM_READY));
        return legacy(messages.single(MessageKey.HOLOGRAM_TIME_LEFT, Placeholder.unparsed("time", DurationText.format(remaining))));
    }

    private long countByCooldown(@NotNull Player player, @NotNull String world, boolean onCooldown) {
        return treasuresIn(world).stream()
                .filter(chest -> cooldowns.remaining(player.getUniqueId(), chest).orElse(Duration.ZERO).isPositive() == onCooldown)
                .count();
    }

    private @NotNull List<TreasureChest> treasuresIn(@NotNull String world) {
        return treasures.all().stream()
                .filter(chest -> chest.location().filter(position -> position.world().equals(world)).isPresent())
                .toList();
    }

    private static @NotNull String legacy(@NotNull Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}
