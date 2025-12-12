package com.wirelessredstone.listener;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.util.ParticleEffects;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;

public class ChestInventoryListener implements Listener {

    private final LinkedChestManager chestManager;

    public ChestInventoryListener(LinkedChestManager chestManager) {
        this.chestManager = chestManager;
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
        if (holder instanceof Chest chest) {
            return chest.getLocation();
        }
        
        // Handle double chests
        if (holder instanceof org.bukkit.block.DoubleChest doubleChest) {
            var leftSide = doubleChest.getLeftSide();
            if (leftSide instanceof Chest chest) {
                Location loc = chest.getLocation();
                if (chestManager.isWirelessChestLocation(loc)) {
                    return loc;
                }
            }
            var rightSide = doubleChest.getRightSide();
            if (rightSide instanceof Chest chest) {
                Location loc = chest.getLocation();
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
        
        // Get the contents - for single chest use first 27 slots
        var contents = new org.bukkit.inventory.ItemStack[27];
        for (int i = 0; i < Math.min(27, inventory.getSize()); i++) {
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
