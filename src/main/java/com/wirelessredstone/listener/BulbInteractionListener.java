package com.wirelessredstone.listener;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.util.BulbUtils;
import com.wirelessredstone.util.ParticleEffects;
import org.bukkit.GameEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.CopperBulb;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
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

        if (!BulbUtils.isCopperBulb(block)) {
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
        if (!BulbUtils.isCopperBulb(sourceBlock)) {
            return;
        }

        Location sourceLocation = sourceBlock.getLocation();
        var linkedLocationOpt = bulbManager.getLinkedBulbLocation(sourceLocation);

        if (linkedLocationOpt.isEmpty()) {
            return;
        }

        Location linkedLocation = linkedLocationOpt.get();
        Block linkedBlock = linkedLocation.getBlock();

        if (!BulbUtils.isCopperBulb(linkedBlock)) {
            return;
        }

        CopperBulb sourceData = (CopperBulb) sourceBlock.getBlockData();
        CopperBulb linkedData = (CopperBulb) linkedBlock.getBlockData();

        boolean sourceLit = sourceData.isLit();

        if (linkedData.isLit() != sourceLit) {
            processingLocations.add(linkedLocation);

            linkedData.setLit(sourceLit);
            linkedBlock.setBlockData(linkedData, false);

            ParticleEffects.spawnSyncParticles(sourceLocation, sourceLit);
            ParticleEffects.spawnSyncParticles(linkedLocation, sourceLit);

            new BukkitRunnable() {
                @Override
                public void run() {
                    processingLocations.remove(linkedLocation);
                }
            }.runTaskLater(WirelessRedstonePlugin.getInstance(), 3L);
        }
    }
}
