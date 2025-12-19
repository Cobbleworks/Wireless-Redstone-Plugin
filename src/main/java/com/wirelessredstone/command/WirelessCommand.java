package com.wirelessredstone.command;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.gui.BulbManagerGUI;
import com.wirelessredstone.gui.CategorySelectionGUI;
import com.wirelessredstone.item.BulbVariant;
import com.wirelessredstone.item.ChestVariant;
import com.wirelessredstone.item.CircuitAnalyserFactory;
import com.wirelessredstone.item.WirelessBulbFactory;
import com.wirelessredstone.item.WirelessChestFactory;
import com.wirelessredstone.manager.CategoryManager;
import com.wirelessredstone.manager.DebugManager;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.model.BaseGroup;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.model.Category;
import com.wirelessredstone.model.ChestGroup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WirelessCommand implements CommandExecutor, TabCompleter {

    private final WirelessRedstonePlugin plugin;
    private final LinkedBulbManager bulbManager;
    private final LinkedChestManager chestManager;
    private final CategoryManager categoryManager;
    private final DebugManager debugManager;

    public WirelessCommand(WirelessRedstonePlugin plugin, LinkedBulbManager bulbManager, LinkedChestManager chestManager, 
                           CategoryManager categoryManager, DebugManager debugManager) {
        this.plugin = plugin;
        this.bulbManager = bulbManager;
        this.chestManager = chestManager;
        this.categoryManager = categoryManager;
        this.debugManager = debugManager;
    }

    /**
     * Parses command arguments, handling quoted strings with spaces.
     * Handles both standalone quoted strings and option=value pairs where the value is quoted.
     * Examples:
     * - ["\"My", "Name\""] becomes ["My Name"]
     * - ["--name=\"My", "Name\""] becomes ["--name=My Name"]
     * - ["--category=\"Buwsi", "Lichter", "Reaktor\""] becomes ["--category=Buwsi Lichter Reaktor"]
     */
    private String[] parseQuotedArgs(String[] args) {
        List<String> parsed = new ArrayList<>();
        StringBuilder current = null;
        String prefix = null; // For handling --option="value with spaces"
        
        for (String arg : args) {
            if (current != null) {
                // We're inside a quoted string
                if (arg.endsWith("\"")) {
                    current.append(" ").append(arg.substring(0, arg.length() - 1));
                    if (prefix != null) {
                        parsed.add(prefix + current.toString());
                        prefix = null;
                    } else {
                        parsed.add(current.toString());
                    }
                    current = null;
                } else {
                    current.append(" ").append(arg);
                }
            } else {
                // Check for --option="value with spaces"
                int equalsIdx = arg.indexOf('=');
                if (equalsIdx > 0 && arg.length() > equalsIdx + 1) {
                    String optionPart = arg.substring(0, equalsIdx + 1);
                    String valuePart = arg.substring(equalsIdx + 1);
                    
                    if (valuePart.startsWith("\"")) {
                        if (valuePart.endsWith("\"") && valuePart.length() > 1) {
                            // Complete quoted value like --name="value"
                            parsed.add(optionPart + valuePart.substring(1, valuePart.length() - 1));
                        } else {
                            // Start of multi-word quoted value like --name="My
                            prefix = optionPart;
                            current = new StringBuilder(valuePart.substring(1));
                        }
                    } else {
                        parsed.add(arg);
                    }
                } else if (arg.startsWith("\"")) {
                    // Start of a standalone quoted string
                    if (arg.endsWith("\"") && arg.length() > 1) {
                        // Single word in quotes like "word"
                        parsed.add(arg.substring(1, arg.length() - 1));
                    } else {
                        current = new StringBuilder(arg.substring(1));
                    }
                } else {
                    parsed.add(arg);
                }
            }
        }
        
        // If we have an unclosed quote, add what we have
        if (current != null) {
            if (prefix != null) {
                parsed.add(prefix + current.toString());
            } else {
                parsed.add(current.toString());
            }
        }
        
        return parsed.toArray(new String[0]);
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

        // Parse arguments to handle quoted strings with spaces
        String[] parsedArgs = parseQuotedArgs(args);
        String subCommand = parsedArgs[0].toLowerCase();
        
        switch (subCommand) {
            case "bulbs" -> handleBulbsCommand(player, parsedArgs);
            case "lamps" -> handleLampsCommand(player, parsedArgs);
            case "chests" -> handleChestsCommand(player, parsedArgs);
            case "append", "extend" -> handleAppendCommand(player, parsedArgs);
            case "recover", "reclaim" -> handleRecoverCommand(player, parsedArgs);
            case "inspect" -> handleInspectCommand(player, parsedArgs);
            case "gui", "manage", "list" -> handleGUICommand(player, parsedArgs);
            case "debug" -> handleDebugCommand(player, parsedArgs);
            case "reload" -> handleReloadCommand(player);
            case "setname", "rename" -> handleSetNameCommand(player, parsedArgs);
            case "setcategory" -> handleSetCategoryCommand(player, parsedArgs);
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
        String groupName = null;
        String categoryName = null;
        
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--name=")) {
                groupName = arg.substring(7);
            } else if (arg.startsWith("--category=")) {
                categoryName = arg.substring(11);
            } else if (arg.startsWith("--")) {
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

        UUID categoryId = resolveCategoryId(player, categoryName);
        giveWirelessBulbs(player, variant, count, groupName, categoryId);
    }

    private void handleLampsCommand(Player player, String[] args) {
        int count = 2;
        String groupName = null;
        String categoryName = null;
        
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--name=")) {
                groupName = arg.substring(7);
            } else if (arg.startsWith("--category=")) {
                categoryName = arg.substring(11);
            } else {
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
        }

        UUID categoryId = resolveCategoryId(player, categoryName);
        giveWirelessBulbs(player, BulbVariant.REDSTONE_LAMP, count, groupName, categoryId);
    }

    private void handleChestsCommand(Player player, String[] args) {
        int count = 2;
        ChestVariant variant = ChestVariant.CHEST;
        String groupName = null;
        String categoryName = null;
        
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--name=")) {
                groupName = arg.substring(7);
            } else if (arg.startsWith("--category=")) {
                categoryName = arg.substring(11);
            } else if (arg.startsWith("--")) {
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

        UUID categoryId = resolveCategoryId(player, categoryName);
        giveWirelessContainers(player, variant, count, groupName, categoryId);
    }

    /**
     * Resolves a category name to its UUID, or returns null if not found or not specified.
     */
    private UUID resolveCategoryId(Player player, String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) {
            return null;
        }
        return findCategoryByName(player, categoryName)
                .map(Category::getCategoryId)
                .orElse(null);
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

        group.extendGroup(extraCount);
        
        BulbVariant variant = BulbVariant.fromBulbType(group.getBulbType());
        var newBulbs = WirelessBulbFactory.createExtensionBulbs(
                group.getGroupId(), variant, group.getOwnerUuid(), currentSize, extraCount, newSize);
        
        distributeItems(player, newBulbs);
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

        group.extendGroup(extraCount);
        
        ChestVariant variant = ChestVariant.fromContainerType(group.getContainerType());
        var newChests = WirelessChestFactory.createExtensionContainers(
                group.getGroupId(), variant, group.getOwnerUuid(), currentSize, extraCount, newSize);
        
        distributeItems(player, newChests);
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

    private void handleInspectCommand(Player player, String[] args) {
        Player target = player;
        
        if (args.length >= 2) {
            if (!player.hasPermission("wirelessredstone.admin")) {
                player.sendMessage(Component.text("You don't have permission to give Circuit Analysers to other players!", NamedTextColor.RED));
                return;
            }
            
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
                return;
            }
        }
        
        ItemStack analyser = CircuitAnalyserFactory.createCircuitAnalyser();
        
        if (target.getInventory().firstEmpty() != -1) {
            target.getInventory().addItem(analyser);
        } else {
            target.getWorld().dropItemNaturally(target.getLocation(), analyser);
        }
        
        if (target == player) {
            player.sendMessage(Component.text("You received a ", NamedTextColor.GREEN)
                    .append(Component.text("Circuit Analyser", NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text("!", NamedTextColor.GREEN)));
            player.sendMessage(Component.text("Right-click any wireless block to inspect it.", NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("Gave a Circuit Analyser to ", NamedTextColor.GREEN)
                    .append(Component.text(target.getName(), NamedTextColor.AQUA)));
            target.sendMessage(Component.text("You received a ", NamedTextColor.GREEN)
                    .append(Component.text("Circuit Analyser", NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text(" from ", NamedTextColor.GREEN))
                    .append(Component.text(player.getName(), NamedTextColor.AQUA)));
        }
    }

    private void handleRecoverCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /wireless recover <groupname>", NamedTextColor.RED));
            player.sendMessage(Component.text("Recovers lost/missing blocks for a group you own.", NamedTextColor.GRAY));
            player.sendMessage(Component.text("Use /wireless gui to see your group names.", NamedTextColor.GRAY));
            return;
        }

        String groupName = args[1];

        // Search for bulb group first
        Optional<BulbGroup> bulbGroupOpt = findBulbGroupByName(player, groupName);
        Optional<ChestGroup> chestGroupOpt = findChestGroupByName(player, groupName);

        if (bulbGroupOpt.isPresent()) {
            recoverBulbGroup(player, bulbGroupOpt.get());
        } else if (chestGroupOpt.isPresent()) {
            recoverChestGroup(player, chestGroupOpt.get());
        } else {
            player.sendMessage(Component.text("No group found with name: " + groupName, NamedTextColor.RED));
            player.sendMessage(Component.text("Use /wireless gui to see your groups.", NamedTextColor.GRAY));
        }
    }

    private void recoverBulbGroup(Player player, BulbGroup group) {
        List<Integer> unplacedIndices = new ArrayList<>();
        
        for (int i = 0; i < group.getMaxSize(); i++) {
            if (group.getLocation(i) == null) {
                unplacedIndices.add(i);
            }
        }
        
        if (unplacedIndices.isEmpty()) {
            player.sendMessage(Component.text("All blocks in group ", NamedTextColor.YELLOW)
                    .append(Component.text(group.getDisplayName(), NamedTextColor.AQUA))
                    .append(Component.text(" are already placed!", NamedTextColor.YELLOW)));
            return;
        }
        
        BulbVariant variant = BulbVariant.fromBulbType(group.getBulbType());
        ItemStack[] recoveredBulbs = WirelessBulbFactory.createRecoveryBulbs(
                group.getGroupId(), variant, group.getOwnerUuid(), unplacedIndices, group.getMaxSize());
        
        distributeItems(player, recoveredBulbs);
        
        player.sendMessage(Component.text("Recovered ", NamedTextColor.GREEN)
                .append(Component.text(unplacedIndices.size(), NamedTextColor.AQUA))
                .append(Component.text(" missing block(s) from group ", NamedTextColor.GREEN))
                .append(Component.text(group.getDisplayName(), NamedTextColor.AQUA))
                .append(Component.text("!", NamedTextColor.GREEN)));
    }

    private void recoverChestGroup(Player player, ChestGroup group) {
        List<Integer> unplacedIndices = new ArrayList<>();
        
        for (int i = 0; i < group.getMaxSize(); i++) {
            if (group.getLocation(i) == null) {
                unplacedIndices.add(i);
            }
        }
        
        if (unplacedIndices.isEmpty()) {
            player.sendMessage(Component.text("All containers in group ", NamedTextColor.YELLOW)
                    .append(Component.text(group.getDisplayName(), NamedTextColor.GOLD))
                    .append(Component.text(" are already placed!", NamedTextColor.YELLOW)));
            return;
        }
        
        ChestVariant variant = ChestVariant.fromContainerType(group.getContainerType());
        ItemStack[] recoveredChests = WirelessChestFactory.createRecoveryContainers(
                group.getGroupId(), variant, group.getOwnerUuid(), unplacedIndices, group.getMaxSize());
        
        distributeItems(player, recoveredChests);
        
        player.sendMessage(Component.text("Recovered ", NamedTextColor.GREEN)
                .append(Component.text(unplacedIndices.size(), NamedTextColor.GOLD))
                .append(Component.text(" missing container(s) from group ", NamedTextColor.GREEN))
                .append(Component.text(group.getDisplayName(), NamedTextColor.GOLD))
                .append(Component.text("!", NamedTextColor.GREEN)));
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

    private void handleReloadCommand(Player player) {
        if (!player.hasPermission("wirelessredstone.admin")) {
            player.sendMessage(Component.text("You don't have permission to reload configurations!", NamedTextColor.RED));
            return;
        }
        
        plugin.reloadData();
        player.sendMessage(Component.text("WirelessRedstone configuration reloaded!", NamedTextColor.GREEN));
    }

    private void handleSetNameCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /wireless setname <groupName> <newName>", NamedTextColor.RED));
            player.sendMessage(Component.text("Example: /wireless setname MyLamps \"Kitchen Lights\"", NamedTextColor.GRAY));
            return;
        }

        String groupName = args[1];
        // Join remaining args for the new name (handles cases like: setname Group1 My New Name)
        String newName = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));

        Optional<BulbGroup> bulbGroupOpt = findBulbGroupByName(player, groupName);
        Optional<ChestGroup> chestGroupOpt = findChestGroupByName(player, groupName);

        if (bulbGroupOpt.isPresent()) {
            BulbGroup group = bulbGroupOpt.get();
            String oldName = group.getDisplayName();
            group.setCustomName(newName);
            bulbManager.saveData();
            player.sendMessage(Component.text("Renamed group ", NamedTextColor.GREEN)
                    .append(Component.text(oldName, NamedTextColor.AQUA))
                    .append(Component.text(" to ", NamedTextColor.GREEN))
                    .append(Component.text(newName, NamedTextColor.AQUA)));
        } else if (chestGroupOpt.isPresent()) {
            ChestGroup group = chestGroupOpt.get();
            String oldName = group.getDisplayName();
            group.setCustomName(newName);
            chestManager.saveData();
            player.sendMessage(Component.text("Renamed group ", NamedTextColor.GREEN)
                    .append(Component.text(oldName, NamedTextColor.GOLD))
                    .append(Component.text(" to ", NamedTextColor.GREEN))
                    .append(Component.text(newName, NamedTextColor.GOLD)));
        } else {
            player.sendMessage(Component.text("No group found with name: " + groupName, NamedTextColor.RED));
            player.sendMessage(Component.text("Use /wireless gui to see your groups.", NamedTextColor.GRAY));
        }
    }

    private void handleSetCategoryCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /wireless setcategory <groupName> <categoryName>", NamedTextColor.RED));
            player.sendMessage(Component.text("Example: /wireless setcategory MyLamps Kitchen", NamedTextColor.GRAY));
            player.sendMessage(Component.text("Use 'none' to remove from category.", NamedTextColor.GRAY));
            return;
        }

        String groupName = args[1];
        String categoryName = args[2];

        // Find the group
        Optional<BulbGroup> bulbGroupOpt = findBulbGroupByName(player, groupName);
        Optional<ChestGroup> chestGroupOpt = findChestGroupByName(player, groupName);
        
        BaseGroup group = bulbGroupOpt.map(g -> (BaseGroup) g)
                .orElseGet(() -> chestGroupOpt.orElse(null));
        
        if (group == null) {
            player.sendMessage(Component.text("No group found with name: " + groupName, NamedTextColor.RED));
            player.sendMessage(Component.text("Use /wireless gui to see your groups.", NamedTextColor.GRAY));
            return;
        }

        // Handle removing from category
        if (categoryName.equalsIgnoreCase("none") || categoryName.equalsIgnoreCase("uncategorized")) {
            group.setCategoryId(null);
            saveGroupData(group);
            player.sendMessage(Component.text("Removed group ", NamedTextColor.GREEN)
                    .append(Component.text(group.getDisplayName(), NamedTextColor.AQUA))
                    .append(Component.text(" from its category.", NamedTextColor.GREEN)));
            return;
        }

        // Find the category
        Optional<Category> categoryOpt = findCategoryByName(player, categoryName);
        if (categoryOpt.isEmpty()) {
            player.sendMessage(Component.text("No category found with name: " + categoryName, NamedTextColor.RED));
            player.sendMessage(Component.text("Use /wireless gui to see your categories.", NamedTextColor.GRAY));
            return;
        }

        Category category = categoryOpt.get();
        group.setCategoryId(category.getCategoryId());
        saveGroupData(group);
        
        player.sendMessage(Component.text("Assigned group ", NamedTextColor.GREEN)
                .append(Component.text(group.getDisplayName(), NamedTextColor.AQUA))
                .append(Component.text(" to category ", NamedTextColor.GREEN))
                .append(Component.text(category.getName(), NamedTextColor.YELLOW)));
    }

    private Optional<Category> findCategoryByName(Player player, String name) {
        UUID playerUuid = player.getUniqueId();
        boolean isAdmin = player.hasPermission("wirelessredstone.admin");
        
        return categoryManager.getAllCategories().stream()
                .filter(c -> isAdmin || playerUuid.equals(c.getOwnerUuid()))
                .filter(c -> c.getName().equalsIgnoreCase(name) || 
                            c.getCategoryId().toString().substring(0, 8).equalsIgnoreCase(name))
                .findFirst();
    }

    private void saveGroupData(BaseGroup group) {
        if (group instanceof BulbGroup) {
            bulbManager.saveData();
        } else if (group instanceof ChestGroup) {
            chestManager.saveData();
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("=== Wireless Redstone Commands ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/wireless bulbs [count] [variant] [--name=<name>] [--category=<cat>]", NamedTextColor.YELLOW)
                .append(Component.text(" - Get linked copper bulbs", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  Count: 2-26 (default: 2)", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("  Variants: --copper, --exposed, --weathered, --oxidized", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("/wireless lamps [count] [--name=<name>] [--category=<cat>]", NamedTextColor.YELLOW)
                .append(Component.text(" - Get linked redstone lamps", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/wireless chests [count] [variant] [--name=<name>] [--category=<cat>]", NamedTextColor.YELLOW)
                .append(Component.text(" - Get linked wireless containers", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  Variants: --chest, --shulker, --white, --orange, etc.", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("  Copper: --copper, --copper-exposed, --copper-weathered, --copper-oxidized", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("/wireless append <name> [count]", NamedTextColor.YELLOW)
                .append(Component.text(" - Add more blocks to an existing group", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/wireless recover <name>", NamedTextColor.YELLOW)
                .append(Component.text(" - Recover lost/missing blocks from a group", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/wireless setname <groupName> <newName>", NamedTextColor.YELLOW)
                .append(Component.text(" - Rename a group", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/wireless setcategory <groupName> <categoryName>", NamedTextColor.YELLOW)
                .append(Component.text(" - Assign a group to a category", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/wireless inspect [player]", NamedTextColor.YELLOW)
                .append(Component.text(" - Get a Circuit Analyser tool", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/wireless gui [--all]", NamedTextColor.YELLOW)
                .append(Component.text(" - Open management GUI", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/wireless debug on|off", NamedTextColor.YELLOW)
                .append(Component.text(" - Toggle sync debug messages for nearby blocks", NamedTextColor.GRAY)));
        if (player.hasPermission("wirelessredstone.admin")) {
            player.sendMessage(Component.text("/wireless reload", NamedTextColor.YELLOW)
                    .append(Component.text(" - Reload configuration files", NamedTextColor.GRAY)));
        }
    }

    private void giveWirelessBulbs(Player player, BulbVariant variant, int count, String groupName, UUID categoryId) {
        var groupId = bulbManager.createNewGroupId();
        var bulbs = WirelessBulbFactory.createLinkedGroup(groupId, variant, player.getUniqueId(), count);
        distributeItems(player, bulbs);
        
        // Pre-create the group with name and category if specified
        if (groupName != null || categoryId != null) {
            // The group will be created when the first bulb is placed, but we can register it now
            // with the custom properties by creating a placeholder that will be updated on placement
            bulbManager.preRegisterGroup(groupId, count, player.getUniqueId(), variant.getBulbType(), groupName, categoryId);
        }
        
        player.sendMessage(Component.text("You received " + count + " linked " + variant.getDisplayName() + "s!", NamedTextColor.GREEN));
        if (groupName != null) {
            player.sendMessage(Component.text("Group name: ", NamedTextColor.GRAY)
                    .append(Component.text(groupName, NamedTextColor.AQUA)));
        }
        player.sendMessage(Component.text("Place them and they will sync their state!", NamedTextColor.GRAY));
    }

    private void giveWirelessContainers(Player player, ChestVariant variant, int count, String groupName, UUID categoryId) {
        var groupId = chestManager.createNewGroupId();
        var containers = WirelessChestFactory.createLinkedContainers(groupId, variant, player.getUniqueId(), count);
        distributeItems(player, containers);
        
        // Pre-create the group with name and category if specified
        if (groupName != null || categoryId != null) {
            chestManager.preRegisterGroup(groupId, count, player.getUniqueId(), variant.getContainerType(), groupName, categoryId);
        }
        
        player.sendMessage(Component.text("You received " + count + " linked " + variant.getDisplayName() + "s!", NamedTextColor.GREEN));
        if (groupName != null) {
            player.sendMessage(Component.text("Group name: ", NamedTextColor.GRAY)
                    .append(Component.text(groupName, NamedTextColor.GOLD)));
        }
        player.sendMessage(Component.text("Place them and they will sync their contents!", NamedTextColor.GRAY));
    }

    /**
     * Distributes items to a player's inventory, dropping any that don't fit.
     */
    private void distributeItems(Player player, ItemStack[] items) {
        var inventory = player.getInventory();
        for (var item : items) {
            if (inventory.firstEmpty() != -1) {
                inventory.addItem(item);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> subCommands = new ArrayList<>(List.of("bulbs", "lamps", "chests", "append", "extend", "recover", "reclaim", "inspect", "gui", "manage", "list", "debug", "setname", "rename", "setcategory"));
            if (sender.hasPermission("wirelessredstone.admin")) {
                subCommands.add("reload");
            }
            for (String sub : subCommands) {
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
                // Add --name= and --category= suggestions
                addNameAndCategoryCompletions(sender, input, completions);
            } else if (subCommand.equals("lamps")) {
                for (int i = 2; i <= 10; i++) {
                    String num = String.valueOf(i);
                    if (num.startsWith(input)) {
                        completions.add(num);
                    }
                }
                // Add --name= and --category= suggestions
                addNameAndCategoryCompletions(sender, input, completions);
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
                // Add --name= and --category= suggestions
                addNameAndCategoryCompletions(sender, input, completions);
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
                    addGroupNameCompletions(player, input, completions);
                } else if (args.length == 3) {
                    // Suggest counts
                    for (int i = 1; i <= 5; i++) {
                        String num = String.valueOf(i);
                        if (num.startsWith(input)) {
                            completions.add(num);
                        }
                    }
                }
            } else if (subCommand.equals("recover") || subCommand.equals("reclaim")) {
                if (args.length == 2 && sender instanceof Player player) {
                    // Suggest group names that have unplaced blocks
                    UUID playerUuid = player.getUniqueId();
                    boolean isAdmin = player.hasPermission("wirelessredstone.admin");
                    
                    bulbManager.getAllGroups().stream()
                            .filter(g -> isAdmin || playerUuid.equals(g.getOwnerUuid()))
                            .filter(g -> g.getPlacedCount() < g.getMaxSize()) // Only groups with unplaced blocks
                            .forEach(g -> {
                                String name = g.getCustomName() != null ? g.getCustomName() : g.getGroupId().toString().substring(0, 8);
                                if (name.toLowerCase().startsWith(input)) {
                                    completions.add(name.contains(" ") ? "\"" + name + "\"" : name);
                                }
                            });
                    
                    if (chestManager != null) {
                        chestManager.getAllGroups().stream()
                                .filter(g -> isAdmin || playerUuid.equals(g.getOwnerUuid()))
                                .filter(g -> g.getPlacedCount() < g.getMaxSize()) // Only groups with unplaced containers
                                .forEach(g -> {
                                    String name = g.getCustomName() != null ? g.getCustomName() : g.getGroupId().toString().substring(0, 8);
                                    if (name.toLowerCase().startsWith(input)) {
                                        completions.add(name.contains(" ") ? "\"" + name + "\"" : name);
                                    }
                                });
                    }
                }
            } else if (subCommand.equals("setname") || subCommand.equals("rename")) {
                if (args.length == 2 && sender instanceof Player player) {
                    // Suggest group names
                    addGroupNameCompletions(player, input, completions);
                }
                // args.length == 3 is for the new name - no suggestions needed
            } else if (subCommand.equals("setcategory")) {
                if (args.length == 2 && sender instanceof Player player) {
                    // Suggest group names
                    addGroupNameCompletions(player, input, completions);
                } else if (args.length == 3 && sender instanceof Player player) {
                    // Suggest category names
                    addCategoryNameCompletions(player, input, completions);
                    // Also suggest "none" to remove from category
                    if ("none".startsWith(input)) {
                        completions.add("none");
                    }
                }
            } else if (subCommand.equals("inspect")) {
                if (args.length == 2 && sender.hasPermission("wirelessredstone.admin")) {
                    // Suggest online player names
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (onlinePlayer.getName().toLowerCase().startsWith(input)) {
                            completions.add(onlinePlayer.getName());
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

    private void addGroupNameCompletions(Player player, String input, List<String> completions) {
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
    }

    private void addCategoryNameCompletions(Player player, String input, List<String> completions) {
        UUID playerUuid = player.getUniqueId();
        boolean isAdmin = player.hasPermission("wirelessredstone.admin");
        
        categoryManager.getAllCategories().stream()
                .filter(c -> isAdmin || playerUuid.equals(c.getOwnerUuid()))
                .forEach(c -> {
                    String name = c.getName();
                    if (name.toLowerCase().startsWith(input)) {
                        completions.add(name.contains(" ") ? "\"" + name + "\"" : name);
                    }
                });
    }

    private void addNameAndCategoryCompletions(CommandSender sender, String input, List<String> completions) {
        if ("--name=".startsWith(input)) {
            completions.add("--name=");
        }
        if ("--category=".startsWith(input)) {
            completions.add("--category=");
        }
        // If already typing --category=, suggest category names
        if (input.startsWith("--category=") && sender instanceof Player player) {
            String categoryInput = input.substring(11).toLowerCase();
            UUID playerUuid = player.getUniqueId();
            boolean isAdmin = player.hasPermission("wirelessredstone.admin");
            
            categoryManager.getAllCategories().stream()
                    .filter(c -> isAdmin || playerUuid.equals(c.getOwnerUuid()))
                    .forEach(c -> {
                        String name = c.getName();
                        if (name.toLowerCase().startsWith(categoryInput)) {
                            completions.add("--category=" + (name.contains(" ") ? "\"" + name + "\"" : name));
                        }
                    });
        }
    }
}
