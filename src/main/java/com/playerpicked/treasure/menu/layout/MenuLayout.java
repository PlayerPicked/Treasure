package com.playerpicked.treasure.menu.layout;

import com.playerpicked.treasure.config.YamlFile;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record MenuLayout(
        @NotNull String title,
        int size,
        @NotNull Map<String, MenuItemTemplate> items,
        @Nullable SlotPattern contentSlots
) {
    private static final int ROW_LENGTH = 9;

    public static @NotNull MenuLayout read(@NotNull YamlFile guis, @NotNull String menuKey) {
        Section menuSection = guis.getSection(menuKey);
        if (menuSection == null) throw new IllegalStateException("guis.yml has no menu named '" + menuKey + "'");

        Map<String, MenuItemTemplate> items = new LinkedHashMap<>();
        Section itemsSection = menuSection.getSection("items");
        if (itemsSection != null) {
            for (Object itemKey : itemsSection.getKeys()) {
                Section itemSection = itemsSection.getSection(itemKey.toString());
                if (itemSection != null) items.put(itemKey.toString(), MenuItemTemplate.parse(itemSection));
            }
        }

        int rows = Math.clamp(menuSection.getInt("rows", 6), 1, 6);
        return new MenuLayout(
                menuSection.getString("title", ""),
                rows * ROW_LENGTH,
                items,
                SlotPattern.parse(menuSection.get("content-slots"))
        );
    }

    public @NotNull List<Integer> resolveContentSlots() {
        return contentSlots != null ? contentSlots.resolve(size) : List.of();
    }

    public @NotNull MenuItemTemplate requireItem(@NotNull String key) {
        MenuItemTemplate template = items.get(key);
        if (template == null) throw new IllegalStateException("guis.yml is missing the '" + key + "' item");
        return template;
    }
}
