package com.wirelessredstone.task;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.model.BulbPair;
import com.wirelessredstone.util.BulbUtils;
import com.wirelessredstone.util.ParticleEffects;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.CopperBulb;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class BulbSyncTask extends BukkitRunnable {

    private final LinkedBulbManager bulbManager;
    private final Set<Location> recentlySynced = new HashSet<>();

    public BulbSyncTask(LinkedBulbManager bulbManager) {
        this.bulbManager = bulbManager;
    }

    @Override
    public void run() {
        recentlySynced.removeIf(loc -> !bulbManager.isWirelessBulbLocation(loc));

        for (BulbPair pair : bulbManager.getAllPairs()) {
            Location loc1 = pair.getLocation1();
            Location loc2 = pair.getLocation2();

            if (loc1 == null || loc2 == null) {
                continue;
            }

            if (!loc1.isChunkLoaded() || !loc2.isChunkLoaded()) {
                continue;
            }

            Block block1 = loc1.getBlock();
            Block block2 = loc2.getBlock();

            if (!BulbUtils.isCopperBulb(block1) || !BulbUtils.isCopperBulb(block2)) {
                continue;
            }

            CopperBulb data1 = (CopperBulb) block1.getBlockData();
            CopperBulb data2 = (CopperBulb) block2.getBlockData();

            boolean lit1 = data1.isLit();
            boolean lit2 = data2.isLit();

            if (lit1 == lit2) {
                pair.setLit(lit1);
                recentlySynced.remove(loc1);
                recentlySynced.remove(loc2);
                continue;
            }

            if (recentlySynced.contains(loc1) && recentlySynced.contains(loc2)) {
                continue;
            }

            Location sourceLocation;
            Location targetLocation;
            Block targetBlock;
            boolean newState;

            if (!recentlySynced.contains(loc1) && lit1 != pair.isLit()) {
                sourceLocation = loc1;
                targetLocation = loc2;
                targetBlock = block2;
                newState = lit1;
            } else if (!recentlySynced.contains(loc2) && lit2 != pair.isLit()) {
                sourceLocation = loc2;
                targetLocation = loc1;
                targetBlock = block1;
                newState = lit2;
            } else {
                sourceLocation = loc1;
                targetLocation = loc2;
                targetBlock = block2;
                newState = lit1;
            }

            syncBulb(targetBlock, targetLocation, newState);
            pair.setLit(newState);

            recentlySynced.add(sourceLocation);
            recentlySynced.add(targetLocation);

            ParticleEffects.spawnSyncParticles(sourceLocation, newState);
            ParticleEffects.spawnSyncParticles(targetLocation, newState);

            new BukkitRunnable() {
                @Override
                public void run() {
                    recentlySynced.remove(sourceLocation);
                    recentlySynced.remove(targetLocation);
                }
            }.runTaskLater(WirelessRedstonePlugin.getInstance(), 5L);
        }
    }

    private void syncBulb(Block targetBlock, Location targetLocation, boolean lit) {
        CopperBulb targetData = (CopperBulb) targetBlock.getBlockData();
        targetData.setLit(lit);
        
        targetBlock.setBlockData(targetData, true);
        
        targetLocation.getWorld().getBlockAt(targetLocation).getState().update(true, true);
    }
}
