package com.wirelessredstone.command;

import com.wirelessredstone.gui.BulbManagerGUI;
import com.wirelessredstone.item.BulbVariant;
import com.wirelessredstone.item.WirelessBulbFactory;
import com.wirelessredstone.manager.LinkedBulbManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class WirelessCommand implements CommandExecutor, TabCompleter {

    private final LinkedBulbManager bulbManager;

    public WirelessCommand(LinkedBulbManager bulbManager) {
        this.bulbManager = bulbManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("wirelessredstone.use")) {
            player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sendUsage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "bulbs" -> handleBulbsCommand(player, args);
            case "lamps" -> handleLampsCommand(player);
            case "gui", "manage", "list" -> handleGUICommand(player, args);
            default -> {
                player.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
                sendUsage(player);
            }
        }
        
        return true;
    }

    private void handleBulbsCommand(Player player, String[] args) {
        BulbVariant variant = BulbVariant.COPPER;
        if (args.length >= 2) {
            variant = BulbVariant.fromArg(args[1]);
            if (variant == null || variant == BulbVariant.REDSTONE_LAMP) {
                player.sendMessage(Component.text("Unknown variant: " + args[1], NamedTextColor.RED));
                player.sendMessage(Component.text("Use /wireless lamps for redstone lamps.", NamedTextColor.GRAY));
                sendUsage(player);
                return;
            }
        }

        giveWirelessBulbs(player, variant);
    }

    private void handleLampsCommand(Player player) {
        giveWirelessBulbs(player, BulbVariant.REDSTONE_LAMP);
    }

    private void handleGUICommand(Player player, String[] args) {
        boolean showAll = args.length >= 2 && args[1].equalsIgnoreCase("--all");
        new BulbManagerGUI(bulbManager, player, showAll).open();
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("=== Wireless Redstone Commands ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/wireless bulbs [variant]", NamedTextColor.YELLOW)
                .append(Component.text(" - Get linked copper bulbs", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  Variants: --copper, --exposed, --weathered, --oxidized", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("/wireless lamps", NamedTextColor.YELLOW)
                .append(Component.text(" - Get linked redstone lamps", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/wireless gui [--all]", NamedTextColor.YELLOW)
                .append(Component.text(" - Open management GUI", NamedTextColor.GRAY)));
    }

    private void giveWirelessBulbs(Player player, BulbVariant variant) {
        var pairId = bulbManager.createNewPairId();
        var bulbs = WirelessBulbFactory.createLinkedPair(pairId, variant, player.getUniqueId());

        var inventory = player.getInventory();
        for (var bulb : bulbs) {
            if (inventory.firstEmpty() != -1) {
                inventory.addItem(bulb);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), bulb);
            }
        }

        player.sendMessage(Component.text("You received 2 linked " + variant.getDisplayName() + "s!", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Place them and they will sync their state!", NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (String sub : List.of("bulbs", "lamps", "gui", "manage", "list")) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String input = args[1].toLowerCase();
            
            if (subCommand.equals("bulbs")) {
                for (BulbVariant variant : BulbVariant.values()) {
                    if (variant != BulbVariant.REDSTONE_LAMP && variant.getArg().startsWith(input)) {
                        completions.add(variant.getArg());
                    }
                }
            } else if (subCommand.equals("gui") || subCommand.equals("manage") || subCommand.equals("list")) {
                if ("--all".startsWith(input) && sender.hasPermission("wirelessredstone.admin")) {
                    completions.add("--all");
                }
            }
        }

        return completions;
    }
}
