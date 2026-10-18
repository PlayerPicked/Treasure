package com.playerpicked.treasure.hologram;

import com.playerpicked.treasure.cooldown.CooldownService;
import com.playerpicked.treasure.placeholder.ExternalPlaceholders;
import com.playerpicked.treasure.text.DurationText;
import com.playerpicked.treasure.text.MessageKey;
import com.playerpicked.treasure.text.Messages;
import com.playerpicked.treasure.treasure.TreasureChest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public final class HologramText {
    private final Messages messages;
    private final CooldownService cooldowns;
    private final ExternalPlaceholders externalPlaceholders;

    public HologramText(@NotNull Messages messages, @NotNull CooldownService cooldowns, @NotNull ExternalPlaceholders externalPlaceholders) {
        this.messages = messages;
        this.cooldowns = cooldowns;
        this.externalPlaceholders = externalPlaceholders;
    }

    public @NotNull Component render(@NotNull Player viewer, @NotNull TreasureChest chest) {
        TagResolver treasurePlaceholders = TagResolver.resolver(
                Placeholder.component("name", Messages.render(chest.name(), TagResolver.empty())),
                Placeholder.component("time-left", timeLeft(viewer, chest))
        );

        Component text = Component.empty();
        boolean firstLine = true;
        for (String line : chest.hologram().lines()) {
            ExternalPlaceholders.PreparedText prepared = externalPlaceholders.prepare(viewer, line);
            Component rendered = Messages.render(prepared.miniMessage(), TagResolver.resolver(treasurePlaceholders, prepared.placeholders()));

            text = firstLine ? rendered : text.append(Component.newline()).append(rendered);
            firstLine = false;
        }
        return text;
    }

    private @NotNull Component timeLeft(@NotNull Player viewer, @NotNull TreasureChest chest) {
        Duration remaining = cooldowns.remaining(viewer.getUniqueId(), chest).orElse(Duration.ZERO);
        if (!remaining.isPositive()) return messages.single(MessageKey.HOLOGRAM_READY);

        return messages.single(MessageKey.HOLOGRAM_TIME_LEFT, Placeholder.unparsed("time", DurationText.format(remaining)));
    }
}
