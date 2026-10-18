package com.playerpicked.treasure.menu;

import com.playerpicked.treasure.config.ConfigNames;
import com.playerpicked.treasure.text.DurationText;
import com.playerpicked.treasure.text.MessageKey;
import com.playerpicked.treasure.text.Messages;
import com.playerpicked.treasure.treasure.TreasureChest;
import com.playerpicked.treasure.world.BlockPosition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class TreasureSummary {
    private TreasureSummary() { }

    public static @NotNull TagResolver placeholders(@NotNull TreasureChest chest, @NotNull Messages messages) {
        return TagResolver.resolver(
                Placeholder.unparsed("id", chest.id()),
                Placeholder.component("name", Messages.render(chest.name(), TagResolver.empty())),
                Placeholder.component("location", chest.location().map(position -> location(position, messages)).orElseGet(() -> messages.single(MessageKey.DISPLAY_NOT_SET))),
                Placeholder.component("permission", chest.requiresPermission() ? Component.text(chest.permission()) : messages.single(MessageKey.DISPLAY_NONE)),
                Placeholder.component("cooldown", chest.hasCooldown() ? Component.text(DurationText.format(chest.cooldown())) : messages.single(MessageKey.DISPLAY_NONE)),
                Placeholder.unparsed("size", String.valueOf(chest.size())),
                Placeholder.unparsed("loot-count", String.valueOf(chest.loot().size())),
                Placeholder.component("pushback", toggle(chest.pushback().enabled(), messages)),
                Placeholder.unparsed("pushback-strength", String.format(Locale.ROOT, "%.1f", chest.pushback().strength())),
                Placeholder.component("hologram", toggle(chest.hologram().enabled(), messages)),
                Placeholder.unparsed("hologram-lines", String.valueOf(chest.hologram().lines().size())),
                Placeholder.unparsed("hologram-height", String.format(Locale.ROOT, "%.1f", chest.hologram().height())),
                Placeholder.component("particles", toggle(chest.particles().enabled(), messages)),
                Placeholder.component("particle-shape", messages.single("display.particle-shape." + ConfigNames.of(chest.particles().shape())))
        );
    }

    public static @NotNull String toggleVariant(boolean enabled) {
        return enabled ? "on" : "off";
    }

    private static @NotNull Component toggle(boolean enabled, @NotNull Messages messages) {
        return messages.single(enabled ? MessageKey.DISPLAY_ON : MessageKey.DISPLAY_OFF);
    }

    private static @NotNull Component location(@NotNull BlockPosition position, @NotNull Messages messages) {
        return messages.single(MessageKey.DISPLAY_LOCATION, TagResolver.resolver(
                Placeholder.unparsed("world", position.world()),
                Placeholder.unparsed("x", String.valueOf(position.x())),
                Placeholder.unparsed("y", String.valueOf(position.y())),
                Placeholder.unparsed("z", String.valueOf(position.z()))
        ));
    }
}
