package com.wirelessredstone.listener;

import com.wirelessredstone.item.BulbVariant;
import com.wirelessredstone.manager.LinkedBulbManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.UUID;

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

        UUID ownerUuid = bulbManager.getOwnerUuid(itemInHand).orElse(event.getPlayer().getUniqueId());
        BulbVariant.BulbType bulbType = bulbManager.getBulbType(itemInHand).orElse(BulbVariant.BulbType.COPPER_BULB);

        var location = event.getBlock().getLocation();
        bulbManager.registerPlacedBulb(location, pairIdOpt.get(), bulbIndexOpt.get(), ownerUuid, bulbType);
    }
}
