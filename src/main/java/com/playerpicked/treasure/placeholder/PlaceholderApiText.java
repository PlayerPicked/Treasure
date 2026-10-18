package com.playerpicked.treasure.placeholder;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderApiText implements ExternalPlaceholders {
    private static final Pattern PLACEHOLDER = Pattern.compile("%[^%\\s]+%");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    // Values are inserted as components, so a PlaceholderAPI result can never inject MiniMessage tags
    @Override
    public @NotNull PreparedText prepare(@NotNull Player player, @NotNull String miniMessage) {
        Matcher matcher = PLACEHOLDER.matcher(miniMessage);
        if (!matcher.find()) return new PreparedText(miniMessage, TagResolver.empty());

        StringBuilder text = new StringBuilder();
        List<TagResolver> values = new ArrayList<>();
        do {
            String tagName = "papi-" + values.size();
            String value = PlaceholderAPI.setPlaceholders(player, matcher.group());
            values.add(Placeholder.component(tagName, LEGACY.deserialize(value)));
            matcher.appendReplacement(text, Matcher.quoteReplacement("<" + tagName + ">"));
        } while (matcher.find());
        matcher.appendTail(text);

        return new PreparedText(text.toString(), TagResolver.resolver(values));
    }
}
