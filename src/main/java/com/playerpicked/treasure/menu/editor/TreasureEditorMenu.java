package com.playerpicked.treasure.menu.editor;

import com.playerpicked.treasure.menu.ClickAction;
import com.playerpicked.treasure.menu.Menu;
import com.playerpicked.treasure.menu.MenuContext;
import com.playerpicked.treasure.menu.MenuNavigator;
import com.playerpicked.treasure.menu.TreasureSummary;
import com.playerpicked.treasure.notification.SoundEffect;
import com.playerpicked.treasure.text.MessageKey;
import com.playerpicked.treasure.treasure.TreasureChest;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

public final class TreasureEditorMenu extends Menu {
    private final MenuNavigator navigator;
    private final TreasureEditPrompts prompts;
    private final String treasureId;
    private volatile TreasureChest chest;

    public TreasureEditorMenu(@NotNull Player viewer, @NotNull MenuContext context, @NotNull MenuNavigator navigator, @NotNull TreasureChest chest) {
        super(viewer, context);
        this.navigator = navigator;
        this.prompts = new TreasureEditPrompts(viewer, context, chest.id(), () -> navigator.openEditor(viewer, chest.id()));
        this.treasureId = chest.id();
        this.chest = chest;
    }

    @Override
    protected @NotNull String layoutKey() {
        return "editor";
    }

    @Override
    protected @NotNull CompletableFuture<Void> loadData() {
        context.treasures().find(treasureId).ifPresent(current -> chest = current);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected @NotNull TagResolver menuPlaceholders() {
        return TreasureSummary.placeholders(chest, context.messages());
    }

    @Override
    protected @Nullable String variantOf(@NotNull String itemKey) {
        return switch (itemKey) {
            case "pushback" -> TreasureSummary.toggleVariant(chest.pushback().enabled());
            case "hologram" -> TreasureSummary.toggleVariant(chest.hologram().enabled());
            case "particles" -> TreasureSummary.toggleVariant(chest.particles().enabled());
            default -> null;
        };
    }

    @Override
    protected @Nullable ClickAction buttonAction(@NotNull String itemKey) {
        return switch (itemKey) {
            case "name" -> clickType -> prompts.askName();
            case "permission" -> clickType -> prompts.askPermission();
            case "cooldown" -> clickType -> prompts.askCooldown();
            case "size" -> clickType -> prompts.askSize();
            case "location" -> clickType -> prompts.pickLocation();
            case "pushback" -> clickType -> {
                if (isDrop(clickType)) toggle(current -> current.withPushback(current.pushback().toggled()));
                else prompts.askPushbackStrength();
            };
            case "hologram" -> clickType -> {
                if (isDrop(clickType)) toggle(current -> current.withHologram(current.hologram().toggled()));
                else navigator.openHologramEditor(viewer, treasureId);
            };
            case "particles" -> this::changeParticles;
            case "loot" -> clickType -> context.loot().openEditor(viewer, chest, () -> navigator.openEditor(viewer, treasureId));
            case "back" -> clickType -> navigator.openOverview(viewer);
            default -> null;
        };
    }

    private void changeParticles(@NotNull ClickType clickType) {
        if (isDrop(clickType)) toggle(current -> current.withParticles(current.particles().toggled()));
        else if (clickType.isRightClick()) toggle(current -> current.withParticles(current.particles().withShape(current.particles().shape().previous())));
        else toggle(current -> current.withParticles(current.particles().withShape(current.particles().shape().next())));
    }

    private void toggle(@NotNull UnaryOperator<TreasureChest> change) {
        context.treasures().update(treasureId, change).ifPresentOrElse(updated -> {
            chest = updated;
            context.sounds().play(viewer, SoundEffect.CLICK);
            redraw();
        }, () -> {
            context.messages().send(viewer, MessageKey.TREASURE_GONE);
            viewer.closeInventory();
        });
    }

    private static boolean isDrop(@NotNull ClickType clickType) {
        return clickType == ClickType.DROP || clickType == ClickType.CONTROL_DROP;
    }
}
