package com.wirelessredstone.listener;

import com.wirelessredstone.gui.BulbManagerGUI;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.manager.LinkedChestManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GUIListener implements Listener {

    private final LinkedBulbManager bulbManager;
    private final LinkedChestManager chestManager;

    public GUIListener(LinkedBulbManager bulbManager, LinkedChestManager chestManager) {
        this.bulbManager = bulbManager;
        this.chestManager = chestManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BulbManagerGUI gui)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() != event.getInventory()) {
            return;
        }

        boolean isMiddleClick = event.getClick() == ClickType.MIDDLE;

        gui.handleClick(event.getSlot(), event.isRightClick(), event.isShiftClick(), isMiddleClick);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        if (BulbManagerGUI.hasPendingRename(player.getUniqueId())) {
            event.setCancelled(true);
            String message = event.getMessage();
            
            player.getServer().getScheduler().runTask(
                player.getServer().getPluginManager().getPlugin("WirelessRedstone"),
                () -> BulbManagerGUI.processRename(player, message, bulbManager, chestManager)
            );
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BulbManagerGUI.cancelPendingRename(event.getPlayer().getUniqueId());
    }
}
