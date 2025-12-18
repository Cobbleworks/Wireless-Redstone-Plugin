package com.wirelessredstone.command;

import com.wirelessredstone.gui.BulbManagerGUI;
import com.wirelessredstone.gui.CategorySelectionGUI;
import com.wirelessredstone.item.BulbVariant;
import com.wirelessredstone.item.ChestVariant;
import com.wirelessredstone.item.WirelessBulbFactory;
import com.wirelessredstone.item.WirelessChestFactory;
import com.wirelessredstone.manager.CategoryManager;
import com.wirelessredstone.manager.DebugManager;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.manager.WireViewManager;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.model.ChestGroup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WirelessCommand implements CommandExecutor, TabCompleter {

    private final LinkedBulbManager bulbManager;
    private final LinkedChestManager chestManager;
    private final CategoryManager categoryManager;
    private final WireViewManager wireViewManager;
    private final DebugManager debugManager;

    public WirelessCommand(LinkedBulbManager bulbManager, LinkedChestManager chestManager, CategoryManager categoryManager, 
                           WireViewManager wireViewManager, DebugManager debugManager) {
        this.bulbManager = bulbManager;
        this.chestManager = chestManager;
        this.categoryManager = categoryManager;
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
            case "append", "extend" -> handleAppendCommand(player, args);
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

    private void handleAppendCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /wireless append <groupname> [count]", NamedTextColor.RED));
            player.sendMessage(Component.text("Example: /wireless append MyLamps 3", NamedTextColor.GRAY));
            return;
        }

        String groupName = args[1];
        int extraCount = 1;
        
        if (args.length >= 3) {
            try {
                extraCount = Integer.parseInt(args[2]);
                if (extraCount < 1 || extraCount > 24) {
                    player.sendMessage(Component.text("Extra count must be between 1 and 24!", NamedTextColor.RED));
                    return;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid number: " + args[2], NamedTextColor.RED));
                return;
            }
        }

        // Search for bulb group first
        Optional<BulbGroup> bulbGroupOpt = findBulbGroupByName(player, groupName);
        Optional<ChestGroup> chestGroupOpt = findChestGroupByName(player, groupName);

        if (bulbGroupOpt.isPresent()) {
            extendBulbGroup(player, bulbGroupOpt.get(), extraCount);
        } else if (chestGroupOpt.isPresent()) {
            extendChestGroup(player, chestGroupOpt.get(), extraCount);
        } else {
            player.sendMessage(Component.text("No group found with name: " + groupName, NamedTextColor.RED));
            player.sendMessage(Component.text("Use /wireless gui to see your groups.", NamedTextColor.GRAY));
        }
    }

    private Optional<BulbGroup> findBulbGroupByName(Player player, String name) {
        UUID playerUuid = player.getUniqueId();
        boolean isAdmin = player.hasPermission("wirelessredstone.admin");
        
        return bulbManager.getAllGroups().stream()
                .filter(g -> isAdmin || playerUuid.equals(g.getOwnerUuid()))
                .filter(g -> matchesGroupName(g.getCustomName(), g.getGroupId(), name))
                .findFirst();
    }

    private Optional<ChestGroup> findChestGroupByName(Player player, String name) {
        if (chestManager == null) return Optional.empty();
        
        UUID playerUuid = player.getUniqueId();
        boolean isAdmin = player.hasPermission("wirelessredstone.admin");
        
        return chestManager.getAllGroups().stream()
                .filter(g -> isAdmin || playerUuid.equals(g.getOwnerUuid()))
                .filter(g -> matchesGroupName(g.getCustomName(), g.getGroupId(), name))
                .findFirst();
    }

    private boolean matchesGroupName(String customName, UUID groupId, String searchName) {
        // Match by custom name (case-insensitive)
        if (customName != null && customName.equalsIgnoreCase(searchName)) {
            return true;
        }
        // Match by partial group ID
        String shortId = groupId.toString().substring(0, 8);
        return shortId.equalsIgnoreCase(searchName) || groupId.toString().startsWith(searchName.toLowerCase());
    }

    private void extendBulbGroup(Player player, BulbGroup group, int extraCount) {
        int currentSize = group.getMaxSize();
        int newSize = currentSize + extraCount;
        
        if (newSize > 26) {
            player.sendMessage(Component.text("Cannot extend beyond 26 blocks! Current size: " + currentSize, NamedTextColor.RED));
            return;
        }

        // Extend the group's internal storage
        group.extendGroup(extraCount);
        
        // Create the new bulb items starting from the current max index
        BulbVariant variant = BulbVariant.fromBulbType(group.getBulbType());
        var newBulbs = WirelessBulbFactory.createExtensionBulbs(
                group.getGroupId(), variant, group.getOwnerUuid(), currentSize, extraCount, newSize);
        
        var inventory = player.getInventory();
        for (var bulb : newBulbs) {
            if (inventory.firstEmpty() != -1) {
                inventory.addItem(bulb);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), bulb);
            }
        }

        bulbManager.saveData();
        
        player.sendMessage(Component.text("Extended group ", NamedTextColor.GREEN)
                .append(Component.text(group.getDisplayName(), NamedTextColor.AQUA))
                .append(Component.text(" with " + extraCount + " new block(s)! New size: " + newSize, NamedTextColor.GREEN)));
    }

    private void extendChestGroup(Player player, ChestGroup group, int extraCount) {
        int currentSize = group.getMaxSize();
        int newSize = currentSize + extraCount;
        
        if (newSize > 26) {
            player.sendMessage(Component.text("Cannot extend beyond 26 blocks! Current size: " + currentSize, NamedTextColor.RED));
            return;
        }

        // Extend the group's internal storage
        group.extendGroup(extraCount);
        
        // Create the new chest items
        ChestVariant variant = ChestVariant.fromContainerType(group.getContainerType());
        var newChests = WirelessChestFactory.createExtensionContainers(
                group.getGroupId(), variant, group.getOwnerUuid(), currentSize, extraCount, newSize);
        
        var inventory = player.getInventory();
        for (var chest : newChests) {
            if (inventory.firstEmpty() != -1) {
                inventory.addItem(chest);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), chest);
            }
        }

        chestManager.saveData();
        
        player.sendMessage(Component.text("Extended group ", NamedTextColor.GREEN)
                .append(Component.text(group.getDisplayName(), NamedTextColor.GOLD))
                .append(Component.text(" with " + extraCount + " new container(s)! New size: " + newSize, NamedTextColor.GREEN)));
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
        boolean skipCategories = args.length >= 2 && args[1].equalsIgnoreCase("--nocategory");
        
        if (skipCategories) {
            // Open the old-style GUI without categories (useful if user doesn't want to use categories)
            new BulbManagerGUI(bulbManager, chestManager, player, showAll).open();
        } else {
            // Open the category selection GUI first
            new CategorySelectionGUI(categoryManager, bulbManager, chestManager, player, showAll).open();
        }
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
        player.sendMessage(Component.text("  Copper: --copper, --copper-exposed, --copper-weathered, --copper-oxidized", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("/wireless append <name> [count]", NamedTextColor.YELLOW)
                .append(Component.text(" - Add more blocks to an existing group", NamedTextColor.GRAY)));
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
            for (String sub : List.of("bulbs", "lamps", "chests", "append", "extend", "gui", "manage", "list", "wireview", "debug")) {
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
                if ("--nocategory".startsWith(input)) {
                    completions.add("--nocategory");
                }
            } else if (subCommand.equals("append") || subCommand.equals("extend")) {
                if (args.length == 2 && sender instanceof Player player) {
                    // Suggest group names
                    UUID playerUuid = player.getUniqueId();
                    boolean isAdmin = player.hasPermission("wirelessredstone.admin");
                    
                    bulbManager.getAllGroups().stream()
                            .filter(g -> isAdmin || playerUuid.equals(g.getOwnerUuid()))
                            .forEach(g -> {
                                String name = g.getCustomName() != null ? g.getCustomName() : g.getGroupId().toString().substring(0, 8);
                                if (name.toLowerCase().startsWith(input)) {
                                    completions.add(name.contains(" ") ? "\"" + name + "\"" : name);
                                }
                            });
                    
                    if (chestManager != null) {
                        chestManager.getAllGroups().stream()
                                .filter(g -> isAdmin || playerUuid.equals(g.getOwnerUuid()))
                                .forEach(g -> {
                                    String name = g.getCustomName() != null ? g.getCustomName() : g.getGroupId().toString().substring(0, 8);
                                    if (name.toLowerCase().startsWith(input)) {
                                        completions.add(name.contains(" ") ? "\"" + name + "\"" : name);
                                    }
                                });
                    }
                } else if (args.length == 3) {
                    // Suggest counts
                    for (int i = 1; i <= 5; i++) {
                        String num = String.valueOf(i);
                        if (num.startsWith(input)) {
                            completions.add(num);
                        }
                    }
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
