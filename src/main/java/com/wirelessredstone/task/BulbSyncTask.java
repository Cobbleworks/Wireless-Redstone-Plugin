package com.wirelessredstone.task;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.item.BulbVariant;
import com.wirelessredstone.manager.DebugManager;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.util.BulbUtils;
import com.wirelessredstone.util.ParticleEffects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.CopperBulb;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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

        sendDebugMessages(group, sourceLocation, placedLocations, sourceState);

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

        sendDebugMessages(group, sourceLocation, placedLocations, sourceState);

        final Set<Location> toRemove = new HashSet<>(placedLocations);
        new BukkitRunnable() {
            @Override
            public void run() {
                recentlySynced.removeAll(toRemove);
            }
        }.runTaskLater(WirelessRedstonePlugin.getInstance(), 5L);
    }

    private void sendDebugMessages(BulbGroup group, Location sourceLocation, List<Location> allLocations, boolean newState) {
        String groupName = group.getDisplayName();
        String stateText = newState ? "ON" : "OFF";
        NamedTextColor stateColor = newState ? NamedTextColor.GREEN : NamedTextColor.RED;

        for (Player player : sourceLocation.getWorld().getPlayers()) {
            boolean shouldShowDebug = false;
            Location nearestLoc = null;

            for (Location loc : allLocations) {
                if (debugManager.shouldShowDebugForLocation(player, loc)) {
                    shouldShowDebug = true;
                    nearestLoc = loc;
                    break;
                }
            }

            if (shouldShowDebug && nearestLoc != null) {
                int syncedCount = allLocations.size() - 1;
                player.sendMessage(
                    Component.text("[" + groupName + "] ", NamedTextColor.AQUA)
                        .append(Component.text(formatShortLocation(sourceLocation), NamedTextColor.GRAY))
                        .append(Component.text(" → ", NamedTextColor.WHITE))
                        .append(Component.text(syncedCount + " bulb(s)", NamedTextColor.GRAY))
                        .append(Component.text(" [", NamedTextColor.DARK_GRAY))
                        .append(Component.text(stateText, stateColor))
                        .append(Component.text("]", NamedTextColor.DARK_GRAY))
                );
            }
        }
    }

    private String formatShortLocation(Location loc) {
        return String.format("%d,%d,%d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
