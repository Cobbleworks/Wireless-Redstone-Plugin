package com.wirelessredstone.task;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.item.BulbVariant;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.model.BulbPair;
import com.wirelessredstone.util.BulbUtils;
import com.wirelessredstone.util.ParticleEffects;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
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

    private int ambientParticleTick = 0;
    private static final int AMBIENT_PARTICLE_INTERVAL = 10; // Every 10 ticks (0.5 seconds)

    @Override
    public void run() {
        recentlySynced.removeIf(loc -> !bulbManager.isWirelessBulbLocation(loc));
        ambientParticleTick++;

        for (BulbPair pair : bulbManager.getAllPairs()) {
            Location loc1 = pair.getLocation1();
            Location loc2 = pair.getLocation2();

            // Spawn ambient particles for placed bulbs (even if only one is placed)
            if (ambientParticleTick >= AMBIENT_PARTICLE_INTERVAL) {
                if (loc1 != null && loc1.isChunkLoaded()) {
                    ParticleEffects.spawnAmbientParticles(loc1, pair.isLit());
                }
                if (loc2 != null && loc2.isChunkLoaded()) {
                    ParticleEffects.spawnAmbientParticles(loc2, pair.isLit());
                }
            }

            if (loc1 == null || loc2 == null) {
                continue;
            }

            if (!loc1.isChunkLoaded() || !loc2.isChunkLoaded()) {
                continue;
            }

            Block block1 = loc1.getBlock();
            Block block2 = loc2.getBlock();

            if (pair.getBulbType() == BulbVariant.BulbType.REDSTONE_LAMP) {
                syncRedstoneLamps(pair, loc1, loc2, block1, block2);
            } else {
                syncCopperBulbs(pair, loc1, loc2, block1, block2);
            }
        }

        if (ambientParticleTick >= AMBIENT_PARTICLE_INTERVAL) {
            ambientParticleTick = 0;
        }
    }

    private void syncCopperBulbs(BulbPair pair, Location loc1, Location loc2, Block block1, Block block2) {
        if (!BulbUtils.isCopperBulb(block1) || !BulbUtils.isCopperBulb(block2)) {
            return;
        }

        CopperBulb data1 = (CopperBulb) block1.getBlockData();
        CopperBulb data2 = (CopperBulb) block2.getBlockData();

        boolean lit1 = data1.isLit();
        boolean lit2 = data2.isLit();

        if (lit1 == lit2) {
            pair.setLit(lit1);
            recentlySynced.remove(loc1);
            recentlySynced.remove(loc2);
            return;
        }

        if (recentlySynced.contains(loc1) && recentlySynced.contains(loc2)) {
            return;
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

        syncCopperBulb(targetBlock, targetLocation, newState);
        pair.setLit(newState);

        recentlySynced.add(sourceLocation);
        recentlySynced.add(targetLocation);

        // Spawn trigger particles for visual feedback
        ParticleEffects.spawnTriggerParticles(sourceLocation, newState);
        ParticleEffects.spawnTriggerParticles(targetLocation, newState);
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

    private void syncRedstoneLamps(BulbPair pair, Location loc1, Location loc2, Block block1, Block block2) {
        if (!BulbUtils.isRedstoneLamp(block1) || !BulbUtils.isRedstoneLamp(block2)) {
            return;
        }

        Lightable data1 = (Lightable) block1.getBlockData();
        Lightable data2 = (Lightable) block2.getBlockData();

        boolean lit1 = data1.isLit();
        boolean lit2 = data2.isLit();

        if (lit1 == lit2) {
            pair.setLit(lit1);
            recentlySynced.remove(loc1);
            recentlySynced.remove(loc2);
            return;
        }

        if (recentlySynced.contains(loc1) && recentlySynced.contains(loc2)) {
            return;
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

        syncRedstoneLamp(targetBlock, newState);
        pair.setLit(newState);

        recentlySynced.add(sourceLocation);
        recentlySynced.add(targetLocation);

        // Spawn trigger particles for visual feedback
        ParticleEffects.spawnTriggerParticles(sourceLocation, newState);
        ParticleEffects.spawnTriggerParticles(targetLocation, newState);
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

    private void syncCopperBulb(Block targetBlock, Location targetLocation, boolean lit) {
        CopperBulb targetData = (CopperBulb) targetBlock.getBlockData();
        targetData.setLit(lit);
        
        targetBlock.setBlockData(targetData, true);
        
        targetLocation.getWorld().getBlockAt(targetLocation).getState().update(true, true);
    }

    private void syncRedstoneLamp(Block targetBlock, boolean lit) {
        Lightable targetData = (Lightable) targetBlock.getBlockData();
        targetData.setLit(lit);
        targetBlock.setBlockData(targetData, false);
    }
}
