package com.wirelessredstone.listener;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.util.ParticleEffects;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ChestInventoryListener implements Listener {

    private final LinkedChestManager chestManager;

    public ChestInventoryListener(LinkedChestManager chestManager) {
        this.chestManager = chestManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        Inventory inventory = event.getInventory();
        Location location = getChestLocation(inventory);
        
        if (location == null || !chestManager.isWirelessChestLocation(location)) {
            return;
        }
        
        // Sync the shared inventory TO this container when opened
        // This ensures containers in previously unloaded chunks get updated
        var groupOpt = chestManager.getGroupByLocation(location);
        if (groupOpt.isEmpty()) return;
        
        chestManager.applySharedInventory(location, groupOpt.get());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        Location location = getChestLocation(inventory);
        
        if (location == null || !chestManager.isWirelessChestLocation(location)) {
            return;
        }
        
        // Schedule sync after the click is processed
        WirelessRedstonePlugin.getInstance().getServer().getScheduler().runTaskLater(
            WirelessRedstonePlugin.getInstance(),
            () -> syncChestInventory(inventory, location),
            1L
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        Location location = getChestLocation(inventory);
        
        if (location == null || !chestManager.isWirelessChestLocation(location)) {
            return;
        }
        
        // Schedule sync after the drag is processed
        WirelessRedstonePlugin.getInstance().getServer().getScheduler().runTaskLater(
            WirelessRedstonePlugin.getInstance(),
            () -> syncChestInventory(inventory, location),
            1L
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        Location location = getChestLocation(inventory);
        
        if (location == null || !chestManager.isWirelessChestLocation(location)) {
            return;
        }
        
        syncChestInventory(inventory, location);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        // Handle hopper interactions
        Location sourceLocation = getChestLocation(event.getSource());
        Location destLocation = getChestLocation(event.getDestination());
        
        if (sourceLocation != null && chestManager.isWirelessChestLocation(sourceLocation)) {
            WirelessRedstonePlugin.getInstance().getServer().getScheduler().runTaskLater(
                WirelessRedstonePlugin.getInstance(),
                () -> syncChestInventory(event.getSource(), sourceLocation),
                1L
            );
        }
        
        if (destLocation != null && chestManager.isWirelessChestLocation(destLocation)) {
            WirelessRedstonePlugin.getInstance().getServer().getScheduler().runTaskLater(
                WirelessRedstonePlugin.getInstance(),
                () -> syncChestInventory(event.getDestination(), destLocation),
                1L
            );
        }
    }

    private Location getChestLocation(Inventory inventory) {
        if (inventory == null) return null;
        
        var holder = inventory.getHolder();
        
        // Handle all single-block containers (chest, shulker box, copper chest, etc.)
        if (holder instanceof org.bukkit.block.Container container) {
            return container.getLocation();
        }
        
        // Handle double chests
        if (holder instanceof org.bukkit.block.DoubleChest doubleChest) {
            var leftSide = doubleChest.getLeftSide();
            if (leftSide instanceof org.bukkit.block.Container container) {
                Location loc = container.getLocation();
                if (chestManager.isWirelessChestLocation(loc)) {
                    return loc;
                }
            }
            var rightSide = doubleChest.getRightSide();
            if (rightSide instanceof org.bukkit.block.Container container) {
                Location loc = container.getLocation();
                if (chestManager.isWirelessChestLocation(loc)) {
                    return loc;
                }
            }
        }
        
        return null;
    }

    private void syncChestInventory(Inventory inventory, Location location) {
        var groupOpt = chestManager.getGroupByLocation(location);
        if (groupOpt.isEmpty()) return;
        
        var contents = new ItemStack[inventory.getSize()];
        for (int i = 0; i < inventory.getSize(); i++) {
            var item = inventory.getItem(i);
            contents[i] = item != null ? item.clone() : null;
        }
        
        chestManager.syncInventoryToGroup(location, contents);
        
        // Visual feedback
        ParticleEffects.spawnSyncParticles(location, true);
        for (Location otherLoc : groupOpt.get().getOtherLocations(location)) {
            if (otherLoc.isChunkLoaded()) {
                ParticleEffects.spawnSyncParticles(otherLoc, true);
            }
        }
    }
}
