package com.playerpicked.treasure.treasure;

import com.playerpicked.treasure.config.PluginConfig;
import com.playerpicked.treasure.loot.LootInventories;
import com.playerpicked.treasure.notification.SoundEffect;
import com.playerpicked.treasure.notification.SoundPlayer;
import com.playerpicked.treasure.notification.ToastSender;
import com.playerpicked.treasure.text.DurationText;
import com.playerpicked.treasure.text.MessageKey;
import com.playerpicked.treasure.text.Messages;
import com.playerpicked.treasure.world.BlockPosition;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class TreasureBlockListener implements Listener {
    private static final double PUSHBACK_LIFT = 0.3;

    private final TreasureService treasures;
    private final LootInventories loot;
    private final PluginConfig config;
    private final Messages messages;
    private final SoundPlayer sounds;
    private final ToastSender toasts;

    public TreasureBlockListener(@NotNull TreasureService treasures, @NotNull LootInventories loot, @NotNull PluginConfig config,
                                 @NotNull Messages messages, @NotNull SoundPlayer sounds, @NotNull ToastSender toasts) {
        this.treasures = treasures;
        this.loot = loot;
        this.config = config;
        this.messages = messages;
        this.sounds = sounds;
        this.toasts = toasts;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        TreasureChest chest = treasures.findAt(BlockPosition.of(block)).orElse(null);
        if (chest == null) return;

        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        switch (treasures.checkAccess(player, chest)) {
            case ALLOWED -> open(player, chest);
            case NO_PERMISSION -> deny(player, chest, MessageKey.TREASURE_NO_PERMISSION);
            case NOT_LOADED -> deny(player, chest, MessageKey.TREASURE_NOT_LOADED);
            case ON_COOLDOWN -> {
                String timeLeft = DurationText.format(treasures.remainingCooldown(player, chest));
                messages.send(player, MessageKey.TREASURE_ON_COOLDOWN, Placeholder.unparsed("time", timeLeft));
                sounds.play(player, SoundEffect.DENIED);
                pushBack(player, chest);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(@NotNull BlockBreakEvent event) {
        if (!isTreasure(event.getBlock())) return;

        event.setCancelled(true);
        messages.send(event.getPlayer(), MessageKey.TREASURE_PROTECTED);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(@NotNull EntityExplodeEvent event) {
        protectFrom(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(@NotNull BlockExplodeEvent event) {
        protectFrom(event.blockList());
    }

    private void open(@NotNull Player player, @NotNull TreasureChest chest) {
        loot.openLoot(player, chest);
        sounds.play(player, SoundEffect.OPEN);
        if (config.toastEnabled()) toasts.show(player, messages.single(MessageKey.TOAST_TITLE), config.toastIcon());
    }

    private void deny(@NotNull Player player, @NotNull TreasureChest chest, @NotNull MessageKey reason) {
        messages.send(player, reason);
        sounds.play(player, SoundEffect.DENIED);
        pushBack(player, chest);
    }

    private static void pushBack(@NotNull Player player, @NotNull TreasureChest chest) {
        TreasureChest.Pushback pushback = chest.pushback();
        if (!pushback.enabled() || pushback.strength() <= 0) return;

        chest.location().flatMap(position -> position.centerAbove(0.5)).ifPresent(center -> {
            Vector away = player.getLocation().toVector().subtract(center.toVector()).setY(0);
            if (away.lengthSquared() < 1.0E-4) away = player.getLocation().getDirection().multiply(-1).setY(0);

            player.setVelocity(away.normalize().multiply(pushback.strength()).setY(PUSHBACK_LIFT));
        });
    }

    private void protectFrom(@NotNull List<Block> explodedBlocks) {
        explodedBlocks.removeIf(this::isTreasure);
    }

    private boolean isTreasure(@NotNull Block block) {
        return treasures.findAt(BlockPosition.of(block)).isPresent();
    }
}
