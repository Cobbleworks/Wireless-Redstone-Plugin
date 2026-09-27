package com.wirelessredstone.listener;

import com.wirelessredstone.gui.BulbManagerGUI;
import com.wirelessredstone.gui.CategoryAssignmentGUI;
import com.wirelessredstone.gui.CategorySelectionGUI;
import com.wirelessredstone.gui.MenuReorderGUI;
import com.wirelessredstone.manager.CategoryManager;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.manager.LinkedChestManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;

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
        if (event.getInventory().getHolder() instanceof MenuReorderGUI gui) {
            gui.handleClick(event);
            return;
        }

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
            return;
        }

        if (event.getInventory().getHolder() instanceof CategoryAssignmentGUI gui) {
            event.setCancelled(true);

            if (event.getClickedInventory() != event.getInventory()) {
                return;
            }

            gui.handleClick(event.getSlot());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuReorderGUI gui) {
            gui.handleDrag(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof MenuReorderGUI gui) {
            gui.handleClose();
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        if (!CategorySelectionGUI.hasPendingConnectorToolPrompt(player.getUniqueId())) {
            return;
        }

        boolean handled = CategorySelectionGUI.processPendingConnectorToolSelection(
                player,
                event.getClickedBlock().getLocation(),
                bulbManager,
                chestManager
        );
        if (handled) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        selectGlowEntityGroup(event);
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        selectGlowEntityGroup(event);
    }

    private void selectGlowEntityGroup(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!CategorySelectionGUI.hasPendingConnectorToolPrompt(event.getPlayer().getUniqueId())) return;
        if (!event.getRightClicked().getScoreboardTags().contains("wireview_glow")
                && !event.getRightClicked().getScoreboardTags().contains("wireview_single_glow")) return;

        if (CategorySelectionGUI.processPendingConnectorToolSelection(
                event.getPlayer(), event.getRightClicked().getLocation().getBlock().getLocation(),
                bulbManager, chestManager)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)
                || !CategorySelectionGUI.hasPendingConnectorToolPrompt(player.getUniqueId())) return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Container container) {
            if (selectContainerGroup(player, container.getLocation())) event.setCancelled(true);
        } else if (holder instanceof DoubleChest doubleChest) {
            if (doubleChest.getLeftSide() instanceof Container left
                    && selectContainerGroup(player, left.getLocation())) {
                event.setCancelled(true);
            } else if (doubleChest.getRightSide() instanceof Container right
                    && selectContainerGroup(player, right.getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    private boolean selectContainerGroup(Player player, Location location) {
        return CategorySelectionGUI.processPendingConnectorToolSelection(
                player, location, bulbManager, chestManager);
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
