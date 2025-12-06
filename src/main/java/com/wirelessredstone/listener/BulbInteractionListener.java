package com.wirelessredstone.listener;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.item.BulbVariant;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.util.BulbUtils;
import com.wirelessredstone.util.ParticleEffects;
import org.bukkit.GameEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.CopperBulb;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BulbInteractionListener implements Listener {

    private final LinkedBulbManager bulbManager;
    private final Set<Location> processingLocations = new HashSet<>();

    public BulbInteractionListener(LinkedBulbManager bulbManager) {
        this.bulbManager = bulbManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameEvent(GenericGameEvent event) {
        GameEvent gameEvent = event.getEvent();

        if (gameEvent != GameEvent.BLOCK_ACTIVATE && gameEvent != GameEvent.BLOCK_DEACTIVATE) {
            return;
        }

        Location location = event.getLocation();
        Block block = location.getBlock();

        if (!BulbUtils.isWirelessCompatibleBlock(block)) {
            return;
        }

        if (!bulbManager.isWirelessBulbLocation(location)) {
            return;
        }

        if (processingLocations.contains(location)) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                syncBulbState(block);
            }
        }.runTaskLater(WirelessRedstonePlugin.getInstance(), 1L);
    }

    private void syncBulbState(Block sourceBlock) {
        if (!BulbUtils.isWirelessCompatibleBlock(sourceBlock)) {
            return;
        }

        Location sourceLocation = sourceBlock.getLocation();
        var groupOpt = bulbManager.getGroupByLocation(sourceLocation);
        
        if (groupOpt.isEmpty()) {
            return;
        }

        BulbGroup group = groupOpt.get();
        List<Location> otherLocations = group.getOtherLocations(sourceLocation);

        if (otherLocations.isEmpty()) {
            return;
        }

        boolean sourceLit = getBlockLitState(sourceBlock, group.getBulbType());

        for (Location linkedLocation : otherLocations) {
            if (!linkedLocation.isChunkLoaded()) continue;
            
            Block linkedBlock = linkedLocation.getBlock();
            if (!BulbUtils.isWirelessCompatibleBlock(linkedBlock)) {
                continue;
            }

            boolean linkedLit = getBlockLitState(linkedBlock, group.getBulbType());

            if (linkedLit != sourceLit) {
                processingLocations.add(linkedLocation);

                setBlockLitState(linkedBlock, sourceLit, group.getBulbType());

                ParticleEffects.spawnSyncParticles(linkedLocation, sourceLit);
            }
        }

        ParticleEffects.spawnSyncParticles(sourceLocation, sourceLit);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Location loc : otherLocations) {
                    processingLocations.remove(loc);
                }
            }
        }.runTaskLater(WirelessRedstonePlugin.getInstance(), 3L);
    }

    private boolean getBlockLitState(Block block, BulbVariant.BulbType bulbType) {
        if (bulbType == BulbVariant.BulbType.REDSTONE_LAMP) {
            if (BulbUtils.isRedstoneLamp(block)) {
                return ((Lightable) block.getBlockData()).isLit();
            }
        } else {
            if (BulbUtils.isCopperBulb(block)) {
                return ((CopperBulb) block.getBlockData()).isLit();
            }
        }
        return false;
    }

    private void setBlockLitState(Block block, boolean lit, BulbVariant.BulbType bulbType) {
        if (bulbType == BulbVariant.BulbType.REDSTONE_LAMP) {
            if (BulbUtils.isRedstoneLamp(block)) {
                Lightable data = (Lightable) block.getBlockData();
                data.setLit(lit);
                block.setBlockData(data, false);
            }
        } else {
            if (BulbUtils.isCopperBulb(block)) {
                CopperBulb data = (CopperBulb) block.getBlockData();
                data.setLit(lit);
                block.setBlockData(data, false);
            }
        }
    }
}
