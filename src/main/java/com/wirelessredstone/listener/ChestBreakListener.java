package com.wirelessredstone.listener;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.item.ChestVariant;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.model.ChestGroup;
import com.wirelessredstone.util.ParticleEffects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Optional;
import java.util.UUID;

public class ChestBreakListener implements Listener {

    private final LinkedChestManager chestManager;

    public ChestBreakListener(LinkedChestManager chestManager) {
        this.chestManager = chestManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material blockType = block.getType();
        
        if (blockType != Material.CHEST && blockType != Material.BARREL && !ChestVariant.isShulkerBox(blockType) && !ChestVariant.isCopperChest(blockType)) {
            return;
        }
        
        Location location = block.getLocation();

        if (!chestManager.isWirelessChestLocation(location)) {
            return;
        }

        Player player = event.getPlayer();
        Optional<ChestGroup> groupOpt = chestManager.getGroupByLocation(location);
        String groupName = groupOpt.map(ChestGroup::getDisplayName).orElse("Unknown");
        UUID groupId = groupOpt.map(ChestGroup::getGroupId).orElse(null);
        int remainingCount = groupOpt.map(g -> g.getPlacedCount() - 1).orElse(0);

        if (remainingCount > 0) {
            clearContainerInventory(block);
        }

        ParticleEffects.spawnBreakParticles(location);

        chestManager.unregisterChest(location);

        if (remainingCount <= 0) {
            player.sendMessage(Component.text("⚡ ", NamedTextColor.YELLOW)
                    .append(Component.text("Group ", NamedTextColor.GRAY))
                    .append(Component.text(groupName, NamedTextColor.GOLD))
                    .append(Component.text(" has been removed (last container broken)", NamedTextColor.GRAY)));
        } else {
            player.sendMessage(Component.text("⚡ ", NamedTextColor.YELLOW)
                    .append(Component.text("Removed from group ", NamedTextColor.GRAY))
                    .append(Component.text(groupName, NamedTextColor.GOLD))
                    .append(Component.text(" (" + remainingCount + " remaining)", NamedTextColor.DARK_GRAY)));
        }

        WirelessRedstonePlugin plugin = WirelessRedstonePlugin.getInstance();
        plugin.getWireViewManager().refreshAllPlayers();
        if (groupId != null) {
            plugin.getWireViewManager().refreshSingleGroupViewForGroup(groupId);
            if (remainingCount > 0) {
                plugin.getServer().getScheduler().runTask(plugin, () -> chestManager.syncGroupToPlacedContainers(groupId));
            }
        }
    }

    private void clearContainerInventory(Block block) {
        var state = block.getState();
        if (state instanceof org.bukkit.block.Container container) {
            container.getInventory().clear();
        }
    }
}
