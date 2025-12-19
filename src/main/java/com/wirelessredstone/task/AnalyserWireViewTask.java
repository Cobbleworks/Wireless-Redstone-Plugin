package com.wirelessredstone.task;

import com.wirelessredstone.item.CircuitAnalyserFactory;
import com.wirelessredstone.manager.WireViewManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

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
