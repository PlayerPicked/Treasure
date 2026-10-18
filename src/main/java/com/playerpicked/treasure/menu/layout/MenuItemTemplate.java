package com.playerpicked.treasure.menu.layout;

import com.playerpicked.treasure.text.Messages;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.tag.Inserting;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record MenuItemTemplate(
        @NotNull Material material,
        int amount,
        @Nullable String name,
        @NotNull List<String> lore,
        @Nullable SlotPattern slots,
        @Nullable String itemModel,
        @Nullable Float customModelData,
        boolean glow,
        int priority,
        @NotNull Map<String, MenuItemTemplate> variants
) {
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    public static @NotNull MenuItemTemplate parse(@NotNull Section section) {
        return parse(section, null);
    }

    private static @NotNull MenuItemTemplate parse(@NotNull Section section, @Nullable MenuItemTemplate base) {
        Material material = Material.matchMaterial(section.getString("material", "").toUpperCase(Locale.ROOT));
        Object customModelData = section.get("custom-model-data");

        MenuItemTemplate template = new MenuItemTemplate(
                material != null ? material : base != null ? base.material : Material.STONE,
                section.getInt("amount", base != null ? base.amount : 1),
                section.getString("name", base != null ? base.name : null),
                section.contains("lore") ? section.getStringList("lore") : base != null ? base.lore : List.of(),
                section.contains("slot") ? SlotPattern.parse(section.get("slot")) : base != null ? base.slots : null,
                section.getString("item-model", base != null ? base.itemModel : null),
                customModelData != null ? Float.valueOf(customModelData.toString()) : base != null ? base.customModelData : null,
                section.getBoolean("glow", base != null && base.glow),
                section.getInt("priority", base != null ? base.priority : 0),
                Map.of()
        );

        Section variantsSection = section.getSection("variants");
        if (base != null || variantsSection == null) return template;

        Map<String, MenuItemTemplate> variants = new HashMap<>();
        for (Object variantName : variantsSection.getKeys()) {
            Section variantSection = variantsSection.getSection(variantName.toString());
            if (variantSection != null) variants.put(variantName.toString(), parse(variantSection, template));
        }
        return template.withVariants(variants);
    }

    public boolean hasFixedSlots() {
        SlotPattern pattern = slots;
        return pattern != null && !pattern.isFill();
    }

    public boolean fillsEmptySlots() {
        SlotPattern pattern = slots;
        return pattern != null && pattern.isFill();
    }

    public @NotNull List<Integer> resolveSlots(int inventorySize) {
        SlotPattern pattern = slots;
        return pattern != null ? pattern.resolve(inventorySize) : List.of();
    }

    public @NotNull MenuItemTemplate variant(@Nullable String variantName) {
        return variantName == null ? this : variants.getOrDefault(variantName, this);
    }

    public @NotNull ItemStack render(@NotNull TagResolver placeholders) {
        ItemStack item = ItemStack.of(material, Math.clamp(amount, 1, material.getMaxStackSize()));
        item.editMeta(meta -> applyTo(meta, placeholders));
        return item;
    }

    private void applyTo(@NotNull ItemMeta meta, @NotNull TagResolver placeholders) {
        if (name != null) meta.displayName(withoutItalics(Messages.render(name, placeholders)));
        if (!lore.isEmpty()) meta.lore(renderLore(placeholders));
        if (itemModel != null) meta.setItemModel(NamespacedKey.fromString(itemModel));
        if (glow) meta.setEnchantmentGlintOverride(true);

        if (customModelData != null) {
            CustomModelDataComponent modelData = meta.getCustomModelDataComponent();
            modelData.setFloats(List.of(customModelData));
            meta.setCustomModelDataComponent(modelData);
        }
    }

    private @NotNull List<Component> renderLore(@NotNull TagResolver placeholders) {
        List<Component> renderedLines = new ArrayList<>();
        for (String line : lore) {
            EmptyPlaceholderDetector detector = new EmptyPlaceholderDetector(placeholders);
            Component rendered = Messages.render(line, detector);
            if (!detector.foundEmptyPlaceholder()) renderedLines.add(withoutItalics(rendered));
        }
        return renderedLines;
    }

    private static final class EmptyPlaceholderDetector implements TagResolver {
        private final TagResolver placeholders;
        private boolean foundEmptyPlaceholder;

        private EmptyPlaceholderDetector(@NotNull TagResolver placeholders) {
            this.placeholders = placeholders;
        }

        @Override
        public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context context) {
            Tag tag = placeholders.resolve(name, arguments, context);
            if (tag instanceof Inserting inserting && PLAIN_TEXT.serialize(inserting.value()).isEmpty()) {
                foundEmptyPlaceholder = true;
            }
            return tag;
        }

        @Override
        public boolean has(@NotNull String name) {
            return placeholders.has(name);
        }

        boolean foundEmptyPlaceholder() {
            return foundEmptyPlaceholder;
        }
    }

    private @NotNull MenuItemTemplate withVariants(@NotNull Map<String, MenuItemTemplate> newVariants) {
        return new MenuItemTemplate(material, amount, name, lore, slots, itemModel, customModelData, glow, priority,
                Map.copyOf(newVariants));
    }

    private static @NotNull Component withoutItalics(@NotNull Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
