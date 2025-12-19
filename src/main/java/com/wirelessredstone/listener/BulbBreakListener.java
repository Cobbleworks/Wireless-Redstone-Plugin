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

        Player player = event.getPlayer();
        Optional<BulbGroup> groupOpt = bulbManager.getGroupByLocation(location);
        String groupName = groupOpt.map(BulbGroup::getDisplayName).orElse("Unknown");
        UUID groupId = groupOpt.map(BulbGroup::getGroupId).orElse(null);
        int remainingCount = groupOpt.map(g -> g.getPlacedCount() - 1).orElse(0);

        ParticleEffects.spawnBreakParticles(location);

        bulbManager.unregisterBulb(location);

        if (remainingCount <= 0) {
            player.sendMessage(Component.text("⚡ ", NamedTextColor.YELLOW)
                    .append(Component.text("Group ", NamedTextColor.GRAY))
                    .append(Component.text(groupName, NamedTextColor.AQUA))
                    .append(Component.text(" has been removed (last block broken)", NamedTextColor.GRAY)));
        } else {
            player.sendMessage(Component.text("⚡ ", NamedTextColor.YELLOW)
                    .append(Component.text("Removed from group ", NamedTextColor.GRAY))
                    .append(Component.text(groupName, NamedTextColor.AQUA))
                    .append(Component.text(" (" + remainingCount + " remaining)", NamedTextColor.DARK_GRAY)));
        }

        WirelessRedstonePlugin plugin = WirelessRedstonePlugin.getInstance();
        plugin.getWireViewManager().refreshAllPlayers();
        if (groupId != null) {
            plugin.getWireViewManager().refreshSingleGroupViewForGroup(groupId);
        }
    }
}
