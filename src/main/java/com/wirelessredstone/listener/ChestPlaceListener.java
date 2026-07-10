package com.wirelessredstone.listener;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.item.ChestVariant;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.model.ChestGroup;
import com.wirelessredstone.util.ParticleEffects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.List;
import java.util.UUID;

public class ChestPlaceListener extends WirelessBlockListener {

    private final LinkedChestManager chestManager;

    public ChestPlaceListener(LinkedChestManager chestManager) {
        this.chestManager = chestManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Material blockType = event.getBlock().getType();
        if (blockType != Material.CHEST && blockType != Material.BARREL && !ChestVariant.isShulkerBox(blockType) && !ChestVariant.isCopperChest(blockType)) {
            return;
        }

        Location otherHalfLocation = getDoubleChestOtherHalf(event.getBlock());
        if (otherHalfLocation != null && chestManager.isWirelessChestLocation(otherHalfLocation)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("Wireless single chests cannot be merged into large chests.", NamedTextColor.RED));
            event.getPlayer().sendMessage(Component.text("Create a separate large-chest group with the circuit tool instead.", NamedTextColor.GRAY));
            return;
        }
        
        var itemInHand = event.getItemInHand();

        if (!chestManager.isWirelessChest(itemInHand)) {
            return;
        }

        var groupIdOpt = chestManager.getGroupId(itemInHand);
        var chestIndexOpt = chestManager.getChestIndex(itemInHand);

        if (groupIdOpt.isEmpty() || chestIndexOpt.isEmpty()) {
            return;
        }

        UUID ownerUuid = chestManager.getOwnerUuid(itemInHand).orElse(event.getPlayer().getUniqueId());
        int groupSize = chestManager.getGroupSize(itemInHand).orElse(2);
        ChestVariant.ContainerType containerType = chestManager.getContainerType(itemInHand).orElse(ChestVariant.ContainerType.CHEST);

        var location = event.getBlock().getLocation();
        boolean isLargeChest = otherHalfLocation != null;
        int containerInventorySize = isLargeChest ? ChestGroup.LARGE_CHEST_INVENTORY_SIZE : ChestGroup.DEFAULT_INVENTORY_SIZE;

        if (isLargeChest) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("Wireless chest items cannot be placed as part of a large chest.", NamedTextColor.RED));
            event.getPlayer().sendMessage(Component.text("Use the circuit tool on an existing large chest to create a large-chest group.", NamedTextColor.GRAY));
            return;
        }

        var groupOpt = chestManager.getGroupById(groupIdOpt.get());
        if (groupOpt.isPresent()) {
            ChestGroup group = groupOpt.get();
            if (group.getPlacedCount() > 0 && group.getInventorySize() != containerInventorySize) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Component.text("This placement would mix single and large chests in one wireless group.", NamedTextColor.RED));
                event.getPlayer().sendMessage(Component.text("Use a separate group for large chests.", NamedTextColor.GRAY));
                return;
            }
            if (group.getPlacedCount() == 0) {
                group.setInventorySize(containerInventorySize);
            }
        }

        chestManager.registerPlacedChest(location, groupIdOpt.get(), chestIndexOpt.get(), ownerUuid, groupSize, containerType);

        finishPlacement(chestManager, groupIdOpt.get(), location,
                group -> chestManager.applySharedInventory(location, group));
    }

    private Location getDoubleChestOtherHalf(Block block) {
        var blockState = block.getState();
        if (blockState instanceof Container container) {
            var inventory = container.getInventory();
            var holder = inventory.getHolder();
            if (holder instanceof DoubleChest doubleChest) {
                var leftSide = doubleChest.getLeftSide();
                var rightSide = doubleChest.getRightSide();

                if (leftSide instanceof org.bukkit.block.Container leftContainer &&
                    rightSide instanceof org.bukkit.block.Container rightContainer) {
                    Location leftLoc = leftContainer.getLocation();
                    Location rightLoc = rightContainer.getLocation();
                    Location currentLoc = block.getLocation();

                    if (leftLoc.equals(currentLoc)) {
                        return rightLoc;
                    } else {
                        return leftLoc;
                    }
                }
            }
        }
        return null;
    }
}
