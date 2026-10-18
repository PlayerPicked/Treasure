package com.playerpicked.treasure.loot;

import com.playerpicked.treasure.config.PluginConfig;
import com.playerpicked.treasure.notification.SoundEffect;
import com.playerpicked.treasure.notification.SoundPlayer;
import com.playerpicked.treasure.scheduler.ServerScheduler;
import com.playerpicked.treasure.text.MessageKey;
import com.playerpicked.treasure.text.Messages;
import com.playerpicked.treasure.treasure.TreasureChest;
import com.playerpicked.treasure.treasure.TreasureService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class LootInventories {
    private final TreasureService treasures;
    private final PluginConfig config;
    private final Messages messages;
    private final SoundPlayer sounds;
    private final ServerScheduler scheduler;

    public LootInventories(@NotNull TreasureService treasures, @NotNull PluginConfig config, @NotNull Messages messages,
                           @NotNull SoundPlayer sounds, @NotNull ServerScheduler scheduler) {
        this.treasures = treasures;
        this.config = config;
        this.messages = messages;
        this.sounds = sounds;
        this.scheduler = scheduler;
    }

    public void openLoot(@NotNull Player player, @NotNull TreasureChest chest) {
        player.openInventory(new LootView(player, chest, treasures, messages, sounds, config.placeLootRandomly()).getInventory());
    }

    public void openEditor(@NotNull Player player, @NotNull TreasureChest chest, @NotNull Runnable afterSave) {
        LootEditor editor = new LootEditor(messages.single(MessageKey.LOOT_EDITOR_TITLE, nameOf(chest)), chest.size(), chest.lootCopy(), loot -> {
            treasures.update(chest.id(), current -> current.withLoot(loot));
            messages.send(player, MessageKey.LOOT_SAVED);
            sounds.play(player, SoundEffect.SAVED);
            scheduler.runForPlayerLater(player, 1, afterSave);
        });
        player.openInventory(editor.getInventory());
    }

    public void openPreview(@NotNull Player player, @NotNull TreasureChest chest) {
        player.openInventory(new LootPreview(messages.single(MessageKey.LOOT_PREVIEW_TITLE, nameOf(chest)), chest.size(), chest.lootCopy()).getInventory());
    }

    private static @NotNull TagResolver nameOf(@NotNull TreasureChest chest) {
        return Placeholder.component("name", Messages.render(chest.name(), TagResolver.empty()));
    }
}
