package com.wirelessredstone.manager;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DebugManager {

    private final Set<UUID> debugEnabledPlayers = new HashSet<>();
    private static final double DEBUG_RADIUS = 3.0;

    public boolean isDebugEnabled(Player player) {
        return debugEnabledPlayers.contains(player.getUniqueId());
    }

    public void setDebugEnabled(Player player, boolean enabled) {
        if (enabled) {
            debugEnabledPlayers.add(player.getUniqueId());
        } else {
            debugEnabledPlayers.remove(player.getUniqueId());
        }
    }

    public boolean toggleDebug(Player player) {
        boolean newState = !isDebugEnabled(player);
        setDebugEnabled(player, newState);
        return newState;
    }

    public boolean shouldShowDebugForLocation(Player player, Location location) {
        if (!isDebugEnabled(player)) return false;
        if (player.getWorld() != location.getWorld()) return false;
        return player.getLocation().distanceSquared(location) <= DEBUG_RADIUS * DEBUG_RADIUS;
    }

    public void removePlayer(UUID playerUuid) {
        debugEnabledPlayers.remove(playerUuid);
    }

    public Set<UUID> getDebugEnabledPlayers() {
        return new HashSet<>(debugEnabledPlayers);
    }
}
