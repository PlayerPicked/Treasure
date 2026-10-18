package com.playerpicked.treasure;

import com.playerpicked.treasure.command.CommandRegistrar;
import com.playerpicked.treasure.command.MainCommand;
import com.playerpicked.treasure.config.PluginConfig;
import com.playerpicked.treasure.config.YamlFile;
import com.playerpicked.treasure.cooldown.CooldownLoader;
import com.playerpicked.treasure.cooldown.CooldownService;
import com.playerpicked.treasure.hologram.HologramRegistry;
import com.playerpicked.treasure.hologram.HologramText;
import com.playerpicked.treasure.loot.LootInventories;
import com.playerpicked.treasure.loot.LootInventoryListener;
import com.playerpicked.treasure.menu.MenuContext;
import com.playerpicked.treasure.menu.MenuListener;
import com.playerpicked.treasure.menu.MenuNavigator;
import com.playerpicked.treasure.notification.SoundPlayer;
import com.playerpicked.treasure.notification.ToastSender;
import com.playerpicked.treasure.particle.ParticleEffects;
import com.playerpicked.treasure.placeholder.ExternalPlaceholders;
import com.playerpicked.treasure.placeholder.PlaceholderApiText;
import com.playerpicked.treasure.placeholder.TreasureExpansion;
import com.playerpicked.treasure.scheduler.AsyncExecutor;
import com.playerpicked.treasure.scheduler.ServerScheduler;
import com.playerpicked.treasure.setup.ChatPrompts;
import com.playerpicked.treasure.setup.LocationPicker;
import com.playerpicked.treasure.storage.CooldownRepository;
import com.playerpicked.treasure.storage.Database;
import com.playerpicked.treasure.text.Messages;
import com.playerpicked.treasure.tracker.TrackerService;
import com.playerpicked.treasure.treasure.TreasureBlockListener;
import com.playerpicked.treasure.treasure.TreasureService;
import com.playerpicked.treasure.treasure.TreasureStore;
import com.playerpicked.treasure.treasure.TreasureVisuals;
import com.playerpicked.treasure.update.UpdateChecker;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import revxrsal.zapper.ZapperJavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class Treasure extends ZapperJavaPlugin {
    private static final int SPIGOT_RESOURCE_ID = 125956;

    private final List<AutoCloseable> resources = new ArrayList<>();

    private PluginConfig config;
    private Messages messages;
    private YamlFile guisFile;
    private MenuNavigator menus;
    private TreasureService treasures;

    @Override
    public void onEnable() {
        Logger logger = getSLF4JLogger();

        try {
            enable(logger);
            logger.info("Treasure {} enabled", getPluginMeta().getVersion());
        } catch (RuntimeException exception) {
            logger.error("Treasure could not start and will be disabled", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        for (AutoCloseable resource : resources.reversed()) {
            try {
                resource.close();
            } catch (Exception exception) {
                getSLF4JLogger().warn("Failed to close {} cleanly", resource.getClass().getSimpleName(), exception);
            }
        }
        resources.clear();
    }

    private void enable(@NotNull Logger logger) {
        config = new PluginConfig(YamlFile.loadFromPluginFolder(this, "config.yml"));
        messages = new Messages(YamlFile.loadFromPluginFolder(this, "messages.yml"));
        guisFile = YamlFile.loadFromPluginFolder(this, "guis.yml");

        AsyncExecutor async = new AsyncExecutor();
        ServerScheduler scheduler = new ServerScheduler(this);

        Database database = track(Database.connect(config.database(), getDataFolder().toPath()));
        runOnShutdown(async::shutdown);

        CooldownService cooldowns = new CooldownService(new CooldownRepository(database.sql()), async, logger);
        ExternalPlaceholders externalPlaceholders = isPlaceholderApiEnabled() ? new PlaceholderApiText() : ExternalPlaceholders.NONE;

        HologramText hologramText = new HologramText(messages, cooldowns, externalPlaceholders);
        TreasureVisuals visuals = new TreasureVisuals(new HologramRegistry(this, scheduler), new ParticleEffects(scheduler), hologramText, config);
        runOnShutdown(visuals::shutdown);

        treasures = new TreasureService(new TreasureStore(new File(getDataFolder(), "treasures.yml"), logger), visuals, cooldowns, async, logger);
        treasures.loadAll();

        ToastSender toasts = new ToastSender(this, scheduler, logger);
        runOnShutdown(toasts::shutdown);
        SoundPlayer sounds = new SoundPlayer(config, logger);

        ChatPrompts prompts = new ChatPrompts(config, messages, scheduler);
        LocationPicker locations = new LocationPicker(config, messages, scheduler);
        LootInventories loot = new LootInventories(treasures, config, messages, sounds, scheduler);
        menus = new MenuNavigator(new MenuContext(guisFile, messages, scheduler, logger, treasures, loot, prompts, locations, sounds));

        TrackerService tracker = new TrackerService(this, treasures, config, messages, scheduler);
        tracker.start();

        registerListeners(
                new MenuListener(),
                new LootInventoryListener(),
                new TreasureBlockListener(treasures, loot, config, messages, sounds, toasts),
                new CooldownLoader(cooldowns, logger),
                prompts,
                locations
        );
        CommandRegistrar.register(this, config.commandAliases(), messages, new MainCommand(menus, tracker, messages, scheduler, this::reloadFiles));

        loadCooldownsOfOnlinePlayers(cooldowns, logger);
        registerIntegrations(cooldowns, scheduler, logger);
    }

    private void registerIntegrations(@NotNull CooldownService cooldowns, @NotNull ServerScheduler scheduler, @NotNull Logger logger) {
        String version = getPluginMeta().getVersion();

        if (isPlaceholderApiEnabled()) {
            TreasureExpansion expansion = new TreasureExpansion(version, treasures, cooldowns, messages);
            expansion.register();
            runOnShutdown(expansion::unregister);
        }

        UpdateChecker updates = new UpdateChecker(SPIGOT_RESOURCE_ID, version, messages, scheduler, logger);
        registerListeners(updates);
        updates.start();
        runOnShutdown(updates::shutdown);
    }

    private void loadCooldownsOfOnlinePlayers(@NotNull CooldownService cooldowns, @NotNull Logger logger) {
        for (Player player : getServer().getOnlinePlayers()) {
            cooldowns.load(player.getUniqueId()).exceptionally(error -> {
                logger.error("Could not load the treasure cooldowns of {}", player.getName(), error);
                return null;
            });
        }
    }

    private void reloadFiles() {
        menus.closeAll();
        config.reload();
        messages.reload();
        guisFile.reload();
        treasures.showAll();
    }

    private boolean isPlaceholderApiEnabled() {
        return getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    private void registerListeners(@NotNull Listener... listeners) {
        for (Listener listener : listeners) getServer().getPluginManager().registerEvents(listener, this);
    }

    private void runOnShutdown(@NotNull Runnable shutdownAction) {
        resources.add(shutdownAction::run);
    }

    private <T extends AutoCloseable> @NotNull T track(@NotNull T resource) {
        resources.add(resource);
        return resource;
    }
}
