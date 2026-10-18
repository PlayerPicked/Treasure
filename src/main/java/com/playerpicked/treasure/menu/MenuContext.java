package com.playerpicked.treasure.menu;

import com.playerpicked.treasure.config.YamlFile;
import com.playerpicked.treasure.loot.LootInventories;
import com.playerpicked.treasure.notification.SoundPlayer;
import com.playerpicked.treasure.scheduler.ServerScheduler;
import com.playerpicked.treasure.setup.ChatPrompts;
import com.playerpicked.treasure.setup.LocationPicker;
import com.playerpicked.treasure.text.Messages;
import com.playerpicked.treasure.treasure.TreasureService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public record MenuContext(
        @NotNull YamlFile guis,
        @NotNull Messages messages,
        @NotNull ServerScheduler scheduler,
        @NotNull Logger logger,
        @NotNull TreasureService treasures,
        @NotNull LootInventories loot,
        @NotNull ChatPrompts prompts,
        @NotNull LocationPicker locations,
        @NotNull SoundPlayer sounds
) {
}
