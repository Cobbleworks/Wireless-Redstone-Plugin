package com.wirelessredstone.listener;

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
                    syncBulbToGroupState(loc, group);
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
                    syncContainerToGroupState(loc, group);
                }
            }
        }
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
