package com.playerpicked.treasure.menu.layout;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

public final class SlotPattern {
    private static final int ROW_LENGTH = 9;

    private final String definition;

    private SlotPattern(@NotNull String definition) {
        this.definition = definition.toLowerCase(Locale.ROOT).trim();
    }

    public static @Nullable SlotPattern parse(@Nullable Object rawDefinition) {
        return rawDefinition == null ? null : new SlotPattern(rawDefinition.toString());
    }

    public boolean isFill() {
        return definition.equals("fill");
    }

    public @NotNull @Unmodifiable List<Integer> resolve(int inventorySize) {
        int rows = inventorySize / ROW_LENGTH;
        int lastRowStart = (rows - 1) * ROW_LENGTH;

        List<Integer> slots = switch (definition) {
            case "fill" -> List.of();
            case "top" -> range(0, ROW_LENGTH - 1);
            case "bottom" -> range(lastRowStart, inventorySize - 1);
            case "left" -> IntStream.range(0, rows).map(row -> row * ROW_LENGTH).boxed().toList();
            case "right" -> IntStream.range(0, rows).map(row -> row * ROW_LENGTH + ROW_LENGTH - 1).boxed().toList();
            case "corners" -> List.of(0, ROW_LENGTH - 1, lastRowStart, inventorySize - 1);
            case "border" -> IntStream.range(0, inventorySize)
                    .filter(slot -> slot < ROW_LENGTH || slot >= lastRowStart
                            || slot % ROW_LENGTH == 0 || slot % ROW_LENGTH == ROW_LENGTH - 1)
                    .boxed().toList();
            default -> definition.startsWith("grid:") ? parseGrid() : parseNumbersAndRanges();
        };

        return slots.stream().filter(slot -> slot >= 0 && slot < inventorySize).distinct().toList();
    }

    private @NotNull List<Integer> parseGrid() {
        String[] sizeAndStart = definition.substring("grid:".length()).split("@");
        String[] columnsAndRows = sizeAndStart[0].split("x");
        try {
            int columns = Integer.parseInt(columnsAndRows[0].trim());
            int rows = Integer.parseInt(columnsAndRows[1].trim());
            int firstSlot = sizeAndStart.length > 1 ? Integer.parseInt(sizeAndStart[1].trim()) : 0;

            List<Integer> slots = new ArrayList<>();
            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < columns; column++) {
                    slots.add(firstSlot + row * ROW_LENGTH + column);
                }
            }
            return slots;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException exception) {
            return List.of();
        }
    }

    private @NotNull List<Integer> parseNumbersAndRanges() {
        List<Integer> slots = new ArrayList<>();
        for (String part : definition.split(",")) {
            try {
                String[] bounds = part.trim().split("-");
                int start = Integer.parseInt(bounds[0].trim());
                int end = bounds.length > 1 ? Integer.parseInt(bounds[1].trim()) : start;
                slots.addAll(range(Math.min(start, end), Math.max(start, end)));
            } catch (NumberFormatException ignored) {}
        }
        return slots;
    }

    private static @NotNull @Unmodifiable List<Integer> range(int firstInclusive, int lastInclusive) {
        return IntStream.rangeClosed(firstInclusive, lastInclusive).boxed().toList();
    }
}
