package com.wirelessredstone.gui;

import com.wirelessredstone.item.BulbVariant;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.model.BulbPair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BulbManagerGUI implements InventoryHolder {

    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;
    private static final int ITEMS_PER_PAGE = 28;

    private final LinkedBulbManager bulbManager;
    private final Player player;
    private final Inventory inventory;
    private int currentPage = 0;
    private List<BulbPair> pairs;
    private boolean showAllPairs;

    public BulbManagerGUI(LinkedBulbManager bulbManager, Player player, boolean showAllPairs) {
        this.bulbManager = bulbManager;
        this.player = player;
        this.showAllPairs = showAllPairs;
        this.inventory = Bukkit.createInventory(this, SIZE, 
            Component.text("Wireless Bulb Manager", NamedTextColor.DARK_AQUA).decoration(TextDecoration.BOLD, true));
        refreshPairs();
        populateInventory();
    }

    private void refreshPairs() {
        if (showAllPairs && player.hasPermission("wirelessredstone.admin")) {
            pairs = bulbManager.getAllPlacedPairs();
        } else {
            pairs = bulbManager.getPairsByOwner(player.getUniqueId());
        }
    }

    private void populateInventory() {
        inventory.clear();

        fillBorder();

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, pairs.size());

        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;

            BulbPair pair = pairs.get(i);
            inventory.setItem(slot, createPairItem(pair));
            slot++;
        }

        if (currentPage > 0) {
            inventory.setItem(48, createNavigationItem(Material.ARROW, "Previous Page", NamedTextColor.YELLOW));
        }

        inventory.setItem(49, createInfoItem());

        if (endIndex < pairs.size()) {
            inventory.setItem(50, createNavigationItem(Material.ARROW, "Next Page", NamedTextColor.YELLOW));
        }

        if (player.hasPermission("wirelessredstone.admin")) {
            inventory.setItem(45, createToggleViewItem());
        }

        inventory.setItem(53, createCloseItem());
    }

    private void fillBorder() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.empty());
        border.setItemMeta(meta);

        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
        }
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }

    private ItemStack createPairItem(BulbPair pair) {
        Material material = pair.getBulbType() == BulbVariant.BulbType.REDSTONE_LAMP 
            ? Material.REDSTONE_LAMP 
            : Material.COPPER_BULB;
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String shortId = pair.getPairId().toString().substring(0, 8);
        meta.displayName(Component.text("Pair: " + shortId, NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        
        lore.add(Component.text("Type: ", NamedTextColor.GRAY)
                .append(Component.text(pair.getBulbType().name(), NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        
        lore.add(Component.text("Status: ", NamedTextColor.GRAY)
                .append(Component.text(pair.isLit() ? "ON" : "OFF", pair.isLit() ? NamedTextColor.GREEN : NamedTextColor.RED))
                .decoration(TextDecoration.ITALIC, false));
        
        lore.add(Component.empty());
        
        Location loc1 = pair.getLocation1();
        Location loc2 = pair.getLocation2();
        
        if (loc1 != null) {
            lore.add(Component.text("Location A: ", NamedTextColor.GRAY)
                    .append(Component.text(formatLocation(loc1), NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Location A: ", NamedTextColor.GRAY)
                    .append(Component.text("Not placed", NamedTextColor.DARK_GRAY))
                    .decoration(TextDecoration.ITALIC, false));
        }
        
        if (loc2 != null) {
            lore.add(Component.text("Location B: ", NamedTextColor.GRAY)
                    .append(Component.text(formatLocation(loc2), NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Location B: ", NamedTextColor.GRAY)
                    .append(Component.text("Not placed", NamedTextColor.DARK_GRAY))
                    .decoration(TextDecoration.ITALIC, false));
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("Left-click: ", NamedTextColor.YELLOW)
                .append(Component.text("Teleport to A", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Right-click: ", NamedTextColor.YELLOW)
                .append(Component.text("Teleport to B", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shift+Click: ", NamedTextColor.RED)
                .append(Component.text("Remove pair", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNavigationItem(Material material, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Page Info", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        
        int totalPages = Math.max(1, (int) Math.ceil((double) pairs.size() / ITEMS_PER_PAGE));
        meta.lore(List.of(
                Component.text("Page " + (currentPage + 1) + "/" + totalPages, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Total pairs: " + pairs.size(), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createToggleViewItem() {
        ItemStack item = new ItemStack(showAllPairs ? Material.ENDER_EYE : Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(showAllPairs ? "Viewing: All Pairs" : "Viewing: My Pairs", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Click to toggle view", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Close", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private String formatLocation(Location loc) {
        return String.format("%s: %d, %d, %d", 
            loc.getWorld().getName(), 
            loc.getBlockX(), 
            loc.getBlockY(), 
            loc.getBlockZ());
    }

    public void handleClick(int slot, boolean isRightClick, boolean isShiftClick) {
        if (slot == 48 && currentPage > 0) {
            currentPage--;
            populateInventory();
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) pairs.size() / ITEMS_PER_PAGE));
        if (slot == 50 && currentPage < totalPages - 1) {
            currentPage++;
            populateInventory();
            return;
        }

        if (slot == 45 && player.hasPermission("wirelessredstone.admin")) {
            showAllPairs = !showAllPairs;
            currentPage = 0;
            refreshPairs();
            populateInventory();
            return;
        }

        if (slot == 53) {
            player.closeInventory();
            return;
        }

        int pairIndex = getPairIndexFromSlot(slot);
        if (pairIndex < 0 || pairIndex >= pairs.size()) {
            return;
        }

        BulbPair pair = pairs.get(pairIndex);

        if (isShiftClick) {
            handleRemovePair(pair);
        } else if (isRightClick) {
            handleTeleport(pair.getLocation2(), "B");
        } else {
            handleTeleport(pair.getLocation1(), "A");
        }
    }

    private int getPairIndexFromSlot(int slot) {
        if (slot < 10 || slot > 43) return -1;
        if (slot % 9 == 0 || slot % 9 == 8) return -1;

        int row = slot / 9 - 1;
        int col = slot % 9 - 1;
        int indexInPage = row * 7 + col;

        return currentPage * ITEMS_PER_PAGE + indexInPage;
    }

    private void handleTeleport(Location location, String name) {
        if (location == null) {
            player.sendMessage(Component.text("Location " + name + " is not placed yet!", NamedTextColor.RED));
            return;
        }

        if (!player.hasPermission("wirelessredstone.teleport")) {
            player.sendMessage(Component.text("You don't have permission to teleport!", NamedTextColor.RED));
            return;
        }

        player.closeInventory();
        Location teleportLoc = location.clone().add(0.5, 1, 0.5);
        teleportLoc.setYaw(player.getLocation().getYaw());
        teleportLoc.setPitch(player.getLocation().getPitch());
        player.teleport(teleportLoc);
        player.sendMessage(Component.text("Teleported to bulb " + name + "!", NamedTextColor.GREEN));
    }

    private void handleRemovePair(BulbPair pair) {
        if (!player.hasPermission("wirelessredstone.remove")) {
            player.sendMessage(Component.text("You don't have permission to remove pairs!", NamedTextColor.RED));
            return;
        }

        UUID ownerId = pair.getOwnerUuid();
        if (ownerId != null && !ownerId.equals(player.getUniqueId()) && !player.hasPermission("wirelessredstone.admin")) {
            player.sendMessage(Component.text("You can only remove your own pairs!", NamedTextColor.RED));
            return;
        }

        bulbManager.removePair(pair.getPairId());
        player.sendMessage(Component.text("Pair removed successfully!", NamedTextColor.GREEN));
        
        refreshPairs();
        
        int totalPages = Math.max(1, (int) Math.ceil((double) pairs.size() / ITEMS_PER_PAGE));
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }
        
        populateInventory();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
    }
}
