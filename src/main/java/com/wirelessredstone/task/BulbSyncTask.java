package com.wirelessredstone.task;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.item.BulbVariant;
import com.wirelessredstone.item.CircuitAnalyserFactory;
import com.wirelessredstone.manager.DebugManager;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.manager.WireViewManager;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.util.BulbUtils;
import com.wirelessredstone.util.ParticleEffects;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.CopperBulb;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BulbSyncTask extends BukkitRunnable {

    private final LinkedBulbManager bulbManager;
    private final DebugManager debugManager;
    private final Set<Location> recentlySynced = new HashSet<>();

    public BulbSyncTask(LinkedBulbManager bulbManager, DebugManager debugManager) {
        this.bulbManager = bulbManager;
        this.debugManager = debugManager;
    }

    private int ambientParticleTick = 0;
    private static final int AMBIENT_PARTICLE_INTERVAL = 10;

    @Override
    public void run() {
        recentlySynced.removeIf(loc -> !bulbManager.isWirelessBulbLocation(loc));
        ambientParticleTick++;

        for (BulbGroup group : bulbManager.getAllGroups()) {
            List<Location> placedLocations = group.getPlacedLocations();

            if (ambientParticleTick >= AMBIENT_PARTICLE_INTERVAL) {
                for (Location loc : placedLocations) {
                    if (loc.isChunkLoaded()) {
                        ParticleEffects.spawnAmbientParticles(loc, group.isLit());
                    }
                }
            }

            if (placedLocations.size() < 2) {
                continue;
            }

            if (group.getBulbType() == BulbVariant.BulbType.REDSTONE_LAMP) {
                syncRedstoneLamps(group, placedLocations);
            } else {
                syncCopperBulbs(group, placedLocations);
            }
        }

        if (ambientParticleTick >= AMBIENT_PARTICLE_INTERVAL) {
            ambientParticleTick = 0;
        }
    }

    private void syncCopperBulbs(BulbGroup group, List<Location> placedLocations) {
        Location sourceLocation = null;
        boolean sourceState = false;
        boolean stateChanged = false;

        for (Location loc : placedLocations) {
            if (!loc.isChunkLoaded()) continue;
            Block block = loc.getBlock();
            if (!BulbUtils.isCopperBulb(block)) continue;

            CopperBulb data = (CopperBulb) block.getBlockData();
            boolean lit = data.isLit();

            if (!recentlySynced.contains(loc) && lit != group.isLit()) {
                sourceLocation = loc;
                sourceState = lit;
                stateChanged = true;
                break;
            }
        }

        if (!stateChanged) {
            boolean anyLit = false;
            for (Location loc : placedLocations) {
                if (!loc.isChunkLoaded()) continue;
                Block block = loc.getBlock();
                if (!BulbUtils.isCopperBulb(block)) continue;
                CopperBulb data = (CopperBulb) block.getBlockData();
                if (data.isLit()) {
                    anyLit = true;
                    break;
                }
            }
            if (anyLit == group.isLit()) {
                recentlySynced.removeAll(placedLocations);
            }
            return;
        }

        group.setLit(sourceState);
        recentlySynced.add(sourceLocation);

        for (Location targetLoc : placedLocations) {
            if (targetLoc.equals(sourceLocation)) continue;
            if (!targetLoc.isChunkLoaded()) continue;

            Block targetBlock = targetLoc.getBlock();
            if (!BulbUtils.isCopperBulb(targetBlock)) continue;

            CopperBulb targetData = (CopperBulb) targetBlock.getBlockData();
            if (targetData.isLit() != sourceState) {
                targetData.setLit(sourceState);
                targetBlock.setBlockData(targetData, true);
                targetLoc.getWorld().getBlockAt(targetLoc).getState().update(true, true);
                recentlySynced.add(targetLoc);

                ParticleEffects.spawnSyncParticles(targetLoc, sourceState);
            }
        }

        ParticleEffects.spawnTriggerParticles(sourceLocation, sourceState);
        ParticleEffects.spawnSyncParticles(sourceLocation, sourceState);

        showDebugConnections(group, sourceLocation, placedLocations);

        final Set<Location> toRemove = new HashSet<>(placedLocations);
        new BukkitRunnable() {
            @Override
            public void run() {
                recentlySynced.removeAll(toRemove);
            }
        }.runTaskLater(WirelessRedstonePlugin.getInstance(), 5L);
    }

    private void syncRedstoneLamps(BulbGroup group, List<Location> placedLocations) {
        Location sourceLocation = null;
        boolean sourceState = false;
        boolean stateChanged = false;

        for (Location loc : placedLocations) {
            if (!loc.isChunkLoaded()) continue;
            Block block = loc.getBlock();
            if (!BulbUtils.isRedstoneLamp(block)) continue;

            Lightable data = (Lightable) block.getBlockData();
            boolean lit = data.isLit();

            if (!recentlySynced.contains(loc) && lit != group.isLit()) {
                sourceLocation = loc;
                sourceState = lit;
                stateChanged = true;
                break;
            }
        }

        if (!stateChanged) {
            boolean anyLit = false;
            for (Location loc : placedLocations) {
                if (!loc.isChunkLoaded()) continue;
                Block block = loc.getBlock();
                if (!BulbUtils.isRedstoneLamp(block)) continue;
                Lightable data = (Lightable) block.getBlockData();
                if (data.isLit()) {
                    anyLit = true;
                    break;
                }
            }
            if (anyLit == group.isLit()) {
                recentlySynced.removeAll(placedLocations);
            }
            return;
        }

        group.setLit(sourceState);
        recentlySynced.add(sourceLocation);

        for (Location targetLoc : placedLocations) {
            if (targetLoc.equals(sourceLocation)) continue;
            if (!targetLoc.isChunkLoaded()) continue;

            Block targetBlock = targetLoc.getBlock();
            if (!BulbUtils.isRedstoneLamp(targetBlock)) continue;

            Lightable targetData = (Lightable) targetBlock.getBlockData();
            if (targetData.isLit() != sourceState) {
                targetData.setLit(sourceState);
                targetBlock.setBlockData(targetData, false);
                recentlySynced.add(targetLoc);

                ParticleEffects.spawnSyncParticles(targetLoc, sourceState);
            }
        }

        ParticleEffects.spawnTriggerParticles(sourceLocation, sourceState);
        ParticleEffects.spawnSyncParticles(sourceLocation, sourceState);

        showDebugConnections(group, sourceLocation, placedLocations);

        final Set<Location> toRemove = new HashSet<>(placedLocations);
        new BukkitRunnable() {
            @Override
            public void run() {
                recentlySynced.removeAll(toRemove);
            }
        }.runTaskLater(WirelessRedstonePlugin.getInstance(), 5L);
    }

    private void showDebugConnections(BulbGroup group, Location sourceLocation, List<Location> allLocations) {
        Color groupColor = WireViewManager.getBulbGroupParticleColor(group.getGroupId(), bulbManager.getAllPlacedGroups());
        List<Location> snapshotLocations = new ArrayList<>(allLocations);
        int lingerTicks = getConnectionLineLingerTicks();

        drawDebugConnections(sourceLocation, snapshotLocations, groupColor);

        if (lingerTicks <= 1) {
            return;
        }

        new BukkitRunnable() {
            private int ticksDrawn = 1;

            @Override
            public void run() {
                if (++ticksDrawn > lingerTicks) {
                    cancel();
                    return;
                }
                drawDebugConnections(sourceLocation, snapshotLocations, groupColor);
            }
        }.runTaskTimer(WirelessRedstonePlugin.getInstance(), 1L, 1L);
    }

    private void drawDebugConnections(Location sourceLocation, List<Location> allLocations, Color groupColor) {
        if (sourceLocation.getWorld() == null) return;

        for (Player player : sourceLocation.getWorld().getPlayers()) {
            if (!debugManager.isDebugEnabled(player) && !isHoldingCircuitAnalyser(player)) {
                continue;
            }

            for (Location targetLocation : allLocations) {
                if (targetLocation.equals(sourceLocation)) continue;
                if (!targetLocation.isChunkLoaded()) continue;
                ParticleEffects.spawnDebugConnectionLine(player, sourceLocation, targetLocation, groupColor);
            }
        }
    }

    private int getConnectionLineLingerTicks() {
        return Math.max(1, Math.min(100, WirelessRedstonePlugin.getInstance()
                .getConfig()
                .getInt("effects.connection-lines.linger-ticks", 10)));
    }

    private boolean isHoldingCircuitAnalyser(Player player) {
        return CircuitAnalyserFactory.isCircuitAnalyser(player.getInventory().getItemInMainHand())
                || CircuitAnalyserFactory.isCircuitAnalyser(player.getInventory().getItemInOffHand());
    }
}
