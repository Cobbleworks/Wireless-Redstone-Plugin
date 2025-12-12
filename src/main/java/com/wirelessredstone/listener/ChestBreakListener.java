package com.wirelessredstone.listener;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.util.ParticleEffects;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class ChestBreakListener implements Listener {

    private final LinkedChestManager chestManager;

    public ChestBreakListener(LinkedChestManager chestManager) {
        this.chestManager = chestManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        if (block.getType() != Material.CHEST) {
            return;
        }
        
        Location location = block.getLocation();

        if (!chestManager.isWirelessChestLocation(location)) {
            return;
        }

        ParticleEffects.spawnBreakParticles(location);

        chestManager.unregisterChest(location);

        WirelessRedstonePlugin.getInstance().getWireViewManager().refreshAllPlayers();
    }
}
