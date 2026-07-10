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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class ChestBreakListener extends WirelessBlockListener {

    private final LinkedChestManager chestManager;
    private static final int SINGLE_CHEST_SIZE = 27;

    public ChestBreakListener(LinkedChestManager chestManager) {
        this.chestManager = chestManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material blockType = block.getType();
        
        if (blockType != Material.CHEST && blockType != Material.BARREL && !ChestVariant.isShulkerBox(blockType) && !ChestVariant.isCopperChest(blockType)) {
            return;
        }
        
        Location location = block.getLocation();

        if (!chestManager.isWirelessChestLocation(location)) {
            return;
        }

        Player player = event.getPlayer();
        Optional<ChestGroup> groupOpt = chestManager.getGroupByLocation(location);
        String groupName = groupOpt.map(ChestGroup::getDisplayName).orElse("Unknown");
        UUID groupId = groupOpt.map(ChestGroup::getGroupId).orElse(null);
        DoubleChestBreak doubleChestBreak = groupOpt
                .flatMap(group -> findWirelessDoubleChestBreak(block, location, group))
                .orElse(null);
        int removedCount = doubleChestBreak != null ? 2 : 1;
        int remainingCount = groupOpt.map(g -> Math.max(0, g.getPlacedCount() - removedCount)).orElse(0);

        if (doubleChestBreak != null && remainingCount <= 0) {
            splitSharedInventoryForVanillaDrop(groupOpt.get(), doubleChestBreak);
        } else if (doubleChestBreak != null) {
            doubleChestBreak.brokenInventory().clear();
            doubleChestBreak.otherInventory().clear();
        } else if (remainingCount > 0) {
            clearContainerInventory(block);
        }

        chestManager.unregisterChest(location);
        if (doubleChestBreak != null) {
            chestManager.unregisterChest(doubleChestBreak.otherLocation());
        }

        finishBreak(player, location, groupOpt.orElse(null), remainingCount, "container", NamedTextColor.GOLD);
        if (remainingCount > 0) {
            if (doubleChestBreak != null) {
                player.sendMessage(Component.text("The remaining half of this double chest was detached from the wireless group.", NamedTextColor.GRAY));
            }
        }

        WirelessRedstonePlugin plugin = WirelessRedstonePlugin.getInstance();
        if (groupId != null) {
            if (remainingCount > 0) {
                plugin.getServer().getScheduler().runTask(plugin, () -> chestManager.syncGroupToPlacedContainers(groupId));
            }
        }
    }

    private void clearContainerInventory(Block block) {
        var state = block.getState();
        if (state instanceof Container container) {
            container.getInventory().clear();
        }
    }

    private Optional<DoubleChestBreak> findWirelessDoubleChestBreak(Block block, Location location, ChestGroup group) {
        var state = block.getState();
        if (!(state instanceof Container currentContainer)) {
            return Optional.empty();
        }

        Inventory inventory = currentContainer.getInventory();
        if (!(inventory.getHolder() instanceof DoubleChest doubleChest)) {
            return Optional.empty();
        }

        Container otherContainer = null;
        Inventory brokenInventory = null;
        Inventory otherInventory = null;
        var leftSide = doubleChest.getLeftSide();
        var rightSide = doubleChest.getRightSide();

        if (leftSide instanceof Container leftContainer && rightSide instanceof Container rightContainer) {
            if (leftContainer.getLocation().equals(location)) {
                otherContainer = rightContainer;
                brokenInventory = getHalfInventory(inventory, true);
                otherInventory = getHalfInventory(inventory, false);
            } else if (rightContainer.getLocation().equals(location)) {
                otherContainer = leftContainer;
                brokenInventory = getHalfInventory(inventory, false);
                otherInventory = getHalfInventory(inventory, true);
            }
        }

        if (otherContainer == null || brokenInventory == null || otherInventory == null || !group.hasLocation(otherContainer.getLocation())) {
            return Optional.empty();
        }

        return Optional.of(new DoubleChestBreak(otherContainer.getLocation(), brokenInventory, otherInventory));
    }

    private void splitSharedInventoryForVanillaDrop(ChestGroup group, DoubleChestBreak doubleChestBreak) {
        Inventory brokenHalf = doubleChestBreak.brokenInventory();
        Inventory remainingHalf = doubleChestBreak.otherInventory();
        ItemStack[] shared = group.getSharedInventory();

        brokenHalf.clear();
        remainingHalf.clear();

        for (int i = 0; i < SINGLE_CHEST_SIZE; i++) {
            if (i < shared.length && shared[i] != null) {
                remainingHalf.setItem(i, shared[i].clone());
            }

            int overflowSlot = i + SINGLE_CHEST_SIZE;
            if (overflowSlot < shared.length && shared[overflowSlot] != null) {
                brokenHalf.setItem(i, shared[overflowSlot].clone());
            }
        }
    }

    private Inventory getHalfInventory(Inventory inventory, boolean leftSide) {
        if (inventory instanceof org.bukkit.inventory.DoubleChestInventory doubleChestInventory) {
            return leftSide ? doubleChestInventory.getLeftSide() : doubleChestInventory.getRightSide();
        }

        return null;
    }

    private record DoubleChestBreak(Location otherLocation, Inventory brokenInventory, Inventory otherInventory) {}
}
