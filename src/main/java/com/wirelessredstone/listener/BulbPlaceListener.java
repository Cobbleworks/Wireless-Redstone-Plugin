package com.wirelessredstone.listener;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.item.BulbVariant;
import com.wirelessredstone.item.WirelessBulbFactory;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.util.ParticleEffects;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.List;
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

        var groupIdOpt = bulbManager.getGroupId(itemInHand);
        var bulbIndexOpt = bulbManager.getBulbIndex(itemInHand);

        if (groupIdOpt.isEmpty() || bulbIndexOpt.isEmpty()) {
            return;
        }

        UUID ownerUuid = bulbManager.getOwnerUuid(itemInHand).orElse(event.getPlayer().getUniqueId());
        BulbVariant.BulbType bulbType = bulbManager.getBulbType(itemInHand).orElse(BulbVariant.BulbType.COPPER_BULB);
        int groupSize = bulbManager.getGroupSize(itemInHand).orElse(2);

        var location = event.getBlock().getLocation();
        bulbManager.registerPlacedBulb(location, groupIdOpt.get(), bulbIndexOpt.get(), ownerUuid, bulbType, groupSize);

        ParticleEffects.spawnTriggerParticles(location, false);

        WirelessRedstonePlugin.getInstance().getWireViewManager().refreshAllPlayers();

        bulbManager.getGroupById(groupIdOpt.get()).ifPresent(group -> {
            List<Location> otherLocations = group.getOtherLocations(location);
            if (!otherLocations.isEmpty()) {
                ParticleEffects.spawnSyncParticles(location, false);
                for (Location otherLoc : otherLocations) {
                    ParticleEffects.spawnSyncParticles(otherLoc, false);
                }
            }
        });
    }
}
