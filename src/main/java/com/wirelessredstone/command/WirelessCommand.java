package com.wirelessredstone.command;

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

        if (!args[0].equalsIgnoreCase("bulbs")) {
            player.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
            sendUsage(player);
            return true;
        }

        BulbVariant variant = BulbVariant.COPPER;
        if (args.length >= 2) {
            variant = BulbVariant.fromArg(args[1]);
            if (variant == null) {
                player.sendMessage(Component.text("Unknown variant: " + args[1], NamedTextColor.RED));
                sendUsage(player);
                return true;
            }
        }

        giveWirelessBulbs(player, variant);
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("Usage: /wireless bulbs [variant]", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Variants: --copper, --exposed, --weathered, --oxidized", NamedTextColor.GRAY));
    }

    private void giveWirelessBulbs(Player player, BulbVariant variant) {
        var pairId = bulbManager.createNewPairId();
        var bulbs = WirelessBulbFactory.createLinkedPair(pairId, variant);

        var inventory = player.getInventory();
        for (var bulb : bulbs) {
            if (inventory.firstEmpty() != -1) {
                inventory.addItem(bulb);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), bulb);
            }
        }

        player.sendMessage(Component.text("You received 2 linked " + variant.getDisplayName() + " Bulbs!", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Place them and they will sync their state!", NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if ("bulbs".startsWith(args[0].toLowerCase())) {
                completions.add("bulbs");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("bulbs")) {
            String input = args[1].toLowerCase();
            for (BulbVariant variant : BulbVariant.values()) {
                if (variant.getArg().startsWith(input)) {
                    completions.add(variant.getArg());
                }
            }
        }

        return completions;
    }
}
