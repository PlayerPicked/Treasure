package com.playerpicked.treasure.command;

import com.playerpicked.treasure.menu.MenuNavigator;
import com.playerpicked.treasure.scheduler.ServerScheduler;
import com.playerpicked.treasure.text.MessageKey;
import com.playerpicked.treasure.text.Messages;
import com.playerpicked.treasure.tracker.TrackerService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.CommandPlaceholder;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.orphan.OrphanCommand;

public final class MainCommand implements OrphanCommand {
    private final MenuNavigator menus;
    private final TrackerService tracker;
    private final Messages messages;
    private final ServerScheduler scheduler;
    private final Runnable reloadPlugin;

    public MainCommand(@NotNull MenuNavigator menus, @NotNull TrackerService tracker, @NotNull Messages messages,
                       @NotNull ServerScheduler scheduler, @NotNull Runnable reloadPlugin) {
        this.menus = menus;
        this.tracker = tracker;
        this.messages = messages;
        this.scheduler = scheduler;
        this.reloadPlugin = reloadPlugin;
    }

    @CommandPlaceholder
    @CommandPermission("treasure.setup")
    public void openOverview(@NotNull Player player) {
        menus.openOverview(player);
    }

    @Subcommand("setup")
    @CommandPermission("treasure.setup")
    public void setup(@NotNull Player player) {
        menus.openOverview(player);
    }

    @Subcommand("give tracker")
    @CommandPermission("treasure.tracker")
    public void giveTracker(@NotNull CommandSender sender, @NotNull Player target) {
        scheduler.runForPlayer(target, () -> {
            target.getInventory().addItem(tracker.createTracker()).values()
                    .forEach(overflow -> target.getWorld().dropItemNaturally(target.getLocation(), overflow));
            messages.send(target, MessageKey.TRACKER_RECEIVED);
        });
        if (sender != target) messages.send(sender, MessageKey.TRACKER_GIVEN, Placeholder.unparsed("player", target.getName()));
    }

    @Subcommand("reload")
    @CommandPermission("treasure.reload")
    public void reload(@NotNull CommandSender sender) {
        reloadPlugin.run();
        messages.send(sender, MessageKey.RELOADED);
    }
}
