package com.playerpicked.treasure.setup;

import com.playerpicked.treasure.config.PluginConfig;
import com.playerpicked.treasure.scheduler.ServerScheduler;
import com.playerpicked.treasure.text.MessageKey;
import com.playerpicked.treasure.text.Messages;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ChatPrompts implements Listener {
    private final PluginConfig config;
    private final Messages messages;
    private final ServerScheduler scheduler;
    private final Map<UUID, Prompt> pending = new ConcurrentHashMap<>();

    public ChatPrompts(@NotNull PluginConfig config, @NotNull Messages messages, @NotNull ServerScheduler scheduler) {
        this.config = config;
        this.messages = messages;
        this.scheduler = scheduler;
    }

    public void ask(@NotNull Player player, @NotNull MessageKey question, @NotNull Consumer<String> onAnswer) {
        Prompt prompt = new Prompt(onAnswer);
        pending.put(player.getUniqueId(), prompt);

        player.closeInventory();
        messages.send(player, question);
        messages.send(player, MessageKey.PROMPT_HOW_TO_CANCEL, Placeholder.unparsed("word", config.promptCancelWord()));

        scheduler.runForPlayerLater(player, config.promptTimeout().toSeconds() * 20, () -> {
            if (pending.remove(player.getUniqueId(), prompt)) messages.send(player, MessageKey.PROMPT_EXPIRED);
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(@NotNull AsyncChatEvent event) {
        Player player = event.getPlayer();
        Prompt prompt = pending.remove(player.getUniqueId());
        if (prompt == null) return;

        event.setCancelled(true);
        String answer = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        if (answer.equalsIgnoreCase(config.promptCancelWord())) {
            messages.send(player, MessageKey.PROMPT_CANCELLED);
            return;
        }
        scheduler.runForPlayer(player, () -> prompt.onAnswer().accept(answer));
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }

    // A class, not a record: an expired timeout must only remove its own prompt, so equality is identity
    private static final class Prompt {
        private final Consumer<String> onAnswer;

        private Prompt(@NotNull Consumer<String> onAnswer) {
            this.onAnswer = onAnswer;
        }

        @NotNull Consumer<String> onAnswer() {
            return onAnswer;
        }
    }
}
