package com.wirelessredstone.listener;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.item.BulbVariant;
import com.wirelessredstone.item.ChestVariant;
import com.wirelessredstone.item.ConnectorToolFactory;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.model.ChestGroup;
import com.wirelessredstone.util.BulbUtils;
import com.wirelessredstone.util.ParticleEffects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.CopperBulb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Listener for Connector Tool interactions.
 * Handles right-click to add blocks to a group and left-click to remove blocks from a group.
 */
public class ConnectorToolListener implements Listener {

    private final LinkedBulbManager bulbManager;
    private final LinkedChestManager chestManager;
    
    // Cooldown to prevent duplicate event triggers
    private final Map<UUID, Long> creationCooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 250;

    public ConnectorToolListener(LinkedBulbManager bulbManager, LinkedChestManager chestManager) {
        this.bulbManager = bulbManager;
        this.chestManager = chestManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        var item = player.getInventory().getItemInMainHand();

        if (!ConnectorToolFactory.isConnectorTool(item)) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        event.setCancelled(true);

        Location location = block.getLocation();

        // Check if this is a creation mode tool
        if (ConnectorToolFactory.isCreationMode(item)) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                // Check cooldown to prevent duplicate creation
                long now = System.currentTimeMillis();
                Long lastUse = creationCooldowns.get(player.getUniqueId());
                if (lastUse != null && (now - lastUse) < COOLDOWN_MS) {
                    return;
                }
                creationCooldowns.put(player.getUniqueId(), now);
                
                handleCreationModeAdd(player, location, block, item);
            } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                player.sendMessage(Component.text("This tool is in creation mode. Right-click a block to create the group first.", NamedTextColor.YELLOW));
            }
            return;
        }

        // Regular mode - get group info
        UUID groupId = ConnectorToolFactory.getGroupId(item);
        ConnectorToolFactory.GroupType groupType = ConnectorToolFactory.getGroupType(item);

        if (groupId == null || groupType == null) {
            player.sendMessage(Component.text("Invalid connector tool!", NamedTextColor.RED));
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Add block to group
            handleAddBlock(player, location, block, groupId, groupType);
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Remove block from group
            handleRemoveBlock(player, location, groupId, groupType);
        }
    }

    private void handleCreationModeAdd(Player player, Location location, Block block, ItemStack tool) {
        String groupName = ConnectorToolFactory.getGroupName(tool);
        if (groupName == null) {
            player.sendMessage(Component.text("Invalid creation mode tool!", NamedTextColor.RED));
            return;
        }

        Material material = block.getType();
        
        // Determine if this is a bulb/lamp or a chest
        BulbVariant.BulbType bulbType = BulbUtils.getBulbTypeFromMaterial(material);
        ChestVariant.ContainerType containerType = getContainerTypeFromMaterial(material);

        if (bulbType != null) {
            // Create a bulb group
            createBulbGroupFromTool(player, location, block, groupName, bulbType, tool);
        } else if (containerType != null) {
            // Create a chest group
            createChestGroupFromTool(player, location, block, groupName, containerType, tool);
        } else {
            player.sendMessage(Component.text("This block cannot be used to create a wireless group!", NamedTextColor.RED));
            player.sendMessage(Component.text("Valid blocks: Copper Bulbs, Redstone Lamps, Chests, Shulker Boxes", NamedTextColor.GRAY));
        }
    }

    private void createBulbGroupFromTool(Player player, Location location, Block block, String groupName, 
                                          BulbVariant.BulbType bulbType, ItemStack tool) {
        // Check if block is already in a group
        if (bulbManager.isWirelessBulbLocation(location)) {
            player.sendMessage(Component.text("This block is already part of a wireless group!", NamedTextColor.RED));
            return;
        }

        // Create a new group
        UUID groupId = bulbManager.createNewGroupId();
        bulbManager.preRegisterGroup(groupId, 1, player.getUniqueId(), bulbType, groupName, null);
        
        // Register the bulb at slot 0
        bulbManager.registerPlacedBulb(location, groupId, 0, player.getUniqueId(), bulbType, 1);

        // Transform the tool from creation mode to regular mode
        ItemStack newTool = ConnectorToolFactory.createConnectorTool(groupId, groupName, ConnectorToolFactory.GroupType.BULB);
        player.getInventory().setItemInMainHand(newTool);

        ParticleEffects.spawnConnectParticles(location);
        player.sendMessage(Component.text("✓ Created group ", NamedTextColor.GREEN)
                .append(Component.text(groupName, NamedTextColor.AQUA))
                .append(Component.text(" with first ", NamedTextColor.GREEN))
                .append(Component.text(bulbType == BulbVariant.BulbType.REDSTONE_LAMP ? "lamp" : "bulb", NamedTextColor.YELLOW))
                .append(Component.text(" at slot A (1/1)", NamedTextColor.GRAY)));
    }

    private void createChestGroupFromTool(Player player, Location location, Block block, String groupName,
                                           ChestVariant.ContainerType containerType, ItemStack tool) {
        // Check if block is already in a group
        if (chestManager.isWirelessChestLocation(location)) {
            player.sendMessage(Component.text("This container is already part of a wireless group!", NamedTextColor.RED));
            return;
        }

        // Create a new group
        UUID groupId = chestManager.createNewGroupId();
        chestManager.preRegisterGroup(groupId, 1, player.getUniqueId(), containerType, groupName, null);
        
        // Register the chest at slot 0
        chestManager.registerPlacedChest(location, groupId, 0, player.getUniqueId(), 1, containerType);

        // Transform the tool from creation mode to regular mode
        ItemStack newTool = ConnectorToolFactory.createConnectorTool(groupId, groupName, ConnectorToolFactory.GroupType.CHEST);
        player.getInventory().setItemInMainHand(newTool);

        ParticleEffects.spawnConnectParticles(location);
        player.sendMessage(Component.text("✓ Created group ", NamedTextColor.GREEN)
                .append(Component.text(groupName, NamedTextColor.GOLD))
                .append(Component.text(" with first ", NamedTextColor.GREEN))
                .append(Component.text("container", NamedTextColor.YELLOW))
                .append(Component.text(" at slot A (1/1)", NamedTextColor.GRAY)));
    }

    private void handleAddBlock(Player player, Location location, Block block, UUID groupId, ConnectorToolFactory.GroupType groupType) {
        if (groupType == ConnectorToolFactory.GroupType.BULB) {
            addBulbToGroup(player, location, block, groupId);
        } else {
            addChestToGroup(player, location, block, groupId);
        }
    }

    private void handleRemoveBlock(Player player, Location location, UUID groupId, ConnectorToolFactory.GroupType groupType) {
        if (groupType == ConnectorToolFactory.GroupType.BULB) {
            removeBulbFromGroup(player, location, groupId);
        } else {
            removeChestFromGroup(player, location, groupId);
        }
    }

    private void addBulbToGroup(Player player, Location location, Block block, UUID groupId) {
        // Check if block is already in a group
        if (bulbManager.isWirelessBulbLocation(location)) {
            player.sendMessage(Component.text("This block is already part of a wireless group!", NamedTextColor.RED));
            return;
        }

        // Verify it's a valid bulb/lamp type
        Material material = block.getType();
        BulbVariant.BulbType bulbType = BulbUtils.getBulbTypeFromMaterial(material);
        if (bulbType == null) {
            player.sendMessage(Component.text("This block cannot be added to a bulb/lamp group!", NamedTextColor.RED));
            player.sendMessage(Component.text("Valid blocks: Copper Bulbs, Redstone Lamps", NamedTextColor.GRAY));
            return;
        }

        // Get the group
        Optional<BulbGroup> groupOpt = bulbManager.getGroupById(groupId);
        if (groupOpt.isEmpty()) {
            player.sendMessage(Component.text("The target group no longer exists!", NamedTextColor.RED));
            return;
        }

        BulbGroup group = groupOpt.get();

        // Check if the bulb type matches the group
        if (group.getBulbType() != bulbType) {
            player.sendMessage(Component.text("This block type doesn't match the group!", NamedTextColor.RED));
            player.sendMessage(Component.text("Group uses: " + group.getBulbType().name(), NamedTextColor.GRAY));
            return;
        }

        // Find the first available slot
        int slot = -1;
        for (int i = 0; i < group.getMaxSize(); i++) {
            if (group.getLocation(i) == null) {
                slot = i;
                break;
            }
        }

        // If group is full, auto-extend it
        if (slot == -1) {
            if (group.getMaxSize() >= 26) {
                player.sendMessage(Component.text("This group has reached the maximum size (26)!", NamedTextColor.RED));
                return;
            }
            group.extendGroup(1);
            slot = group.getMaxSize() - 1;
            player.sendMessage(Component.text("Group extended to " + group.getMaxSize() + " slots.", NamedTextColor.GRAY));
        }

        // Register the bulb
        bulbManager.registerPlacedBulb(location, groupId, slot, player.getUniqueId(), bulbType, group.getMaxSize());

        // Sync the state
        boolean isLit = group.isLit();
        if (block.getBlockData() instanceof CopperBulb copperBulb) {
            copperBulb.setLit(isLit);
            block.setBlockData(copperBulb);
        } else if (material == Material.REDSTONE_LAMP) {
            // For redstone lamps, we need to use powered state
            org.bukkit.block.data.Lightable lightable = (org.bukkit.block.data.Lightable) block.getBlockData();
            lightable.setLit(isLit);
            block.setBlockData(lightable);
        }

        char slotLabel = (char) ('A' + slot);
        ParticleEffects.spawnConnectParticles(location);
        player.sendMessage(Component.text("✓ Added block to group ", NamedTextColor.GREEN)
                .append(Component.text(group.getDisplayName(), NamedTextColor.AQUA))
                .append(Component.text(" at slot ", NamedTextColor.GREEN))
                .append(Component.text(String.valueOf(slotLabel), NamedTextColor.YELLOW))
                .append(Component.text(" (" + group.getPlacedCount() + "/" + group.getMaxSize() + ")", NamedTextColor.GRAY)));

        // Refresh wire view for players viewing this group
        WirelessRedstonePlugin.getInstance().getWireViewManager().refreshSingleGroupViewForGroup(groupId);
    }

    private void addChestToGroup(Player player, Location location, Block block, UUID groupId) {
        // Check if block is already in a group
        if (chestManager.isWirelessChestLocation(location)) {
            player.sendMessage(Component.text("This container is already part of a wireless group!", NamedTextColor.RED));
            return;
        }

        // Verify it's a valid container type
        Material material = block.getType();
        ChestVariant.ContainerType containerType = getContainerTypeFromMaterial(material);
        if (containerType == null) {
            player.sendMessage(Component.text("This block cannot be added to a container group!", NamedTextColor.RED));
            player.sendMessage(Component.text("Valid blocks: Chests, Shulker Boxes, Copper Chests", NamedTextColor.GRAY));
            return;
        }

        // Get the group
        Optional<ChestGroup> groupOpt = chestManager.getGroupById(groupId);
        if (groupOpt.isEmpty()) {
            player.sendMessage(Component.text("The target group no longer exists!", NamedTextColor.RED));
            return;
        }

        ChestGroup group = groupOpt.get();

        // Check if the container type matches the group
        if (group.getContainerType() != containerType) {
            player.sendMessage(Component.text("This container type doesn't match the group!", NamedTextColor.RED));
            player.sendMessage(Component.text("Group uses: " + group.getContainerType().name(), NamedTextColor.GRAY));
            return;
        }

        // Find the first available slot
        int slot = -1;
        for (int i = 0; i < group.getMaxSize(); i++) {
            if (group.getLocation(i) == null) {
                slot = i;
                break;
            }
        }

        // If group is full, auto-extend it
        if (slot == -1) {
            if (group.getMaxSize() >= 26) {
                player.sendMessage(Component.text("This group has reached the maximum size (26)!", NamedTextColor.RED));
                return;
            }
            group.extendGroup(1);
            slot = group.getMaxSize() - 1;
            player.sendMessage(Component.text("Group extended to " + group.getMaxSize() + " slots.", NamedTextColor.GRAY));
        }

        // Register the chest
        chestManager.registerPlacedChest(location, groupId, slot, player.getUniqueId(), group.getMaxSize(), containerType);

        // Sync the inventory
        var blockState = block.getState();
        org.bukkit.inventory.Inventory inventory = null;
        if (blockState instanceof org.bukkit.block.Chest chest) {
            inventory = chest.getInventory();
        } else if (blockState instanceof org.bukkit.block.ShulkerBox shulker) {
            inventory = shulker.getInventory();
        }

        if (inventory != null) {
            var sharedInventory = group.getSharedInventory();
            for (int i = 0; i < Math.min(sharedInventory.length, inventory.getSize()); i++) {
                inventory.setItem(i, sharedInventory[i] != null ? sharedInventory[i].clone() : null);
            }
        }

        char slotLabel = (char) ('A' + slot);
        ParticleEffects.spawnConnectParticles(location);
        player.sendMessage(Component.text("✓ Added container to group ", NamedTextColor.GREEN)
                .append(Component.text(group.getDisplayName(), NamedTextColor.GOLD))
                .append(Component.text(" at slot ", NamedTextColor.GREEN))
                .append(Component.text(String.valueOf(slotLabel), NamedTextColor.YELLOW))
                .append(Component.text(" (" + group.getPlacedCount() + "/" + group.getMaxSize() + ")", NamedTextColor.GRAY)));

        // Refresh wire view for players viewing this group
        WirelessRedstonePlugin.getInstance().getWireViewManager().refreshSingleGroupViewForGroup(groupId);
    }

    private void removeBulbFromGroup(Player player, Location location, UUID groupId) {
        // Check if this location is part of the group
        Optional<BulbGroup> groupOpt = bulbManager.getGroupByLocation(location);
        if (groupOpt.isEmpty()) {
            player.sendMessage(Component.text("This block is not part of any wireless group!", NamedTextColor.RED));
            return;
        }

        BulbGroup group = groupOpt.get();
        if (!group.getGroupId().equals(groupId)) {
            player.sendMessage(Component.text("This block is not part of the selected group!", NamedTextColor.RED));
            player.sendMessage(Component.text("It belongs to: " + group.getDisplayName(), NamedTextColor.GRAY));
            return;
        }

        int slot = group.getLocationIndex(location);
        char slotLabel = slot >= 0 ? (char) ('A' + slot) : '?';

        // Unregister the bulb (clears the slot)
        bulbManager.unregisterBulb(location);
        
        // Shrink the group by removing the now-empty slot
        if (slot >= 0 && group.getMaxSize() > 1) {
            group.removeSlot(slot);
            bulbManager.saveData();
        }

        ParticleEffects.spawnDisconnectParticles(location);
        player.sendMessage(Component.text("✓ Removed block from group ", NamedTextColor.GREEN)
                .append(Component.text(group.getDisplayName(), NamedTextColor.AQUA))
                .append(Component.text(" (was slot ", NamedTextColor.GREEN))
                .append(Component.text(String.valueOf(slotLabel), NamedTextColor.YELLOW))
                .append(Component.text(", group now has " + group.getMaxSize() + " slots)", NamedTextColor.GRAY)));

        // Refresh wire view for players viewing this group
        WirelessRedstonePlugin.getInstance().getWireViewManager().refreshSingleGroupViewForGroup(groupId);
    }

    private void removeChestFromGroup(Player player, Location location, UUID groupId) {
        // Check if this location is part of the group
        Optional<ChestGroup> groupOpt = chestManager.getGroupByLocation(location);
        if (groupOpt.isEmpty()) {
            player.sendMessage(Component.text("This container is not part of any wireless group!", NamedTextColor.RED));
            return;
        }

        ChestGroup group = groupOpt.get();
        if (!group.getGroupId().equals(groupId)) {
            player.sendMessage(Component.text("This container is not part of the selected group!", NamedTextColor.RED));
            player.sendMessage(Component.text("It belongs to: " + group.getDisplayName(), NamedTextColor.GRAY));
            return;
        }

        int slot = group.getLocationIndex(location);
        char slotLabel = slot >= 0 ? (char) ('A' + slot) : '?';

        // Unregister the chest (clears the slot)
        chestManager.unregisterChest(location);
        
        // Shrink the group by removing the now-empty slot
        if (slot >= 0 && group.getMaxSize() > 1) {
            group.removeSlot(slot);
            chestManager.saveData();
        }

        ParticleEffects.spawnDisconnectParticles(location);
        player.sendMessage(Component.text("✓ Removed container from group ", NamedTextColor.GREEN)
                .append(Component.text(group.getDisplayName(), NamedTextColor.GOLD))
                .append(Component.text(" (was slot ", NamedTextColor.GREEN))
                .append(Component.text(String.valueOf(slotLabel), NamedTextColor.YELLOW))
                .append(Component.text(", group now has " + group.getMaxSize() + " slots)", NamedTextColor.GRAY)));

        // Refresh wire view for players viewing this group
        WirelessRedstonePlugin.getInstance().getWireViewManager().refreshSingleGroupViewForGroup(groupId);
    }

    private ChestVariant.ContainerType getContainerTypeFromMaterial(Material material) {
        return switch (material) {
            case CHEST, TRAPPED_CHEST -> ChestVariant.ContainerType.CHEST;
            case SHULKER_BOX, WHITE_SHULKER_BOX, ORANGE_SHULKER_BOX, MAGENTA_SHULKER_BOX,
                 LIGHT_BLUE_SHULKER_BOX, YELLOW_SHULKER_BOX, LIME_SHULKER_BOX, PINK_SHULKER_BOX,
                 GRAY_SHULKER_BOX, LIGHT_GRAY_SHULKER_BOX, CYAN_SHULKER_BOX, PURPLE_SHULKER_BOX,
                 BLUE_SHULKER_BOX, BROWN_SHULKER_BOX, GREEN_SHULKER_BOX, RED_SHULKER_BOX,
                 BLACK_SHULKER_BOX -> ChestVariant.ContainerType.SHULKER;
            default -> null;
        };
    }
}
