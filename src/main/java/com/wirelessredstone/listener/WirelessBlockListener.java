package com.wirelessredstone.listener;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.manager.LinkedGroupManager;
import com.wirelessredstone.model.BaseGroup;
import com.wirelessredstone.util.ParticleEffects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.UUID;
import java.util.function.Consumer;

/** Common visual and player feedback for wireless block lifecycle listeners. */
abstract class WirelessBlockListener implements Listener {

    protected final <G extends BaseGroup> void finishPlacement(LinkedGroupManager<G> manager,
                                                                UUID groupId, Location location,
                                                                Consumer<G> synchronize) {
        ParticleEffects.spawnTriggerParticles(location, false);
        WirelessRedstonePlugin.getInstance().getWireViewManager().refreshAllPlayers();
        manager.getGroupById(groupId).ifPresent(group -> {
            if (!group.getOtherLocations(location).isEmpty()) {
                ParticleEffects.spawnSyncParticles(location, false);
                group.getOtherLocations(location)
                        .forEach(other -> ParticleEffects.spawnSyncParticles(other, false));
                synchronize.accept(group);
            }
        });
    }

    protected final void finishBreak(Player player, Location location, BaseGroup group,
                                     int remainingCount, String blockName, NamedTextColor groupColor) {
        ParticleEffects.spawnBreakParticles(location);
        String groupName = group != null ? group.getDisplayName() : "Unknown";
        if (remainingCount <= 0) {
            player.sendMessage(Component.text("⚡ ", NamedTextColor.YELLOW)
                    .append(Component.text("Group ", NamedTextColor.GRAY))
                    .append(Component.text(groupName, groupColor))
                    .append(Component.text(" has been removed (last " + blockName + " broken)", NamedTextColor.GRAY)));
        } else {
            player.sendMessage(Component.text("⚡ ", NamedTextColor.YELLOW)
                    .append(Component.text("Removed from group ", NamedTextColor.GRAY))
                    .append(Component.text(groupName, groupColor))
                    .append(Component.text(" (" + remainingCount + " remaining)", NamedTextColor.DARK_GRAY)));
        }
        WirelessRedstonePlugin.getInstance().getWireViewManager().refreshAllPlayers();
        if (group != null) {
            WirelessRedstonePlugin.getInstance().getWireViewManager()
                    .refreshSingleGroupViewForGroup(group.getGroupId());
        }
    }
}
