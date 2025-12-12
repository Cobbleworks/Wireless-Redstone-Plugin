package com.wirelessredstone.command;

import com.wirelessredstone.gui.BulbManagerGUI;
import com.wirelessredstone.item.BulbVariant;
import com.wirelessredstone.item.ChestVariant;
import com.wirelessredstone.item.WirelessBulbFactory;
import com.wirelessredstone.item.WirelessChestFactory;
import com.wirelessredstone.manager.DebugManager;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.manager.WireViewManager;
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
    private final LinkedChestManager chestManager;
    private final WireViewManager wireViewManager;
    private final DebugManager debugManager;

    public WirelessCommand(LinkedBulbManager bulbManager, LinkedChestManager chestManager, WireViewManager wireViewManager, DebugManager debugManager) {
        this.bulbManager = bulbManager;
        this.chestManager = chestManager;
        this.wireViewManager = wireViewManager;
        this.debugManager = debugManager;
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
            case "lamps" -> handleLampsCommand(player, args);
            case "chests" -> handleChestsCommand(player, args);
            case "gui", "manage", "list" -> handleGUICommand(player, args);
            case "wireview" -> handleWireViewCommand(player);
            case "debug" -> handleDebugCommand(player, args);
            default -> {
                player.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
                sendUsage(player);
            }
        }
        
        return true;
    }

    private void handleBulbsCommand(Player player, String[] args) {
        int count = 2;
        BulbVariant variant = BulbVariant.COPPER;
        
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                BulbVariant parsed = BulbVariant.fromArg(arg);
                if (parsed != null && parsed != BulbVariant.REDSTONE_LAMP) {
                    variant = parsed;
                } else {
                    player.sendMessage(Component.text("Unknown variant: " + arg, NamedTextColor.RED));
                    player.sendMessage(Component.text("Use /wireless lamps for redstone lamps.", NamedTextColor.GRAY));
                    return;
                }
            } else {
                try {
                    count = Integer.parseInt(arg);
                    if (count < 2 || count > 26) {
                        player.sendMessage(Component.text("Bulb count must be between 2 and 26!", NamedTextColor.RED));
                        return;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Invalid number: " + arg, NamedTextColor.RED));
                    return;
                }
            }
        }

        giveWirelessBulbs(player, variant, count);
    }

    private void handleLampsCommand(Player player, String[] args) {
        int count = 2;
        
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            try {
                count = Integer.parseInt(arg);
                if (count < 2 || count > 26) {
                    player.sendMessage(Component.text("Lamp count must be between 2 and 26!", NamedTextColor.RED));
                    return;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid number: " + arg, NamedTextColor.RED));
                return;
            }
        }

        giveWirelessBulbs(player, BulbVariant.REDSTONE_LAMP, count);
    }

    private void handleChestsCommand(Player player, String[] args) {
        int count = 2;
        ChestVariant variant = ChestVariant.CHEST;
        
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                ChestVariant parsed = ChestVariant.fromArg(arg);
                if (parsed != null) {
                    variant = parsed;
                } else {
                    player.sendMessage(Component.text("Unknown variant: " + arg, NamedTextColor.RED));
                    player.sendMessage(Component.text("Available: --chest, --shulker, --white, --orange, --magenta, etc.", NamedTextColor.GRAY));
                    return;
                }
            } else {
                try {
                    count = Integer.parseInt(arg);
                    if (count < 2 || count > 26) {
                        player.sendMessage(Component.text("Container count must be between 2 and 26!", NamedTextColor.RED));
                        return;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Invalid number: " + arg, NamedTextColor.RED));
                    return;
                }
            }
        }

        giveWirelessContainers(player, variant, count);
    }

    private void handleDebugCommand(Player player, String[] args) {
        if (args.length < 2) {
            boolean current = debugManager.isDebugEnabled(player);
            player.sendMessage(Component.text("Debug mode is currently: ", NamedTextColor.GRAY)
                    .append(Component.text(current ? "ON" : "OFF", current ? NamedTextColor.GREEN : NamedTextColor.RED)));
            player.sendMessage(Component.text("Use /wireless debug on|off to toggle.", NamedTextColor.GRAY));
            return;
        }

        String toggle = args[1].toLowerCase();
        boolean enabled;
        if (toggle.equals("on") || toggle.equals("true") || toggle.equals("enable")) {
            enabled = true;
        } else if (toggle.equals("off") || toggle.equals("false") || toggle.equals("disable")) {
            enabled = false;
        } else {
            player.sendMessage(Component.text("Usage: /wireless debug on|off", NamedTextColor.RED));
            return;
        }

        debugManager.setDebugEnabled(player, enabled);
        if (enabled) {
            player.sendMessage(Component.text("Debug mode enabled! ", NamedTextColor.GREEN)
                    .append(Component.text("You will see sync messages for blocks within 3 blocks of you.", NamedTextColor.GRAY)));
        } else {
            player.sendMessage(Component.text("Debug mode disabled.", NamedTextColor.YELLOW));
        }
    }

    private void handleGUICommand(Player player, String[] args) {
        boolean showAll = args.length >= 2 && args[1].equalsIgnoreCase("--all");
        new BulbManagerGUI(bulbManager, chestManager, player, showAll).open();
    }

    private void handleWireViewCommand(Player player) {
        boolean enabled = wireViewManager.toggleWireView(player);
        if (enabled) {
            player.sendMessage(Component.text("WireView enabled! ", NamedTextColor.GREEN)
                    .append(Component.text("Paired bulbs are now highlighted with glowing outlines.", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("Bulbs in the same pair share the same color.", NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("WireView disabled.", NamedTextColor.YELLOW));
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("=== Wireless Redstone Commands ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/wireless bulbs [count] [variant]", NamedTextColor.YELLOW)
                .append(Component.text(" - Get linked copper bulbs", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  Count: 2-26 (default: 2)", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("  Variants: --copper, --exposed, --weathered, --oxidized", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("/wireless lamps [count]", NamedTextColor.YELLOW)
                .append(Component.text(" - Get linked redstone lamps", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/wireless chests [count] [variant]", NamedTextColor.YELLOW)
                .append(Component.text(" - Get linked wireless containers", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  Variants: --chest, --shulker, --white, --orange, etc.", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("/wireless gui [--all]", NamedTextColor.YELLOW)
                .append(Component.text(" - Open management GUI", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/wireless wireview", NamedTextColor.YELLOW)
                .append(Component.text(" - Toggle glowing outline on paired bulbs", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/wireless debug on|off", NamedTextColor.YELLOW)
                .append(Component.text(" - Toggle sync debug messages for nearby blocks", NamedTextColor.GRAY)));
    }

    private void giveWirelessBulbs(Player player, BulbVariant variant, int count) {
        var groupId = bulbManager.createNewGroupId();
        var bulbs = WirelessBulbFactory.createLinkedGroup(groupId, variant, player.getUniqueId(), count);

        var inventory = player.getInventory();
        for (var bulb : bulbs) {
            if (inventory.firstEmpty() != -1) {
                inventory.addItem(bulb);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), bulb);
            }
        }

        player.sendMessage(Component.text("You received " + count + " linked " + variant.getDisplayName() + "s!", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Place them and they will sync their state!", NamedTextColor.GRAY));
    }

    private void giveWirelessContainers(Player player, ChestVariant variant, int count) {
        var groupId = chestManager.createNewGroupId();
        var containers = WirelessChestFactory.createLinkedContainers(groupId, variant, player.getUniqueId(), count);

        var inventory = player.getInventory();
        for (var container : containers) {
            if (inventory.firstEmpty() != -1) {
                inventory.addItem(container);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), container);
            }
        }

        player.sendMessage(Component.text("You received " + count + " linked " + variant.getDisplayName() + "s!", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Place them and they will sync their contents!", NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (String sub : List.of("bulbs", "lamps", "chests", "gui", "manage", "list", "wireview", "debug")) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length >= 2) {
            String subCommand = args[0].toLowerCase();
            String input = args[args.length - 1].toLowerCase();
            
            if (subCommand.equals("bulbs")) {
                for (BulbVariant variant : BulbVariant.values()) {
                    if (variant != BulbVariant.REDSTONE_LAMP && variant.getArg().startsWith(input)) {
                        completions.add(variant.getArg());
                    }
                }
                for (int i = 2; i <= 10; i++) {
                    String num = String.valueOf(i);
                    if (num.startsWith(input)) {
                        completions.add(num);
                    }
                }
            } else if (subCommand.equals("lamps")) {
                for (int i = 2; i <= 10; i++) {
                    String num = String.valueOf(i);
                    if (num.startsWith(input)) {
                        completions.add(num);
                    }
                }
            } else if (subCommand.equals("chests")) {
                for (ChestVariant variant : ChestVariant.values()) {
                    if (variant.getArg().startsWith(input)) {
                        completions.add(variant.getArg());
                    }
                }
                for (int i = 2; i <= 10; i++) {
                    String num = String.valueOf(i);
                    if (num.startsWith(input)) {
                        completions.add(num);
                    }
                }
            } else if (subCommand.equals("gui") || subCommand.equals("manage") || subCommand.equals("list")) {
                if ("--all".startsWith(input) && sender.hasPermission("wirelessredstone.admin")) {
                    completions.add("--all");
                }
            } else if (subCommand.equals("debug")) {
                for (String opt : List.of("on", "off")) {
                    if (opt.startsWith(input)) {
                        completions.add(opt);
                    }
                }
            }
        }

        return completions;
    }
}
