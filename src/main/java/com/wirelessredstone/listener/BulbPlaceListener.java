package com.wirelessredstone.listener;

import com.wirelessredstone.manager.LinkedBulbManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BulbPlaceListener implements Listener {

    private final LinkedBulbManager bulbManager;

    public BulbPlaceListener(LinkedBulbManager bulbManager) {
        this.bulbManager = bulbManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        var itemInHand = event.getItemInHand();

        if (!bulbManager.isWirelessBulb(itemInHand)) {
            return;
        }

        var pairIdOpt = bulbManager.getPairId(itemInHand);
        var bulbIndexOpt = bulbManager.getBulbIndex(itemInHand);

        if (pairIdOpt.isEmpty() || bulbIndexOpt.isEmpty()) {
            return;
        }

        var location = event.getBlock().getLocation();
        bulbManager.registerPlacedBulb(location, pairIdOpt.get(), bulbIndexOpt.get());
    }
}
