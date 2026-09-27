package com.wirelessredstone.command;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.gui.BulbManagerGUI;
import com.wirelessredstone.gui.CategorySelectionGUI;
import com.wirelessredstone.item.BulbVariant;
import com.wirelessredstone.item.ChestVariant;
import com.wirelessredstone.item.ConnectorToolFactory;
import com.wirelessredstone.listener.CircuitAnalyserListener;
import com.wirelessredstone.manager.CategoryManager;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.manager.WireViewManager;
import com.wirelessredstone.model.BaseGroup;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.model.ChestGroup;
import com.wirelessredstone.util.GroupNameParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class WirelessCommand implements CommandExecutor, TabCompleter {

    private final WirelessRedstonePlugin plugin;
    private final LinkedBulbManager bulbManager;
    private final LinkedChestManager chestManager;
    private final CategoryManager categoryManager;

    public WirelessCommand(WirelessRedstonePlugin plugin, LinkedBulbManager bulbManager, LinkedChestManager chestManager, 
                           CategoryManager categoryManager) {
        this.plugin = plugin;
        this.bulbManager = bulbManager;
        this.chestManager = chestManager;
        this.categoryManager = categoryManager;
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

        // No arguments - open GUI by default
        if (args.length < 1) {
            handleGUICommand(player, new String[]{"gui"});
            return true;
        }

        // Parse arguments to handle quoted strings with spaces
        String[] parsedArgs = parseQuotedArgs(args);
        String subCommand = parsedArgs[0].toLowerCase();
        
        switch (subCommand) {
            case "help", "?" -> sendUsage(player);
            case "create" -> handleCreateCommand(player, parsedArgs);
            case "modify" -> handleModifyCommand(player, parsedArgs);
            case "gui", "manage", "list" -> handleGUICommand(player, parsedArgs);
            case "reload" -> handleReloadCommand(player);
            case "circuit-rename" -> handleCircuitRenameCommand(player, parsedArgs);
            case "circuit-category" -> handleCircuitCategoryCommand(player, parsedArgs);
            case "circuit-description" -> handleCircuitDescriptionCommand(player, parsedArgs);
            case "circuit-delete" -> handleCircuitDeleteCommand(player, parsedArgs);
            case "teleport" -> handleTeleportCommand(player, parsedArgs);
            default -> {
                player.sendMessage(Component.text("Unknown subcommand. Use ", NamedTextColor.RED)
                        .append(Component.text("/wireless help", NamedTextColor.YELLOW))
                        .append(Component.text(" for commands.", NamedTextColor.RED)));
            }
        }
        
        return true;
    }

    private Optional<BulbGroup> findBulbGroupByName(Player player, String name) {
        return visibleGroups(player).filter(BulbGroup.class::isInstance)
                .map(BulbGroup.class::cast)
                .filter(g -> matchesGroupName(g.getCustomName(), g.getGroupId(), name)).findFirst();
    }

    private Optional<ChestGroup> findChestGroupByName(Player player, String name) {
        return visibleGroups(player).filter(ChestGroup.class::isInstance)
                .map(ChestGroup.class::cast)
                .filter(g -> matchesGroupName(g.getCustomName(), g.getGroupId(), name)).findFirst();
    }

    private Optional<BaseGroup> findGroupByName(Player player, String name) {
        return visibleGroups(player)
                .filter(g -> matchesGroupName(g.getCustomName(), g.getGroupId(), name)).findFirst();
    }

    private java.util.stream.Stream<BaseGroup> visibleGroups(Player player) {
        var bulbs = bulbManager.getAllGroups().stream().map(BaseGroup.class::cast);
        var chests = chestManager == null
                ? java.util.stream.Stream.<BaseGroup>empty()
                : chestManager.getAllGroups().stream().map(BaseGroup.class::cast);
        return java.util.stream.Stream.concat(bulbs, chests)
                .filter(g -> player.hasPermission("wirelessredstone.admin")
                        || player.getUniqueId().equals(g.getOwnerUuid()));
    }

    private boolean matchesGroupName(String customName, UUID groupId, String searchName) {
        // Match by custom name (case-insensitive)
        if (customName != null && customName.equalsIgnoreCase(searchName)) {
            return true;
        }
        if (customName != null && GroupNameParser.parse(customName).groupName().equalsIgnoreCase(searchName)) {
            return true;
        }
        // Match by partial group ID
        String shortId = groupId.toString().substring(0, 8);
        return shortId.equalsIgnoreCase(searchName) || groupId.toString().startsWith(searchName.toLowerCase());
    }

    /**
     * Handles /wireless create [groupName].
     * Without arguments, starts the same chat prompt used by the GUI connector action.
     * With a name, creates a circuit tool for the specified group (or creates a new group if it doesn't exist).
     * Use category/group-name to make the group appear under a category.
     */
    private void handleCreateCommand(Player player, String[] args) {
        if (args.length < 2) {
            CategorySelectionGUI.startConnectorToolPrompt(player, null);
            return;
        }

        String groupName = args[1];
        if (args.length >= 3 && !groupName.contains("/")) {
            groupName = args[2] + "/" + groupName;
        }

        Optional<BaseGroup> groupOpt = findGroupByName(player, groupName);

        if (groupOpt.isPresent()) {
            BaseGroup group = groupOpt.get();
            boolean bulb = group instanceof BulbGroup;
            ConnectorToolFactory.GroupType type = bulb
                    ? ConnectorToolFactory.GroupType.BULB : ConnectorToolFactory.GroupType.CHEST;
            NamedTextColor color = bulb
                    ? WireViewManager.getBulbGroupTextColor(group.getGroupId(), bulbManager.getAllPlacedGroups())
                    : WireViewManager.getChestGroupTextColor(group.getGroupId(), chestManager.getAllPlacedGroups());
            ItemStack tool = ConnectorToolFactory.createConnectorTool(
                    group.getGroupId(), group.getDisplayName(), type, color);
            giveItemToPlayer(player, tool);
            player.sendMessage(Component.text("You received a ", NamedTextColor.GREEN)
                    .append(Component.text("Circuit Tool", NamedTextColor.GREEN).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD))
                    .append(Component.text(" for group ", NamedTextColor.GREEN))
                    .append(Component.text(group.getDisplayName(), color)));
            String targets = bulb ? "bulbs/lamps" : "containers";
            player.sendMessage(Component.text("Right-click " + targets
                    + " to add, Left-click any wireless block to remove it from its group.", NamedTextColor.GRAY));
        } else {
            // No group found - create a creation-mode tool with optional category
            ItemStack tool = ConnectorToolFactory.createCreationModeConnectorTool(groupName);
            giveItemToPlayer(player, tool);
            player.sendMessage(Component.text("You received a ", NamedTextColor.GREEN)
                    .append(Component.text("Circuit Tool", NamedTextColor.GREEN).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD))
                    .append(Component.text(" in ", NamedTextColor.GREEN))
                    .append(Component.text("creation mode", NamedTextColor.LIGHT_PURPLE)));
            player.sendMessage(Component.text("Right-click a bulb, lamp, or chest to create group \"", NamedTextColor.GRAY)
                    .append(Component.text(groupName, NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text("\".", NamedTextColor.GRAY)));
            if (GroupNameParser.parse(groupName).hasCategory()) {
                player.sendMessage(Component.text("Category: ", NamedTextColor.GRAY)
                        .append(Component.text(GroupNameParser.parse(groupName).categoryName(), NamedTextColor.YELLOW)));
            }
        }
    }

    /**
     * Handles /wireless modify name <groupName> <newValue>.
     */
    private void handleModifyCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /wireless modify name <groupName> <newName>", NamedTextColor.RED));
            player.sendMessage(Component.text("/wireless modify name <groupName> <newName>", NamedTextColor.GRAY)
                    .append(Component.text(" - Rename a group", NamedTextColor.DARK_GRAY)));
            return;
        }

        String modifyType = args[1].toLowerCase();
        String[] subArgs = java.util.Arrays.copyOfRange(args, 1, args.length);

        switch (modifyType) {
            case "name" -> handleModifyNameCommand(player, subArgs);
            default -> {
                player.sendMessage(Component.text("Unknown modify type: " + modifyType, NamedTextColor.RED));
                player.sendMessage(Component.text("Available: name", NamedTextColor.GRAY));
            }
        }
    }

    /**
     * Handles /wireless modify name <groupName> <newName>
     */
    private void handleModifyNameCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /wireless modify name <groupName> <newName>", NamedTextColor.RED));
            player.sendMessage(Component.text("Example: /wireless modify name MyLamps \"Kitchen Lights\"", NamedTextColor.GRAY));
            return;
        }

        String groupName = args[1];
        // Join remaining args for the new name (handles cases like: modify name Group1 My New Name)
        String newName = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));

        Optional<BaseGroup> groupOpt = findGroupByName(player, groupName);

        if (groupOpt.isPresent()) {
            BaseGroup group = groupOpt.get();
            String oldName = group.getDisplayName();
            group.setCustomName(newName);
            saveGroupData(group);
            NamedTextColor color = group instanceof BulbGroup ? NamedTextColor.AQUA : NamedTextColor.GOLD;
            player.sendMessage(Component.text("Renamed group ", NamedTextColor.GREEN)
                    .append(Component.text(oldName, color))
                    .append(Component.text(" to ", NamedTextColor.GREEN))
                    .append(Component.text(newName, color)));
        } else {
            player.sendMessage(Component.text("No group found with name: " + groupName, NamedTextColor.RED));
            player.sendMessage(Component.text("Use /wireless gui to see your groups.", NamedTextColor.GRAY));
        }
    }

    private void giveItemToPlayer(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
    }

    private void handleGUICommand(Player player, String[] args) {
        boolean showAll = args.length >= 2 && args[1].equalsIgnoreCase("--all");
        new BulbManagerGUI(bulbManager, chestManager, categoryManager, player, showAll, null).open();
    }

    private void handleReloadCommand(Player player) {
        if (!player.hasPermission("wirelessredstone.admin")) {
            player.sendMessage(Component.text("You don't have permission to reload configurations!", NamedTextColor.RED));
            return;
        }
        
        plugin.reloadData();
        player.sendMessage(Component.text("WirelessRedstone configuration reloaded!", NamedTextColor.GREEN));
    }

    /**
     * Internal command used by clickable GUI group-detail rows.
     */
    private void handleTeleportCommand(Player player, String[] args) {
        if (args.length < 4) {
            return;
        }

        if (!player.hasPermission("wirelessredstone.teleport")) {
            player.sendMessage(Component.text("You don't have permission to teleport!", NamedTextColor.RED));
            return;
        }

        try {
            UUID groupId = UUID.fromString(args[1]);
            String groupType = args[2].toLowerCase();
            int index = Integer.parseInt(args[3]);

            BaseGroup group;
            NamedTextColor groupColor;
            if (groupType.equals("bulb")) {
                group = bulbManager.getGroupById(groupId).orElse(null);
                groupColor = NamedTextColor.AQUA;
            } else if (groupType.equals("chest")) {
                group = chestManager != null ? chestManager.getGroupById(groupId).orElse(null) : null;
                groupColor = NamedTextColor.GOLD;
            } else {
                return;
            }

            if (group == null) {
                player.sendMessage(Component.text("That wireless group no longer exists.", NamedTextColor.RED));
                return;
            }

            if (!player.hasPermission("wirelessredstone.admin")
                    && group.getOwnerUuid() != null
                    && !player.getUniqueId().equals(group.getOwnerUuid())) {
                player.sendMessage(Component.text("You can only teleport to your own wireless groups.", NamedTextColor.RED));
                return;
            }

            Location location = group.getLocation(index);
            if (location == null) {
                player.sendMessage(Component.text("Slot " + BaseGroup.getIndexLabel(index) + " is not placed yet.", NamedTextColor.RED));
                return;
            }

            Location teleportLocation = location.clone().add(0.5, 1, 0.5);
            teleportLocation.setYaw(player.getLocation().getYaw());
            teleportLocation.setPitch(player.getLocation().getPitch());
            player.teleport(teleportLocation);
            player.sendMessage(Component.text("Teleported to ", NamedTextColor.GREEN)
                    .append(Component.text(group.getDisplayName(), groupColor))
                    .append(Component.text(" slot " + BaseGroup.getIndexLabel(index) + ".", NamedTextColor.GREEN)));
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("That teleport link is no longer valid.", NamedTextColor.RED));
        }
    }

    /**
     * Handles the circuit-rename command triggered from the circuit report.
     * This initiates a chat input capture for renaming a group.
     */
    private void handleCircuitRenameCommand(Player player, String[] args) {
        if (args.length < 3) {
            return; // Silent fail - this is an internal command
        }

        try {
            UUID groupId = UUID.fromString(args[1]);
            boolean isBulbGroup = args[2].equalsIgnoreCase("bulb");
            CircuitAnalyserListener.initiateRename(player, groupId, isBulbGroup);
        } catch (IllegalArgumentException e) {
            // Invalid UUID - silent fail
        }
    }

    /**
     * Handles the circuit-category command triggered from the circuit report.
     * This initiates a chat input capture for changing a group's category.
     */
    private void handleCircuitCategoryCommand(Player player, String[] args) {
        if (args.length < 3) {
            return; // Silent fail - this is an internal command
        }

        try {
            UUID groupId = UUID.fromString(args[1]);
            boolean isBulbGroup = args[2].equalsIgnoreCase("bulb");
            CircuitAnalyserListener.initiateCategoryChange(player, groupId, isBulbGroup, categoryManager);
        } catch (IllegalArgumentException e) {
            // Invalid UUID - silent fail
        }
    }

    /**
     * Handles the circuit-description command triggered from the circuit report.
     * This initiates a chat input capture for changing a group's description.
     */
    private void handleCircuitDescriptionCommand(Player player, String[] args) {
        if (args.length < 3) {
            return; // Silent fail - this is an internal command
        }

        try {
            UUID groupId = UUID.fromString(args[1]);
            boolean isBulbGroup = args[2].equalsIgnoreCase("bulb");
            CircuitAnalyserListener.initiateDescriptionChange(player, groupId, isBulbGroup);
        } catch (IllegalArgumentException e) {
            // Invalid UUID - silent fail
        }
    }

    private void handleCircuitDeleteCommand(Player player, String[] args) {
        if (args.length < 3 || !player.hasPermission("wirelessredstone.remove")) {
            player.sendMessage(Component.text("You don't have permission to remove groups.", NamedTextColor.RED));
            return;
        }

        try {
            UUID groupId = UUID.fromString(args[1]);
            BaseGroup group = switch (args[2].toLowerCase()) {
                case "bulb" -> bulbManager.getGroupById(groupId).orElse(null);
                case "chest" -> chestManager.getGroupById(groupId).orElse(null);
                default -> null;
            };
            if (group == null) {
                player.sendMessage(Component.text("That wireless group no longer exists.", NamedTextColor.RED));
                return;
            }
            if (group.getOwnerUuid() != null && !group.getOwnerUuid().equals(player.getUniqueId())
                    && !player.hasPermission("wirelessredstone.admin")) {
                player.sendMessage(Component.text("You can only remove your own groups.", NamedTextColor.RED));
                return;
            }

            if (group instanceof BulbGroup) {
                bulbManager.removeGroup(groupId);
            } else {
                chestManager.removeGroup(groupId);
            }
            plugin.getWireViewManager().refreshAllPlayers();
            player.sendMessage(Component.text("Group deleted: " + group.getDisplayName(), NamedTextColor.GREEN));
        } catch (IllegalArgumentException ignored) {
            player.sendMessage(Component.text("That delete link is no longer valid.", NamedTextColor.RED));
        }
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

        player.sendMessage(Component.text("/wireless create", NamedTextColor.YELLOW)
                .append(Component.text(" - Enter a new group name and receive a Circuit Tool", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/wireless create <groupName>", NamedTextColor.YELLOW)
                .append(Component.text(" - Get a Circuit Tool directly; use category/group for categories", NamedTextColor.GRAY)));
        
        // Group management
        player.sendMessage(Component.text("/wireless modify name <groupName> <newName>", NamedTextColor.YELLOW)
                .append(Component.text(" - Rename a group", NamedTextColor.GRAY)));
        // Other
        player.sendMessage(Component.text("/wireless gui [--all]", NamedTextColor.YELLOW)
                .append(Component.text(" - Open management GUI", NamedTextColor.GRAY)));
        if (player.hasPermission("wirelessredstone.admin")) {
            player.sendMessage(Component.text("/wireless reload", NamedTextColor.YELLOW)
                    .append(Component.text(" - Reload configuration files", NamedTextColor.GRAY)));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> subCommands = new ArrayList<>(List.of(
                    "create", "modify",
                    "gui", "manage", "list"
            ));
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
            
            // /wireless create <groupName>
            if (subCommand.equals("create")) {
                if (args.length == 2 && sender instanceof Player player) {
                    // Suggest existing group names
                    addGroupNameCompletions(player, input, completions);
                }
            }
            // /wireless modify <name|category> <groupName> <newValue>
            else if (subCommand.equals("modify")) {
                if (args.length == 2) {
                    for (String modifyType : List.of("name")) {
                        if (modifyType.startsWith(input)) {
                            completions.add(modifyType);
                        }
                    }
                } else if (args.length == 3 && sender instanceof Player player) {
                    // Suggest group names
                    addGroupNameCompletions(player, input, completions);
                } else if (args.length == 4 && sender instanceof Player player) {
                    String modifyType = args[1].toLowerCase();
                    if (modifyType.equals("category")) {
                        // Suggest category names + "none"
                        addCategoryNameCompletions(player, input, completions);
                        if ("none".startsWith(input)) {
                            completions.add("none");
                        }
                    }
                    // For "name", no suggestions - user provides new name
                }
            }
            // /wireless gui [--all]
            else if (subCommand.equals("gui") || subCommand.equals("manage") || subCommand.equals("list")) {
                if ("--all".startsWith(input) && sender.hasPermission("wirelessredstone.admin")) {
                    completions.add("--all");
                }
            }
        }

        return completions;
    }

    private void addGroupNameCompletions(Player player, String input, List<String> completions) {
        visibleGroups(player)
                .forEach(g -> {
                    String name = g.getCustomName() != null ? g.getCustomName() : g.getGroupId().toString().substring(0, 8);
                    if (name.toLowerCase().startsWith(input)) {
                        completions.add(name.contains(" ") ? "\"" + name + "\"" : name);
                    }
                });
    }

    private void addCategoryNameCompletions(Player player, String input, List<String> completions) {
        Set<String> categoryNames = new HashSet<>();
        visibleGroups(player)
                .map(g -> GroupNameParser.parse(g.getDisplayName()).categoryName())
                .filter(Objects::nonNull)
                .forEach(categoryNames::add);

        categoryNames.stream()
                .filter(name -> name.toLowerCase().startsWith(input))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(name -> completions.add(name.contains(" ") ? "\"" + name + "\"" : name));
    }

}
