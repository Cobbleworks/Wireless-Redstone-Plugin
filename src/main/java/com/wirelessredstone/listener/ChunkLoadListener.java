package com.wirelessredstone.listener;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.item.ChestVariant;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.model.ChestGroup;
import com.wirelessredstone.util.BulbUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.CopperBulb;
import org.bukkit.block.data.Lightable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles chunk load events to sync wireless blocks when their chunks are loaded.
 * This ensures bulbs and containers in previously unloaded chunks get the correct state.
 */
public class ChunkLoadListener implements Listener {

    private final LinkedBulbManager bulbManager;
    private final LinkedChestManager chestManager;

    public ChunkLoadListener(LinkedBulbManager bulbManager, LinkedChestManager chestManager) {
        this.bulbManager = bulbManager;
        this.chestManager = chestManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        var chunk = event.getChunk();
        var world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        // Sync bulbs in this chunk
        for (BulbGroup group : bulbManager.getAllGroups()) {
            for (Location loc : group.getPlacedLocations()) {
                if (loc == null || !loc.getWorld().equals(world)) continue;
                
                // Check if this location is in the loaded chunk
                int locChunkX = loc.getBlockX() >> 4;
                int locChunkZ = loc.getBlockZ() >> 4;
                
                if (locChunkX == chunkX && locChunkZ == chunkZ) {
                    if (BulbUtils.getBulbTypeFromMaterial(loc.getBlock().getType()) == group.getBulbType()) {
                        syncBulbToGroupState(loc, group);
                    } else {
                        bulbManager.unregisterBulb(loc);
                    }
                }
            }
        }

        // Sync containers in this chunk
        for (ChestGroup group : chestManager.getAllGroups()) {
            for (Location loc : group.getPlacedLocations()) {
                if (loc == null || !loc.getWorld().equals(world)) continue;
                
                // Check if this location is in the loaded chunk
                int locChunkX = loc.getBlockX() >> 4;
                int locChunkZ = loc.getBlockZ() >> 4;
                
                if (locChunkX == chunkX && locChunkZ == chunkZ) {
                    if (isMatchingContainer(loc, group)) {
                        syncContainerToGroupState(loc, group);
                    } else {
                        chestManager.unregisterChest(loc);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        reconcileAfterEvent(event.blockList().stream().map(Block::getLocation).toList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        reconcileAfterEvent(event.blockList().stream().map(Block::getLocation).toList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        reconcileAfterEvent(List.of(event.getBlock().getLocation()));
    }

    private void reconcileAfterEvent(List<Location> locations) {
        List<Location> tracked = new ArrayList<>();
        for (Location loc : locations) {
            if (bulbManager.isWirelessBulbLocation(loc) || chestManager.isWirelessChestLocation(loc)) {
                tracked.add(loc);
            }
        }
        if (tracked.isEmpty()) return;
        WirelessRedstonePlugin plugin = WirelessRedstonePlugin.getInstance();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Location loc : tracked) {
                bulbManager.getGroupByLocation(loc).ifPresent(group -> {
                    if (BulbUtils.getBulbTypeFromMaterial(loc.getBlock().getType()) != group.getBulbType()) {
                        bulbManager.unregisterBulb(loc);
                    }
                });
                chestManager.getGroupByLocation(loc).ifPresent(group -> {
                    if (!isMatchingContainer(loc, group)) {
                        chestManager.unregisterChest(loc);
                    }
                });
            }
            plugin.getWireViewManager().refreshAllPlayers();
        });
    }

    private boolean isMatchingContainer(Location loc, ChestGroup group) {
        ChestVariant variant = ChestVariant.fromMaterial(loc.getBlock().getType());
        return variant != null && variant.getContainerType() == group.getContainerType();
    }

    private void syncBulbToGroupState(Location location, BulbGroup group) {
        Block block = location.getBlock();
        
        if (BulbUtils.isCopperBulb(block)) {
            CopperBulb data = (CopperBulb) block.getBlockData();
            if (data.isLit() != group.isLit()) {
                data.setLit(group.isLit());
                block.setBlockData(data, true);
            }
        } else if (BulbUtils.isRedstoneLamp(block)) {
            Lightable data = (Lightable) block.getBlockData();
            if (data.isLit() != group.isLit()) {
                data.setLit(group.isLit());
                block.setBlockData(data, true);
            }
        }
    }

    private void syncContainerToGroupState(Location location, ChestGroup group) {
        chestManager.applySharedInventory(location, group);
    }
}
