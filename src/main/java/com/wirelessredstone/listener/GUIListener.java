package com.wirelessredstone.listener;

import com.wirelessredstone.gui.BulbManagerGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BulbManagerGUI gui)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() != event.getInventory()) {
            return;
        }

        gui.handleClick(event.getSlot(), event.isRightClick(), event.isShiftClick());
    }
}
