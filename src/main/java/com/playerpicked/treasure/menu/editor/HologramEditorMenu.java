package com.playerpicked.treasure.menu.editor;

import com.playerpicked.treasure.menu.ClickAction;
import com.playerpicked.treasure.menu.MenuContext;
import com.playerpicked.treasure.menu.MenuNavigator;
import com.playerpicked.treasure.menu.PaginatedMenu;
import com.playerpicked.treasure.menu.TreasureSummary;
import com.playerpicked.treasure.notification.SoundEffect;
import com.playerpicked.treasure.text.MessageKey;
import com.playerpicked.treasure.text.Messages;
import com.playerpicked.treasure.treasure.TreasureChest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

public final class HologramEditorMenu extends PaginatedMenu<HologramEditorMenu.Line> {
    private static final String BLANK_ANSWER = "blank";
    private static final double HEIGHT_STEP = 0.1;

    private final MenuNavigator navigator;
    private final String treasureId;
    private volatile TreasureChest chest;

    public HologramEditorMenu(@NotNull Player viewer, @NotNull MenuContext context, @NotNull MenuNavigator navigator, @NotNull TreasureChest chest) {
        super(viewer, context);
        this.navigator = navigator;
        this.treasureId = chest.id();
        this.chest = chest;
    }

    public record Line(int index, @NotNull String text) {
    }

    @Override
    protected @NotNull String layoutKey() {
        return "hologram-editor";
    }

    @Override
    protected @NotNull String entryItemKey() {
        return "line";
    }

    @Override
    protected @NotNull CompletableFuture<Void> loadData() {
        context.treasures().find(treasureId).ifPresent(current -> chest = current);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected @NotNull List<Line> entries() {
        List<String> lines = chest.hologram().lines();
        return IntStream.range(0, lines.size()).mapToObj(index -> new Line(index, lines.get(index))).toList();
    }

    @Override
    protected @NotNull TagResolver menuPlaceholders() {
        return TagResolver.resolver(TreasureSummary.placeholders(chest, context.messages()), pagePlaceholders());
    }

    @Override
    protected @NotNull TagResolver entryPlaceholders(@NotNull Line line) {
        return TagResolver.resolver(
                Placeholder.unparsed("number", String.valueOf(line.index() + 1)),
                Placeholder.component("line", preview(line.text())),
                Placeholder.unparsed("raw", line.text())
        );
    }

    @Override
    protected @Nullable String variantOf(@NotNull String itemKey) {
        return itemKey.equals("toggle") ? TreasureSummary.toggleVariant(chest.hologram().enabled()) : null;
    }

    @Override
    protected void onEntryClicked(@NotNull Line line, @NotNull ClickType clickType) {
        if (clickType == ClickType.DROP || clickType == ClickType.CONTROL_DROP) {
            changeLines(lines -> lines.remove(line.index()));
            return;
        }
        askLine(MessageKey.PROMPT_HOLOGRAM_LINE, text -> changeLines(lines -> {
            if (line.index() < lines.size()) lines.set(line.index(), text);
        }));
    }

    @Override
    protected @Nullable ClickAction buttonAction(@NotNull String itemKey) {
        return switch (itemKey) {
            case "previous-page" -> previousPageButton();
            case "next-page" -> nextPageButton();
            case "add-line" -> clickType -> {
                if (chest.hologram().lines().size() >= TreasureChest.HologramText.MAX_LINES) {
                    context.messages().send(viewer, MessageKey.HOLOGRAM_TOO_MANY_LINES);
                    return;
                }
                askLine(MessageKey.PROMPT_HOLOGRAM_LINE, text -> changeLines(lines -> lines.add(text)));
            };
            case "height" -> clickType -> change(current -> current.withHologram(current.hologram().raisedBy(clickType.isRightClick() ? -HEIGHT_STEP : HEIGHT_STEP)));
            case "toggle" -> clickType -> change(current -> current.withHologram(current.hologram().toggled()));
            case "back" -> clickType -> navigator.openEditor(viewer, treasureId);
            default -> null;
        };
    }

    private void askLine(@NotNull MessageKey question, @NotNull Consumer<String> onText) {
        context.prompts().ask(viewer, question, answer -> {
            onText.accept(answer.equalsIgnoreCase(BLANK_ANSWER) ? "" : answer);
            navigator.openHologramEditor(viewer, treasureId);
        });
    }

    private void changeLines(@NotNull Consumer<List<String>> edit) {
        change(current -> {
            List<String> lines = new ArrayList<>(current.hologram().lines());
            edit.accept(lines);
            return current.withHologram(current.hologram().withLines(lines));
        });
    }

    private void change(@NotNull UnaryOperator<TreasureChest> change) {
        context.treasures().update(treasureId, change).ifPresentOrElse(updated -> {
            chest = updated;
            context.sounds().play(viewer, SoundEffect.CLICK);
            redraw();
        }, () -> context.messages().send(viewer, MessageKey.TREASURE_GONE));
    }

    private @NotNull Component preview(@NotNull String line) {
        if (line.isEmpty()) return context.messages().single(MessageKey.DISPLAY_BLANK_LINE);

        return Messages.render(line, TagResolver.resolver(
                Placeholder.component("name", Messages.render(chest.name(), TagResolver.empty())),
                Placeholder.component("time-left", context.messages().single(MessageKey.HOLOGRAM_READY))
        ));
    }
}
