package com.wirelessredstone.listener;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.item.CircuitAnalyserFactory;
import com.wirelessredstone.manager.CategoryManager;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.model.BaseGroup;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.model.Category;
import com.wirelessredstone.model.ChestGroup;
import com.wirelessredstone.util.BulbUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener for Circuit Analyser interactions.
 * Handles right-clicks on wireless blocks to display detailed information.
 */
public class CircuitAnalyserListener implements Listener {

    private final LinkedBulbManager bulbManager;
    private final LinkedChestManager chestManager;
    private final CategoryManager categoryManager;

    // Pending rename/category operations from the circuit analyser report
    private static final Map<UUID, PendingOperation> pendingOperations = new ConcurrentHashMap<>();

    private record PendingOperation(UUID groupId, boolean isBulbGroup, OperationType type) {}
    private enum OperationType { RENAME, CATEGORY }

    public CircuitAnalyserListener(LinkedBulbManager bulbManager, LinkedChestManager chestManager, CategoryManager categoryManager) {
        this.bulbManager = bulbManager;
        this.chestManager = chestManager;
        this.categoryManager = categoryManager;
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        var item = player.getInventory().getItemInMainHand();

        if (!CircuitAnalyserFactory.isCircuitAnalyser(item)) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        event.setCancelled(true);

        Location location = block.getLocation();

        // Check if it's a wireless bulb
        if (bulbManager.isWirelessBulbLocation(location)) {
            displayBulbInfo(player, location);
            return;
        }

        // Check if it's a wireless chest
        if (chestManager.isWirelessChestLocation(location)) {
            displayChestInfo(player, location);
            return;
        }

        // Not a wireless block
        player.sendMessage(Component.text("⚡ ", NamedTextColor.YELLOW)
                .append(Component.text("This block is not part of a wireless group.", NamedTextColor.GRAY)));
    }

    private void displayBulbInfo(Player player, Location location) {
        Optional<BulbGroup> groupOpt = bulbManager.getGroupByLocation(location);
        if (groupOpt.isEmpty()) {
            player.sendMessage(Component.text("Error: Could not find group data.", NamedTextColor.RED));
            return;
        }

        BulbGroup group = groupOpt.get();
        displayGroupInfo(player, group, "Wireless Bulb", NamedTextColor.AQUA);
    }

    private void displayChestInfo(Player player, Location location) {
        Optional<ChestGroup> groupOpt = chestManager.getGroupByLocation(location);
        if (groupOpt.isEmpty()) {
            player.sendMessage(Component.text("Error: Could not find group data.", NamedTextColor.RED));
            return;
        }

        ChestGroup group = groupOpt.get();
        displayGroupInfo(player, group, "Wireless Container", NamedTextColor.GOLD);
    }

    private void displayGroupInfo(Player player, BaseGroup group, String typeName, NamedTextColor typeColor) {
        boolean isBulbGroup = group instanceof BulbGroup;
        UUID groupId = group.getGroupId();

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("═══ ⚡ Circuit Analysis ⚡ ═══", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true));

        // Group Name (clickable to rename) - more prominent
        String displayName = group.getDisplayName();
        String renameCommand = "/wireless analyser-rename " + groupId + " " + (isBulbGroup ? "bulb" : "chest");
        player.sendMessage(Component.text("Name: ", NamedTextColor.GRAY)
                .append(Component.text(displayName, NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true)
                        .hoverEvent(HoverEvent.showText(Component.text("Click to rename", NamedTextColor.YELLOW)))
                        .clickEvent(ClickEvent.runCommand(renameCommand)))
                .append(Component.text(" ✎", NamedTextColor.DARK_GRAY)));

        // Category (clickable to change)
        String categoryName = "Uncategorized";
        if (group.getCategoryId() != null) {
            Optional<Category> categoryOpt = categoryManager.getCategoryById(group.getCategoryId());
            categoryName = categoryOpt.map(Category::getName).orElse("Unknown");
        }
        String setCategoryCommand = "/wireless analyser-category " + groupId + " " + (isBulbGroup ? "bulb" : "chest");
        player.sendMessage(Component.text("Category: ", NamedTextColor.GRAY)
                .append(Component.text(categoryName, NamedTextColor.YELLOW)
                        .hoverEvent(HoverEvent.showText(Component.text("Click to change category", NamedTextColor.YELLOW)))
                        .clickEvent(ClickEvent.runCommand(setCategoryCommand)))
                .append(Component.text(" ✎", NamedTextColor.DARK_GRAY)));

        // Placed count and state on same line for bulbs
        int placedCount = group.getPlacedCount();
        int maxSize = group.getMaxSize();
        NamedTextColor countColor = placedCount == maxSize ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
        
        if (group instanceof BulbGroup bulbGroup) {
            player.sendMessage(Component.text("Placed: ", NamedTextColor.GRAY)
                    .append(Component.text(placedCount + "/" + maxSize, countColor))
                    .append(Component.text(" | State: ", NamedTextColor.GRAY))
                    .append(Component.text(bulbGroup.isLit() ? "ON" : "OFF", 
                            bulbGroup.isLit() ? NamedTextColor.GREEN : NamedTextColor.RED)));
        } else {
            player.sendMessage(Component.text("Placed: ", NamedTextColor.GRAY)
                    .append(Component.text(placedCount + "/" + maxSize, countColor)));
        }

        // Connector Tool button (clickable to get tool)
        String createToolCommand = "/wireless create " + displayName;
        player.sendMessage(Component.text("[Get Connector Tool]", NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .hoverEvent(HoverEvent.showText(Component.text("Click to receive a Connector Tool for this group", NamedTextColor.YELLOW)))
                .clickEvent(ClickEvent.runCommand(createToolCommand)));

        // Collapsible locations section
        player.sendMessage(Component.text("Blocks:", NamedTextColor.GRAY)
                .decoration(TextDecoration.UNDERLINED, true));

        // List all block locations in a more compact format
        List<Location> locations = group.getLocations();
        for (int i = 0; i < locations.size(); i++) {
            Location loc = locations.get(i);
            String label = BaseGroup.getIndexLabel(i);
            
            if (loc != null) {
                String coords = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
                
                Component locationComponent = Component.text("[" + label + "] ", NamedTextColor.DARK_AQUA)
                        .append(Component.text(coords, NamedTextColor.WHITE)
                                .hoverEvent(HoverEvent.showText(Component.text("Click to teleport", NamedTextColor.YELLOW)))
                                .clickEvent(ClickEvent.runCommand("/tp " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ())));
                
                player.sendMessage(locationComponent);
            } else {
                player.sendMessage(Component.text("[" + label + "] ", NamedTextColor.DARK_AQUA)
                        .append(Component.text("Not placed", NamedTextColor.RED)));
            }
        }

        player.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.DARK_GRAY));
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        
        PendingOperation operation = pendingOperations.get(playerUuid);
        if (operation == null) return;
        
        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        
        player.getServer().getScheduler().runTask(
            WirelessRedstonePlugin.getInstance(),
            () -> processPendingOperation(player, message, operation)
        );
    }

    private void processPendingOperation(Player player, String input, PendingOperation operation) {
        pendingOperations.remove(player.getUniqueId());
        
        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(Component.text("Operation cancelled.", NamedTextColor.GRAY));
            return;
        }

        BaseGroup group = operation.isBulbGroup() 
            ? bulbManager.getGroupById(operation.groupId()).orElse(null)
            : chestManager.getGroupById(operation.groupId()).orElse(null);
        
        if (group == null) {
            player.sendMessage(Component.text("Group no longer exists!", NamedTextColor.RED));
            return;
        }

        if (operation.type() == OperationType.RENAME) {
            processRename(player, input, group, operation.isBulbGroup());
        } else {
            processCategoryChange(player, input, group, operation.isBulbGroup());
        }
    }

    private void processRename(Player player, String newName, BaseGroup group, boolean isBulbGroup) {
        if (newName.length() > 32) {
            newName = newName.substring(0, 32);
        }

        if (newName.equalsIgnoreCase("reset") || newName.equalsIgnoreCase("clear")) {
            group.setCustomName(null);
            player.sendMessage(Component.text("✓ Group name reset to default: ", NamedTextColor.GREEN)
                    .append(Component.text(group.getDisplayName(), isBulbGroup ? NamedTextColor.AQUA : NamedTextColor.GOLD)));
        } else {
            group.setCustomName(newName);
            player.sendMessage(Component.text("✓ Group renamed to: ", NamedTextColor.GREEN)
                    .append(Component.text(newName, isBulbGroup ? NamedTextColor.AQUA : NamedTextColor.GOLD)));
        }
        
        if (isBulbGroup) {
            bulbManager.saveData();
        } else {
            chestManager.saveData();
        }
    }

    private void processCategoryChange(Player player, String input, BaseGroup group, boolean isBulbGroup) {
        if (input.equalsIgnoreCase("none") || input.equalsIgnoreCase("uncategorized")) {
            group.setCategoryId(null);
            player.sendMessage(Component.text("✓ Group moved to ", NamedTextColor.GREEN)
                    .append(Component.text("Uncategorized", NamedTextColor.YELLOW)));
        } else {
            List<Category> playerCategories = categoryManager.getCategoriesByOwner(player.getUniqueId());
            Category targetCategory = null;
            
            // Try to parse as number
            try {
                int index = Integer.parseInt(input);
                if (index >= 1 && index <= playerCategories.size()) {
                    targetCategory = playerCategories.get(index - 1);
                }
            } catch (NumberFormatException ignored) {}
            
            // Try to find by name
            if (targetCategory == null) {
                for (Category cat : playerCategories) {
                    if (cat.getName().equalsIgnoreCase(input) || cat.getDisplayName().equalsIgnoreCase(input)) {
                        targetCategory = cat;
                        break;
                    }
                }
            }
            
            if (targetCategory == null) {
                player.sendMessage(Component.text("Category not found: " + input, NamedTextColor.RED));
                player.sendMessage(Component.text("Type 'none' to move to Uncategorized.", NamedTextColor.GRAY));
                return;
            }
            
            group.setCategoryId(targetCategory.getCategoryId());
            player.sendMessage(Component.text("✓ Group moved to category: ", NamedTextColor.GREEN)
                    .append(Component.text(targetCategory.getDisplayName(), NamedTextColor.YELLOW)));
        }
        
        if (isBulbGroup) {
            bulbManager.saveData();
        } else {
            chestManager.saveData();
        }
    }

    /**
     * Initiates a rename operation for a group from the analyser report.
     */
    public static void initiateRename(Player player, UUID groupId, boolean isBulbGroup) {
        pendingOperations.put(player.getUniqueId(), new PendingOperation(groupId, isBulbGroup, OperationType.RENAME));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("✎ ", NamedTextColor.YELLOW)
                .append(Component.text("Enter new name in chat ", NamedTextColor.GRAY))
                .append(Component.text("(or 'cancel' to abort, 'reset' for default)", NamedTextColor.DARK_GRAY)));
    }

    /**
     * Initiates a category change operation for a group from the analyser report.
     */
    public static void initiateCategoryChange(Player player, UUID groupId, boolean isBulbGroup, CategoryManager categoryManager) {
        pendingOperations.put(player.getUniqueId(), new PendingOperation(groupId, isBulbGroup, OperationType.CATEGORY));
        
        List<Category> playerCategories = categoryManager.getCategoriesByOwner(player.getUniqueId());
        
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("✎ ", NamedTextColor.YELLOW)
                .append(Component.text("Enter category name or number ", NamedTextColor.GRAY))
                .append(Component.text("('cancel' to abort, 'none' for uncategorized)", NamedTextColor.DARK_GRAY)));
        
        if (!playerCategories.isEmpty()) {
            player.sendMessage(Component.text("  Your categories:", NamedTextColor.GRAY));
            for (int i = 0; i < playerCategories.size(); i++) {
                player.sendMessage(Component.text("  " + (i + 1) + ". ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(playerCategories.get(i).getDisplayName(), NamedTextColor.YELLOW)));
            }
        } else {
            player.sendMessage(Component.text("  You have no categories yet.", NamedTextColor.GRAY));
        }
    }

    /**
     * Checks if a player has a pending operation.
     */
    public static boolean hasPendingOperation(UUID playerUuid) {
        return pendingOperations.containsKey(playerUuid);
    }

    /**
     * Cancels any pending operation for a player.
     */
    public static void cancelPendingOperation(UUID playerUuid) {
        pendingOperations.remove(playerUuid);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        
        // Clean up pending operations
        cancelPendingOperation(playerUuid);
        
        // Clean up analyser wireview when player leaves
        var analyserTask = WirelessRedstonePlugin.getInstance().getAnalyserWireViewTask();
        if (analyserTask != null) {
            analyserTask.cleanupPlayer(event.getPlayer());
        }
    }
}
