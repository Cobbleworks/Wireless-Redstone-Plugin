package com.wirelessredstone.listener;

import com.wirelessredstone.gui.BulbManagerGUI;
import com.wirelessredstone.gui.CategorySelectionGUI;
import com.wirelessredstone.manager.CategoryManager;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.manager.LinkedChestManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GUIListener implements Listener {

    private final LinkedBulbManager bulbManager;
    private final LinkedChestManager chestManager;
    private final CategoryManager categoryManager;

    public GUIListener(LinkedBulbManager bulbManager, LinkedChestManager chestManager, CategoryManager categoryManager) {
        this.bulbManager = bulbManager;
        this.chestManager = chestManager;
        this.categoryManager = categoryManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof BulbManagerGUI gui) {
            event.setCancelled(true);

            if (event.getClickedInventory() != event.getInventory()) {
                return;
            }

            boolean isMiddleClick = event.getClick() == ClickType.MIDDLE;
            boolean isDrop = event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP;

            gui.handleClick(event.getSlot(), event.isRightClick(), event.isShiftClick(), isMiddleClick, isDrop);
            return;
        }

        if (event.getInventory().getHolder() instanceof CategorySelectionGUI gui) {
            event.setCancelled(true);

            if (event.getClickedInventory() != event.getInventory()) {
                return;
            }

            boolean isMiddleClick = event.getClick() == ClickType.MIDDLE;

            gui.handleClick(event.getSlot(), event.isRightClick(), event.isShiftClick(), isMiddleClick);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        
        if (BulbManagerGUI.hasPendingRename(player.getUniqueId())) {
            event.setCancelled(true);
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());
            
            player.getServer().getScheduler().runTask(
                player.getServer().getPluginManager().getPlugin("WirelessRedstone"),
                () -> BulbManagerGUI.processRename(player, message, bulbManager, chestManager, categoryManager)
            );
            return;
        }

        if (BulbManagerGUI.hasPendingCategoryChange(player.getUniqueId())) {
            event.setCancelled(true);
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());
            
            player.getServer().getScheduler().runTask(
                player.getServer().getPluginManager().getPlugin("WirelessRedstone"),
                () -> BulbManagerGUI.processCategoryChange(player, message, bulbManager, chestManager, categoryManager)
            );
            return;
        }

        if (CategorySelectionGUI.hasPendingAction(player.getUniqueId())) {
            event.setCancelled(true);
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());
            
            player.getServer().getScheduler().runTask(
                player.getServer().getPluginManager().getPlugin("WirelessRedstone"),
                () -> CategorySelectionGUI.processPendingAction(player, message, categoryManager, bulbManager, chestManager)
            );
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BulbManagerGUI.cancelPendingRename(event.getPlayer().getUniqueId());
        BulbManagerGUI.cancelPendingCategoryChange(event.getPlayer().getUniqueId());
        CategorySelectionGUI.cancelPendingAction(event.getPlayer().getUniqueId());
    }
}
