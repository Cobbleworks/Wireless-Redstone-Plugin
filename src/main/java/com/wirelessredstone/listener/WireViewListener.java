package com.wirelessredstone.listener;

import com.wirelessredstone.manager.WireViewManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class WireViewListener implements Listener {

    private final WireViewManager wireViewManager;

    public WireViewListener(WireViewManager wireViewManager) {
        this.wireViewManager = wireViewManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        wireViewManager.cleanupPlayer(event.getPlayer());
    }
}
