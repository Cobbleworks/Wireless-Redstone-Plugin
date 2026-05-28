package com.wirelessredstone.task;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.item.CircuitAnalyserFactory;
import com.wirelessredstone.manager.WireViewManager;
import com.wirelessredstone.model.BaseGroup;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.model.ChestGroup;
import com.wirelessredstone.util.ParticleEffects;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Task that automatically enables/disables WireView based on whether the player
 * is holding a Circuit Analyser in their hand.
 */
public class AnalyserWireViewTask extends BukkitRunnable {

    private final JavaPlugin plugin;
    private final WireViewManager wireViewManager;
    private final Set<UUID> playersWithAnalyserView = ConcurrentHashMap.newKeySet();
    private int connectionLineTick = 0;

    private static final int CONNECTION_LINE_INTERVAL_TICKS = 30;
    private static final Color CHEST_GROUP_CONNECTION_COLOR = Color.fromRGB(255, 170, 0);

    public AnalyserWireViewTask(JavaPlugin plugin, WireViewManager wireViewManager) {
        this.plugin = plugin;
        this.wireViewManager = wireViewManager;
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            
            boolean holdingAnalyser = CircuitAnalyserFactory.isCircuitAnalyser(mainHand) 
                    || CircuitAnalyserFactory.isCircuitAnalyser(offHand);
            
            UUID playerId = player.getUniqueId();
            boolean hadAnalyserView = playersWithAnalyserView.contains(playerId);
            
            if (holdingAnalyser && !hadAnalyserView) {
                // Player just started holding analyser - enable wireview
                playersWithAnalyserView.add(playerId);
                wireViewManager.enableWireView(player);
            } else if (!holdingAnalyser && hadAnalyserView) {
                // Player stopped holding analyser - disable wireview
                playersWithAnalyserView.remove(playerId);
                wireViewManager.disableWireView(player);
            }
        }

        connectionLineTick += 10;
        if (connectionLineTick >= CONNECTION_LINE_INTERVAL_TICKS) {
            connectionLineTick = 0;
            drawConnectionLinesForAnalyserViewers();
        }
    }

    private void drawConnectionLinesForAnalyserViewers() {
        if (playersWithAnalyserView.isEmpty()) {
            return;
        }

        WirelessRedstonePlugin wirelessPlugin = WirelessRedstonePlugin.getInstance();
        List<BulbGroup> bulbGroups = wirelessPlugin.getBulbManager().getAllPlacedGroups();
        for (BulbGroup group : bulbGroups) {
            Color groupColor = WireViewManager.getBulbGroupParticleColor(group.getGroupId(), bulbGroups);
            drawConnectionLinesForGroup(group, groupColor);
        }

        for (ChestGroup group : wirelessPlugin.getChestManager().getAllPlacedGroups()) {
            drawConnectionLinesForGroup(group, CHEST_GROUP_CONNECTION_COLOR);
        }
    }

    private void drawConnectionLinesForGroup(BaseGroup group, Color color) {
        List<Location> locations = group.getPlacedLocations();
        if (locations.size() < 2) {
            return;
        }

        Location source = locations.get(0);
        if (!source.isChunkLoaded() || source.getWorld() == null) {
            return;
        }

        for (UUID playerId : playersWithAnalyserView) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player == null || !player.isOnline() || !source.getWorld().equals(player.getWorld())) {
                continue;
            }

            for (int i = 1; i < locations.size(); i++) {
                Location target = locations.get(i);
                if (target.getWorld() != null && target.isChunkLoaded()) {
                    ParticleEffects.spawnDebugConnectionLine(player, source, target, color);
                }
            }
        }
    }

    /**
     * Called when a player leaves the server.
     */
    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        if (playersWithAnalyserView.remove(playerId)) {
            wireViewManager.disableWireView(player);
        }
    }

    /**
     * Cleans up all state when the plugin is disabled.
     */
    public void cleanupAll() {
        playersWithAnalyserView.clear();
    }
}
