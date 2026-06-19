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
import org.bukkit.block.Chest;
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

public class ChestBreakListener implements Listener {

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

        if (doubleChestBreak != null) {
            splitSharedInventoryForVanillaDrop(groupOpt.get(), doubleChestBreak);
        } else if (remainingCount > 0) {
            clearContainerInventory(block);
        }

        ParticleEffects.spawnBreakParticles(location);

        chestManager.unregisterChest(location);
        if (doubleChestBreak != null) {
            chestManager.unregisterChest(doubleChestBreak.otherLocation());
        }

        if (remainingCount <= 0) {
            player.sendMessage(Component.text("⚡ ", NamedTextColor.YELLOW)
                    .append(Component.text("Group ", NamedTextColor.GRAY))
                    .append(Component.text(groupName, NamedTextColor.GOLD))
                    .append(Component.text(" has been removed (last container broken)", NamedTextColor.GRAY)));
        } else {
            player.sendMessage(Component.text("⚡ ", NamedTextColor.YELLOW)
                    .append(Component.text("Removed from group ", NamedTextColor.GRAY))
                    .append(Component.text(groupName, NamedTextColor.GOLD))
                    .append(Component.text(" (" + remainingCount + " remaining)", NamedTextColor.DARK_GRAY)));
            if (doubleChestBreak != null) {
                player.sendMessage(Component.text("The remaining half of this double chest was detached from the wireless group.", NamedTextColor.GRAY));
            }
        }

        WirelessRedstonePlugin plugin = WirelessRedstonePlugin.getInstance();
        plugin.getWireViewManager().refreshAllPlayers();
        if (groupId != null) {
            plugin.getWireViewManager().refreshSingleGroupViewForGroup(groupId);
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
        if (!(state instanceof Chest currentChest)) {
            return Optional.empty();
        }

        Inventory inventory = currentChest.getInventory();
        if (!(inventory.getHolder() instanceof DoubleChest doubleChest)) {
            return Optional.empty();
        }

        Chest otherChest = null;
        var leftSide = doubleChest.getLeftSide();
        var rightSide = doubleChest.getRightSide();

        if (leftSide instanceof Chest leftChest && !leftChest.getLocation().equals(location)) {
            otherChest = leftChest;
        } else if (rightSide instanceof Chest rightChest && !rightChest.getLocation().equals(location)) {
            otherChest = rightChest;
        }

        if (otherChest == null || !group.hasLocation(otherChest.getLocation())) {
            return Optional.empty();
        }

        return Optional.of(new DoubleChestBreak(otherChest.getLocation(), currentChest, otherChest));
    }

    private void splitSharedInventoryForVanillaDrop(ChestGroup group, DoubleChestBreak doubleChestBreak) {
        Inventory brokenHalf = doubleChestBreak.brokenChest().getBlockInventory();
        Inventory remainingHalf = doubleChestBreak.otherChest().getBlockInventory();
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

    private record DoubleChestBreak(Location otherLocation, Chest brokenChest, Chest otherChest) {}
}
