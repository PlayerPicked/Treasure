package com.playerpicked.treasure.menu.editor;

import com.playerpicked.treasure.menu.MenuContext;
import com.playerpicked.treasure.notification.SoundEffect;
import com.playerpicked.treasure.text.DurationText;
import com.playerpicked.treasure.text.MessageKey;
import com.playerpicked.treasure.text.Messages;
import com.playerpicked.treasure.treasure.TreasureChest;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.function.UnaryOperator;

final class TreasureEditPrompts {
    private static final String NO_PERMISSION_ANSWER = "none";

    private final Player viewer;
    private final MenuContext context;
    private final String treasureId;
    private final Runnable reopenEditor;

    TreasureEditPrompts(@NotNull Player viewer, @NotNull MenuContext context, @NotNull String treasureId, @NotNull Runnable reopenEditor) {
        this.viewer = viewer;
        this.context = context;
        this.treasureId = treasureId;
        this.reopenEditor = reopenEditor;
    }

    void askName() {
        context.prompts().ask(viewer, MessageKey.PROMPT_NAME, name -> apply(chest -> chest.withName(name), MessageKey.NAME_SET,
                Placeholder.component("name", Messages.render(name, TagResolver.empty()))));
    }

    void askPermission() {
        context.prompts().ask(viewer, MessageKey.PROMPT_PERMISSION, answer -> {
            if (answer.equalsIgnoreCase(NO_PERMISSION_ANSWER)) {
                apply(chest -> chest.withPermission(""), MessageKey.PERMISSION_CLEARED);
            } else {
                apply(chest -> chest.withPermission(answer), MessageKey.PERMISSION_SET, Placeholder.unparsed("permission", answer));
            }
        });
    }

    void askCooldown() {
        context.prompts().ask(viewer, MessageKey.PROMPT_COOLDOWN, answer -> {
            Optional<Duration> cooldown = DurationText.parse(answer);
            if (cooldown.isEmpty()) {
                reject(MessageKey.INVALID_DURATION, Placeholder.unparsed("input", answer));
                return;
            }
            apply(chest -> chest.withCooldown(cooldown.get()), MessageKey.COOLDOWN_SET, Placeholder.unparsed("cooldown", DurationText.format(cooldown.get())));
        });
    }

    void askSize() {
        context.prompts().ask(viewer, MessageKey.PROMPT_SIZE, answer -> {
            Optional<Integer> size = parseInt(answer).filter(TreasureChest::isValidSize);
            if (size.isEmpty()) {
                reject(MessageKey.INVALID_SIZE, Placeholder.unparsed("input", answer));
                return;
            }
            apply(chest -> chest.withSize(size.get()), MessageKey.SIZE_SET, Placeholder.unparsed("size", String.valueOf(size.get())));
        });
    }

    void askPushbackStrength() {
        context.prompts().ask(viewer, MessageKey.PROMPT_PUSHBACK, answer -> {
            Optional<Double> strength = parseDouble(answer).filter(value -> value >= 0 && value <= TreasureChest.Pushback.MAX_STRENGTH);
            if (strength.isEmpty()) {
                reject(MessageKey.INVALID_PUSHBACK, Placeholder.unparsed("input", answer));
                return;
            }
            apply(chest -> chest.withPushback(chest.pushback().withStrength(strength.get())), MessageKey.PUSHBACK_SET,
                    Placeholder.unparsed("strength", String.format(Locale.ROOT, "%.1f", strength.get())));
        });
    }

    void pickLocation() {
        context.locations().pick(viewer, position -> {
            switch (context.treasures().moveTo(treasureId, position)) {
                case MOVED -> {
                    confirm(MessageKey.LOCATION_SET);
                    reopenEditor.run();
                }
                case OCCUPIED -> reject(MessageKey.LOCATION_OCCUPIED);
                case TREASURE_GONE -> context.messages().send(viewer, MessageKey.TREASURE_GONE);
            }
        });
    }

    private void apply(@NotNull UnaryOperator<TreasureChest> change, @NotNull MessageKey confirmation, @NotNull TagResolver... placeholders) {
        if (context.treasures().update(treasureId, change).isEmpty()) {
            context.messages().send(viewer, MessageKey.TREASURE_GONE);
            return;
        }
        confirm(confirmation, placeholders);
        reopenEditor.run();
    }

    private void confirm(@NotNull MessageKey confirmation, @NotNull TagResolver... placeholders) {
        context.messages().send(viewer, confirmation, placeholders);
        context.sounds().play(viewer, SoundEffect.SAVED);
    }

    private void reject(@NotNull MessageKey reason, @NotNull TagResolver... placeholders) {
        context.messages().send(viewer, reason, placeholders);
        context.sounds().play(viewer, SoundEffect.DENIED);
        reopenEditor.run();
    }

    private static @NotNull Optional<Integer> parseInt(@NotNull String text) {
        try {
            return Optional.of(Integer.parseInt(text.trim()));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private static @NotNull Optional<Double> parseDouble(@NotNull String text) {
        try {
            return Optional.of(Double.parseDouble(text.trim().replace(',', '.')));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}
