package com.wirelessredstone.listener;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.util.ParticleEffects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Optional;
import java.util.UUID;

public class BulbBreakListener extends WirelessBlockListener {

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

        Player player = event.getPlayer();
        Optional<BulbGroup> groupOpt = bulbManager.getGroupByLocation(location);
        String groupName = groupOpt.map(BulbGroup::getDisplayName).orElse("Unknown");
        UUID groupId = groupOpt.map(BulbGroup::getGroupId).orElse(null);
        int remainingCount = groupOpt.map(g -> g.getPlacedCount() - 1).orElse(0);

        bulbManager.unregisterBulb(location);
        finishBreak(player, location, groupOpt.orElse(null), remainingCount, "block", NamedTextColor.AQUA);
    }
}
