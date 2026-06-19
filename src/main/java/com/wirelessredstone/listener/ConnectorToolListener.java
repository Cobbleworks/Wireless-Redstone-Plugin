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
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
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
            player.sendMessage(Component.text("Valid blocks: Copper Bulbs, Redstone Lamps, Chests, Copper Chests, Shulker Boxes, Barrels", NamedTextColor.GRAY));
        }
    }

    private void createBulbGroupFromTool(Player player, Location location, Block block, String groupName, 
                                          BulbVariant.BulbType bulbType, ItemStack tool) {
        // Check if block is already in a group
        if (bulbManager.isWirelessBulbLocation(location)) {
            player.sendMessage(Component.text("This block is already part of a wireless group!", NamedTextColor.RED));
            return;
        }

        // Get category from tool if present
        String categoryName = ConnectorToolFactory.getCategoryName(tool);
        UUID categoryId = null;
        if (categoryName != null && !categoryName.isEmpty()) {
            var categoryManager = WirelessRedstonePlugin.getInstance().getCategoryManager();
            var category = categoryManager.getCategoryByName(categoryName);
            if (category.isPresent()) {
                categoryId = category.get().getCategoryId();
            }
        }

        // Get the block material for the variant icon
        Material variantMaterial = block.getType();

        // Create a new group with the variant material for icon
        UUID groupId = bulbManager.createNewGroupId();
        bulbManager.preRegisterGroup(groupId, 1, player.getUniqueId(), bulbType, groupName, categoryId, variantMaterial);
        
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

        // Check for large (double) chest and handle both halves
        Location otherHalfLocation = getDoubleChestOtherHalf(block);
        boolean isLargeChest = otherHalfLocation != null;
        
        if (isLargeChest) {
            // Check if the other half is already in a group
            if (chestManager.isWirelessChestLocation(otherHalfLocation)) {
                player.sendMessage(Component.text("The other half of this large chest is already part of a wireless group!", NamedTextColor.RED));
                return;
            }
        }

        // For copper chests, convert both large-chest halves to waxed variants after
        // detecting the pair so waxing does not collapse the chest into a single half.
        if (containerType == ChestVariant.ContainerType.COPPER_CHEST) {
            boolean converted = convertCopperChestPairToWaxed(block, otherHalfLocation);
            if (converted) {
                player.sendMessage(Component.text("Converted copper chest to waxed variant to prevent oxidation.", NamedTextColor.GRAY));
            }
        }

        // Get the block material for the variant icon (after potential conversion)
        Material variantMaterial = block.getType();

        // Create a new group - large chests start with 2 slots
        UUID groupId = chestManager.createNewGroupId();
        int initialSize = isLargeChest ? 2 : 1;
        
        // Get category from tool if present
        String categoryName = ConnectorToolFactory.getCategoryName(tool);
        UUID categoryId = null;
        if (categoryName != null && !categoryName.isEmpty()) {
            var categoryManager = WirelessRedstonePlugin.getInstance().getCategoryManager();
            var category = categoryManager.getCategoryByName(categoryName);
            if (category.isPresent()) {
                categoryId = category.get().getCategoryId();
            }
        }
        
        // Pre-register with variant material for icon
        chestManager.preRegisterGroup(groupId, initialSize, player.getUniqueId(), containerType, groupName, categoryId, variantMaterial);
        chestManager.getGroupById(groupId).ifPresent(group ->
                group.setInventorySize(isLargeChest ? ChestGroup.LARGE_CHEST_INVENTORY_SIZE : ChestGroup.DEFAULT_INVENTORY_SIZE));
        
        // Register the chest(s)
        chestManager.registerPlacedChest(location, groupId, 0, player.getUniqueId(), initialSize, containerType);
        
        if (isLargeChest) {
            chestManager.registerPlacedChest(otherHalfLocation, groupId, 1, player.getUniqueId(), initialSize, containerType);
            ParticleEffects.spawnConnectParticles(otherHalfLocation);
        }

        // Transform the tool from creation mode to regular mode
        ItemStack newTool = ConnectorToolFactory.createConnectorTool(groupId, groupName, ConnectorToolFactory.GroupType.CHEST);
        player.getInventory().setItemInMainHand(newTool);

        ParticleEffects.spawnConnectParticles(location);
        
        String containerLabel = getContainerLabel(containerType);
        String sizeText = isLargeChest ? " (large chest with slots A & B)" : " at slot A (1/1)";
        
        player.sendMessage(Component.text("✓ Created group ", NamedTextColor.GREEN)
                .append(Component.text(groupName, NamedTextColor.GOLD))
                .append(Component.text(" with first ", NamedTextColor.GREEN))
                .append(Component.text(containerLabel, NamedTextColor.YELLOW))
                .append(Component.text(sizeText, NamedTextColor.GRAY)));
    }

    /**
     * Gets a friendly label for the container type.
     */
    private String getContainerLabel(ChestVariant.ContainerType containerType) {
        return switch (containerType) {
            case CHEST -> "chest";
            case SHULKER -> "shulker box";
            case COPPER_CHEST -> "copper chest";
            case BARREL -> "barrel";
        };
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
        Optional<ChestGroup> existingGroupOpt = chestManager.getGroupByLocation(location);
        if (existingGroupOpt.isPresent()) {
            ChestGroup existingGroup = existingGroupOpt.get();
            if (existingGroup.getGroupId().equals(groupId)) {
                removeChestFromGroup(player, location, groupId);
                return;
            }

            player.sendMessage(Component.text("This container is already part of a different wireless group!", NamedTextColor.RED));
            player.sendMessage(Component.text("It belongs to: " + existingGroup.getDisplayName(), NamedTextColor.GRAY));
            return;
        }

        // Verify it's a valid container type
        Material material = block.getType();
        ChestVariant.ContainerType containerType = getContainerTypeFromMaterial(material);
        if (containerType == null) {
            player.sendMessage(Component.text("This block cannot be added to a container group!", NamedTextColor.RED));
            player.sendMessage(Component.text("Valid blocks: Chests, Shulker Boxes, Copper Chests, Barrels", NamedTextColor.GRAY));
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

        // Check for large (double) chest and handle both halves
        Location otherHalfLocation = getDoubleChestOtherHalf(block);
        boolean isLargeChest = otherHalfLocation != null;
        
        if (isLargeChest) {
            // Check if the other half is already in a group
            if (chestManager.isWirelessChestLocation(otherHalfLocation)) {
                player.sendMessage(Component.text("The other half of this large chest is already part of a wireless group!", NamedTextColor.RED));
                return;
            }
        }

        // For copper chests, convert both large-chest halves to waxed variants after
        // detecting the pair so waxing does not collapse the chest into a single half.
        if (containerType == ChestVariant.ContainerType.COPPER_CHEST) {
            boolean converted = convertCopperChestPairToWaxed(block, otherHalfLocation);
            if (converted) {
                player.sendMessage(Component.text("Converted copper chest to waxed variant to prevent oxidation.", NamedTextColor.GRAY));
            }
        }

        int containerInventorySize = isLargeChest ? ChestGroup.LARGE_CHEST_INVENTORY_SIZE : ChestGroup.DEFAULT_INVENTORY_SIZE;
        if (group.getPlacedCount() == 0) {
            group.setInventorySize(containerInventorySize);
        } else if (group.getInventorySize() != containerInventorySize) {
            player.sendMessage(Component.text("This container has a different inventory size than the group!", NamedTextColor.RED));
            player.sendMessage(Component.text("Single chests and large chests must be kept in separate wireless groups.", NamedTextColor.GRAY));
            return;
        }

        // Calculate how many slots we need (1 or 2 for large chest)
        int slotsNeeded = isLargeChest ? 2 : 1;
        
        // Find available slots
        int slot = -1;
        int slot2 = -1;
        for (int i = 0; i < group.getMaxSize(); i++) {
            if (group.getLocation(i) == null) {
                if (slot == -1) {
                    slot = i;
                    if (!isLargeChest) break;
                } else if (slot2 == -1) {
                    slot2 = i;
                    break;
                }
            }
        }

        // Count available slots
        int availableSlots = (slot != -1 ? 1 : 0) + (slot2 != -1 ? 1 : 0);
        int slotsToExtend = slotsNeeded - availableSlots;
        
        // If we need more slots, auto-extend
        if (slotsToExtend > 0) {
            if (group.getMaxSize() + slotsToExtend > 26) {
                player.sendMessage(Component.text("This group has reached the maximum size (26)!", NamedTextColor.RED));
                return;
            }
            int oldSize = group.getMaxSize();
            group.extendGroup(slotsToExtend);
            
            // Fill in the newly created slots
            if (slot == -1) {
                slot = oldSize;
            }
            if (isLargeChest && slot2 == -1) {
                slot2 = (slot == oldSize) ? oldSize + 1 : oldSize;
            }
            
            player.sendMessage(Component.text("Group extended to " + group.getMaxSize() + " slots.", NamedTextColor.GRAY));
        }

        // Register the chest(s)
        chestManager.registerPlacedChest(location, groupId, slot, player.getUniqueId(), group.getMaxSize(), containerType);
        
        if (isLargeChest && slot2 != -1) {
            chestManager.registerPlacedChest(otherHalfLocation, groupId, slot2, player.getUniqueId(), group.getMaxSize(), containerType);
            ParticleEffects.spawnConnectParticles(otherHalfLocation);
        }

        chestManager.applySharedInventory(location, group);

        char slotLabel = (char) ('A' + slot);
        ParticleEffects.spawnConnectParticles(location);
        
        if (isLargeChest && slot2 != -1) {
            char slot2Label = (char) ('A' + slot2);
            player.sendMessage(Component.text("✓ Added large chest to group ", NamedTextColor.GREEN)
                    .append(Component.text(group.getDisplayName(), NamedTextColor.GOLD))
                    .append(Component.text(" at slots ", NamedTextColor.GREEN))
                    .append(Component.text(slotLabel + " & " + slot2Label, NamedTextColor.YELLOW))
                    .append(Component.text(" (" + group.getPlacedCount() + "/" + group.getMaxSize() + ")", NamedTextColor.GRAY)));
        } else {
            player.sendMessage(Component.text("✓ Added container to group ", NamedTextColor.GREEN)
                    .append(Component.text(group.getDisplayName(), NamedTextColor.GOLD))
                    .append(Component.text(" at slot ", NamedTextColor.GREEN))
                    .append(Component.text(String.valueOf(slotLabel), NamedTextColor.YELLOW))
                    .append(Component.text(" (" + group.getPlacedCount() + "/" + group.getMaxSize() + ")", NamedTextColor.GRAY)));
        }

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

        Location otherHalfLocation = getDoubleChestOtherHalf(location.getBlock());
        boolean isLargeChest = otherHalfLocation != null && group.hasLocation(otherHalfLocation);
        int slot = group.getLocationIndex(location);
        int slot2 = isLargeChest ? group.getLocationIndex(otherHalfLocation) : -1;
        String slotText = formatSlotText(slot, slot2);

        chestManager.applySharedInventory(location, group);
        chestManager.unregisterChest(location);
        if (isLargeChest) {
            chestManager.unregisterChest(otherHalfLocation);
        }
        
        removeSlots(group, slot, slot2);
        chestManager.saveData();

        ParticleEffects.spawnDisconnectParticles(location);
        if (isLargeChest) {
            ParticleEffects.spawnDisconnectParticles(otherHalfLocation);
        }
        player.sendMessage(Component.text("✓ Removed container from group ", NamedTextColor.GREEN)
                .append(Component.text(group.getDisplayName(), NamedTextColor.GOLD))
                .append(Component.text(" (was slot ", NamedTextColor.GREEN))
                .append(Component.text(slotText, NamedTextColor.YELLOW))
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
            case COPPER_CHEST, EXPOSED_COPPER_CHEST, WEATHERED_COPPER_CHEST, OXIDIZED_COPPER_CHEST,
                 WAXED_COPPER_CHEST, WAXED_EXPOSED_COPPER_CHEST, WAXED_WEATHERED_COPPER_CHEST,
                 WAXED_OXIDIZED_COPPER_CHEST -> ChestVariant.ContainerType.COPPER_CHEST;
            case BARREL -> ChestVariant.ContainerType.BARREL;
            default -> null;
        };
    }

    /**
     * Converts regular copper chest to waxed variant and returns the waxed Material.
     * Returns the original material if already waxed or not a copper chest.
     */
    private Material convertToWaxedCopperIfNeeded(Block block) {
        Material material = block.getType();
        Material waxedMaterial = ChestVariant.toWaxedCopperChest(material);
        if (waxedMaterial != material) {
            var oldBlockData = block.getBlockData();
            block.setType(waxedMaterial, false);

            if (oldBlockData instanceof org.bukkit.block.data.Directional oldDirectional) {
                var newBlockData = block.getBlockData();
                if (newBlockData instanceof org.bukkit.block.data.Directional newDirectional) {
                    newDirectional.setFacing(oldDirectional.getFacing());
                }
                if (oldBlockData instanceof org.bukkit.block.data.type.Chest oldChest &&
                    newBlockData instanceof org.bukkit.block.data.type.Chest newChest) {
                    newChest.setType(oldChest.getType());
                }
                if (oldBlockData instanceof org.bukkit.block.data.Waterlogged oldWaterlogged &&
                    newBlockData instanceof org.bukkit.block.data.Waterlogged newWaterlogged) {
                    newWaterlogged.setWaterlogged(oldWaterlogged.isWaterlogged());
                }
                block.setBlockData(newBlockData, false);
            }
        }
        return waxedMaterial;
    }

    private boolean convertCopperChestPairToWaxed(Block block, Location otherHalfLocation) {
        Material originalMaterial = block.getType();
        Material waxedMaterial = convertToWaxedCopperIfNeeded(block);
        boolean converted = waxedMaterial != originalMaterial;

        if (otherHalfLocation != null) {
            Block otherHalf = otherHalfLocation.getBlock();
            Material otherOriginalMaterial = otherHalf.getType();
            Material otherWaxedMaterial = convertToWaxedCopperIfNeeded(otherHalf);
            converted = converted || otherWaxedMaterial != otherOriginalMaterial;
        }

        return converted;
    }

    /**
     * Gets the location of the other half of a double chest, if applicable.
     * Returns null if not a double chest.
     */
    private Location getDoubleChestOtherHalf(Block block) {
        var blockState = block.getState();
        if (blockState instanceof Container container) {
            var inventory = container.getInventory();
            var holder = inventory.getHolder();
            if (holder instanceof org.bukkit.block.DoubleChest doubleChest) {
                var leftSide = doubleChest.getLeftSide();
                var rightSide = doubleChest.getRightSide();
                
                if (leftSide instanceof org.bukkit.block.Container leftContainer &&
                    rightSide instanceof org.bukkit.block.Container rightContainer) {
                    Location leftLoc = leftContainer.getLocation();
                    Location rightLoc = rightContainer.getLocation();
                    Location currentLoc = block.getLocation();
                    
                    // Return the other half's location
                    if (leftLoc.equals(currentLoc)) {
                        return rightLoc;
                    } else {
                        return leftLoc;
                    }
                }
            }
        }

        if (block.getBlockData() instanceof org.bukkit.block.data.type.Chest chestData &&
            chestData.getType() != org.bukkit.block.data.type.Chest.Type.SINGLE) {
            BlockFace otherHalfFace = getOtherChestHalfFace(chestData);
            if (otherHalfFace == null) {
                return null;
            }

            Location detectedLocation = findMatchingChestHalfLocation(block, chestData, otherHalfFace);
            if (detectedLocation != null) {
                return detectedLocation;
            }
        }

        return null;
    }

    private BlockFace getOtherChestHalfFace(org.bukkit.block.data.type.Chest chestData) {
        return switch (chestData.getType()) {
            case LEFT -> rotateClockwise(chestData.getFacing());
            case RIGHT -> rotateCounterClockwise(chestData.getFacing());
            case SINGLE -> null;
        };
    }

    private boolean isMatchingChestHalf(Block block, Block otherHalf, org.bukkit.block.data.type.Chest chestData) {
        if (getContainerTypeFromMaterial(otherHalf.getType()) != getContainerTypeFromMaterial(block.getType())) {
            return false;
        }
        if (!(otherHalf.getBlockData() instanceof org.bukkit.block.data.type.Chest otherChestData)) {
            return false;
        }

        return otherChestData.getFacing() == chestData.getFacing()
                && otherChestData.getType() != org.bukkit.block.data.type.Chest.Type.SINGLE
                && otherChestData.getType() != chestData.getType();
    }

    private Location findMatchingChestHalfLocation(Block block, org.bukkit.block.data.type.Chest chestData, BlockFace preferredFace) {
        BlockFace[] faces = {
                preferredFace,
                BlockFace.NORTH,
                BlockFace.EAST,
                BlockFace.SOUTH,
                BlockFace.WEST
        };

        for (BlockFace face : faces) {
            if (face == null) continue;

            Block otherHalf = block.getRelative(face);
            if (isMatchingChestHalf(block, otherHalf, chestData)) {
                return otherHalf.getLocation();
            }
        }

        return null;
    }

    private BlockFace rotateClockwise(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> null;
        };
    }

    private BlockFace rotateCounterClockwise(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            default -> null;
        };
    }

    private String formatSlotText(int slot, int slot2) {
        String first = slot >= 0 ? ChestGroup.getIndexLabel(slot) : "?";
        if (slot2 < 0) {
            return first;
        }

        return first + " & " + ChestGroup.getIndexLabel(slot2);
    }

    private void removeSlots(ChestGroup group, int slot, int slot2) {
        int first = Math.max(slot, slot2);
        int second = Math.min(slot, slot2);

        if (first >= 0 && group.getMaxSize() > 1) {
            group.removeSlot(first);
        }
        if (second >= 0 && group.getMaxSize() > 1) {
            group.removeSlot(second);
        }
    }
}
