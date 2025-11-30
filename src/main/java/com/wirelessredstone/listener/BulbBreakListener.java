package com.wirelessredstone.listener;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.util.BulbUtils;
import com.wirelessredstone.util.ParticleEffects;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class BulbBreakListener implements Listener {

    private final LinkedBulbManager bulbManager;
    private final Set<Location> processingBreaks = new HashSet<>();

    public BulbBreakListener(LinkedBulbManager bulbManager) {
        this.bulbManager = bulbManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();

        if (!bulbManager.isWirelessBulbLocation(location)) {
            return;
        }

        if (processingBreaks.contains(location)) {
            processingBreaks.remove(location);
            bulbManager.unregisterBulb(location);
            return;
        }

        var linkedLocationOpt = bulbManager.getLinkedBulbLocation(location);

        bulbManager.unregisterBulb(location);

        if (linkedLocationOpt.isPresent()) {
            Location linkedLocation = linkedLocationOpt.get();
            Block linkedBlock = linkedLocation.getBlock();

            if (BulbUtils.isCopperBulb(linkedBlock)) {
                processingBreaks.add(linkedLocation);

                ParticleEffects.spawnBreakParticles(linkedLocation);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (BulbUtils.isCopperBulb(linkedBlock)) {
                            linkedBlock.breakNaturally();
                        }
                        processingBreaks.remove(linkedLocation);
                        bulbManager.unregisterBulb(linkedLocation);
                    }
                }.runTaskLater(WirelessRedstonePlugin.getInstance(), 1L);
            }
        }
    }
}
