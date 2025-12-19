package com.wirelessredstone.listener;

import com.wirelessredstone.item.CircuitAnalyserFactory;
import com.wirelessredstone.manager.CategoryManager;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.model.BaseGroup;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.model.Category;
import com.wirelessredstone.model.ChestGroup;
import com.wirelessredstone.util.BulbUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Listener for Circuit Analyser interactions.
 * Handles right-clicks on wireless blocks to display detailed information.
 */
public class CircuitAnalyserListener implements Listener {

    private final LinkedBulbManager bulbManager;
    private final LinkedChestManager chestManager;
    private final CategoryManager categoryManager;

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
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("  ⚡ Circuit Analysis Report ⚡", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true));
        player.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.empty());

        // Type
        player.sendMessage(Component.text("  Type: ", NamedTextColor.GRAY)
                .append(Component.text(typeName, typeColor)));

        // Group Name
        String displayName = group.getDisplayName();
        player.sendMessage(Component.text("  Name: ", NamedTextColor.GRAY)
                .append(Component.text(displayName, NamedTextColor.WHITE)));

        // Group ID (clickable to copy)
        String fullGroupId = group.getGroupId().toString();
        player.sendMessage(Component.text("  Group ID: ", NamedTextColor.GRAY)
                .append(Component.text(fullGroupId.substring(0, 8), NamedTextColor.DARK_AQUA)
                        .hoverEvent(HoverEvent.showText(Component.text("Click to copy full ID", NamedTextColor.YELLOW)))
                        .clickEvent(ClickEvent.copyToClipboard(fullGroupId))));

        // Category
        String categoryName = "Uncategorized";
        if (group.getCategoryId() != null) {
            Optional<Category> categoryOpt = categoryManager.getCategoryById(group.getCategoryId());
            categoryName = categoryOpt.map(Category::getName).orElse("Unknown");
        }
        player.sendMessage(Component.text("  Category: ", NamedTextColor.GRAY)
                .append(Component.text(categoryName, NamedTextColor.YELLOW)));

        // Owner
        String ownerName = "Unknown";
        if (group.getOwnerUuid() != null) {
            var offlinePlayer = Bukkit.getOfflinePlayer(group.getOwnerUuid());
            ownerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : group.getOwnerUuid().toString().substring(0, 8);
        }
        player.sendMessage(Component.text("  Owner: ", NamedTextColor.GRAY)
                .append(Component.text(ownerName, NamedTextColor.GREEN)));

        // Placed count
        int placedCount = group.getPlacedCount();
        int maxSize = group.getMaxSize();
        NamedTextColor countColor = placedCount == maxSize ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
        player.sendMessage(Component.text("  Placed: ", NamedTextColor.GRAY)
                .append(Component.text(placedCount + "/" + maxSize, countColor)));

        // Additional info for specific types
        if (group instanceof BulbGroup bulbGroup) {
            String bulbTypeName = bulbGroup.getBulbType() == com.wirelessredstone.item.BulbVariant.BulbType.REDSTONE_LAMP 
                    ? "Redstone Lamp" : "Copper Bulb";
            player.sendMessage(Component.text("  Bulb Type: ", NamedTextColor.GRAY)
                    .append(Component.text(bulbTypeName, NamedTextColor.WHITE)));
            player.sendMessage(Component.text("  State: ", NamedTextColor.GRAY)
                    .append(Component.text(bulbGroup.isLit() ? "ON" : "OFF", 
                            bulbGroup.isLit() ? NamedTextColor.GREEN : NamedTextColor.RED)));
        } else if (group instanceof ChestGroup chestGroup) {
            String containerTypeName = switch (chestGroup.getContainerType()) {
                case CHEST -> "Chest";
                case SHULKER -> "Shulker Box";
                case COPPER_CHEST -> "Copper Chest";
            };
            player.sendMessage(Component.text("  Container: ", NamedTextColor.GRAY)
                    .append(Component.text(containerTypeName, NamedTextColor.WHITE)));
        }

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  Associated Blocks:", NamedTextColor.GRAY)
                .decoration(TextDecoration.UNDERLINED, true));

        // List all block locations
        List<Location> locations = group.getLocations();
        for (int i = 0; i < locations.size(); i++) {
            Location loc = locations.get(i);
            String label = BaseGroup.getIndexLabel(i);
            
            if (loc != null) {
                String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "Unknown";
                String coords = loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
                
                Component locationComponent = Component.text("  [" + label + "] ", NamedTextColor.DARK_AQUA)
                        .append(Component.text(worldName + ": ", NamedTextColor.GRAY))
                        .append(Component.text(coords, NamedTextColor.WHITE)
                                .hoverEvent(HoverEvent.showText(Component.text("Click to teleport", NamedTextColor.YELLOW)))
                                .clickEvent(ClickEvent.runCommand("/tp " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ())));
                
                player.sendMessage(locationComponent);
            } else {
                player.sendMessage(Component.text("  [" + label + "] ", NamedTextColor.DARK_AQUA)
                        .append(Component.text("Not placed", NamedTextColor.RED)));
            }
        }

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.empty());
    }
}
