package com.wirelessredstone.listener;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.util.ParticleEffects;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BulbBreakListener implements Listener {

    private final LinkedBulbManager bulbManager;

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

        ParticleEffects.spawnBreakParticles(location);

        bulbManager.unregisterBulb(location);

        WirelessRedstonePlugin.getInstance().getWireViewManager().refreshAllPlayers();
    }
}
