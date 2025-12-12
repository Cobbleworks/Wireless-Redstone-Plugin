package com.wirelessredstone.listener;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.model.ChestGroup;
import com.wirelessredstone.util.ParticleEffects;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.List;
import java.util.UUID;

public class ChestPlaceListener implements Listener {

    private final LinkedChestManager chestManager;

    public ChestPlaceListener(LinkedChestManager chestManager) {
        this.chestManager = chestManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.CHEST) {
            return;
        }
        
        var itemInHand = event.getItemInHand();

        if (!chestManager.isWirelessChest(itemInHand)) {
            return;
        }

        var groupIdOpt = chestManager.getGroupId(itemInHand);
        var chestIndexOpt = chestManager.getChestIndex(itemInHand);

        if (groupIdOpt.isEmpty() || chestIndexOpt.isEmpty()) {
            return;
        }

        UUID ownerUuid = chestManager.getOwnerUuid(itemInHand).orElse(event.getPlayer().getUniqueId());
        int groupSize = chestManager.getGroupSize(itemInHand).orElse(2);

        var location = event.getBlock().getLocation();
        chestManager.registerPlacedChest(location, groupIdOpt.get(), chestIndexOpt.get(), ownerUuid, groupSize);

        ParticleEffects.spawnTriggerParticles(location, false);

        WirelessRedstonePlugin.getInstance().getWireViewManager().refreshAllPlayers();

        chestManager.getGroupById(groupIdOpt.get()).ifPresent(group -> {
            List<Location> otherLocations = group.getOtherLocations(location);
            if (!otherLocations.isEmpty()) {
                ParticleEffects.spawnSyncParticles(location, false);
                for (Location otherLoc : otherLocations) {
                    ParticleEffects.spawnSyncParticles(otherLoc, false);
                }
                
                // Sync the shared inventory to the newly placed chest
                var block = location.getBlock();
                if (block.getState() instanceof org.bukkit.block.Chest chest) {
                    var inventory = chest.getInventory();
                    var sharedInventory = group.getSharedInventory();
                    for (int i = 0; i < Math.min(sharedInventory.length, inventory.getSize()); i++) {
                        inventory.setItem(i, sharedInventory[i] != null ? sharedInventory[i].clone() : null);
                    }
                }
            }
        });
    }
}
