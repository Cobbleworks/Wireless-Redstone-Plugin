package com.wirelessredstone.gui;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.manager.CategoryManager;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.manager.LinkedChestManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** A temporary, non-persisted working copy of a player's menu order. */
public class MenuReorderGUI implements InventoryHolder {

    private static final int SIZE = 54;
    private static final int ITEMS_PER_PAGE = 45;

    public record Entry(String key, ItemStack item) {}

    private final LinkedBulbManager bulbManager;
    private final LinkedChestManager chestManager;
    private final CategoryManager categoryManager;
    private final Player player;
    private final boolean showAllGroups;
    private final String categoryName;
    private final String orderContext;
    private final Inventory inventory;
    private final List<Entry> entries;
    private final NamespacedKey markerKey;
    private int currentPage;
    private Entry heldEntry;
    private int heldOriginalIndex = -1;
    private boolean leaving;

    public MenuReorderGUI(LinkedBulbManager bulbManager, LinkedChestManager chestManager,
                          CategoryManager categoryManager, Player player, boolean showAllGroups,
                          String categoryName, String orderContext, List<Entry> entries) {
        this.bulbManager = bulbManager;
        this.chestManager = chestManager;
        this.categoryManager = categoryManager;
        this.player = player;
        this.showAllGroups = showAllGroups;
        this.categoryName = categoryName;
        this.orderContext = orderContext;
        this.entries = new ArrayList<>(entries);
        this.markerKey = new NamespacedKey(WirelessRedstonePlugin.getInstance(), "menu_reorder_marker");
        this.inventory = Bukkit.createInventory(this, SIZE,
                Component.text("Rearrange Wireless Menu", NamedTextColor.DARK_AQUA)
                        .decoration(TextDecoration.BOLD, true));
        populateInventory();
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() != inventory) return;

        int slot = event.getRawSlot();
        if (slot < ITEMS_PER_PAGE) {
            handleEntrySlot(slot);
            return;
        }

        if (slot == 45) {
            if (currentPage > 0) {
                currentPage--;
                populateInventory();
            }
        } else if (slot == 50) {
            if ((currentPage + 1) * ITEMS_PER_PAGE < entries.size()) {
                currentPage++;
                populateInventory();
            }
        } else if (slot == 52) {
            returnHeldEntry();
            WirelessRedstonePlugin.getInstance().getMenuOrderManager().setOrder(
                    player.getUniqueId(), orderContext, entries.stream().map(Entry::key).toList());
            player.sendMessage(Component.text("Wireless menu order saved.", NamedTextColor.GREEN));
            reopenMainMenu();
        } else if (slot == 53) {
            clearHeldCursor();
            player.sendMessage(Component.text("Menu reordering cancelled.", NamedTextColor.GRAY));
            reopenMainMenu();
        }
    }

    public void handleDrag(InventoryDragEvent event) {
        event.setCancelled(true);
        if (heldEntry == null || event.getRawSlots().size() != 1) return;
        int slot = event.getRawSlots().iterator().next();
        if (slot < 0 || slot >= ITEMS_PER_PAGE) return;
        placeHeldAt(currentPage * ITEMS_PER_PAGE + slot);
    }

    public void handleClose() {
        if (!leaving) {
            clearHeldCursor();
        }
        // Some client/server combinations return a cursor item after the close event.
        Bukkit.getScheduler().runTask(WirelessRedstonePlugin.getInstance(), this::removeLeakedMarkerItems);
    }

    private void handleEntrySlot(int slot) {
        int index = currentPage * ITEMS_PER_PAGE + slot;
        if (heldEntry != null) {
            placeHeldAt(index);
            return;
        }
        if (index < 0 || index >= entries.size()) return;
        ItemStack currentCursor = player.getItemOnCursor();
        if (!currentCursor.getType().isAir()) {
            player.sendMessage(Component.text("Place the item on your cursor away before rearranging.", NamedTextColor.RED));
            return;
        }

        heldOriginalIndex = index;
        heldEntry = entries.remove(index);
        ItemStack marker = heldEntry.item().clone();
        ItemMeta markerMeta = marker.getItemMeta();
        markerMeta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        marker.setItemMeta(markerMeta);
        player.setItemOnCursor(marker);
        populateInventory();
    }

    private void placeHeldAt(int index) {
        if (heldEntry == null) return;
        entries.add(Math.max(0, Math.min(index, entries.size())), heldEntry);
        clearHeldCursor();
        populateInventory();
    }

    private void returnHeldEntry() {
        if (heldEntry == null) return;
        entries.add(Math.max(0, Math.min(heldOriginalIndex, entries.size())), heldEntry);
        clearHeldCursor();
    }

    private void clearHeldCursor() {
        heldEntry = null;
        heldOriginalIndex = -1;
        if (isMarker(player.getItemOnCursor())) {
            player.setItemOnCursor(new ItemStack(Material.AIR));
        }
    }

    private boolean isMarker(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    private void removeLeakedMarkerItems() {
        if (!player.isOnline()) return;
        if (isMarker(player.getItemOnCursor())) {
            player.setItemOnCursor(new ItemStack(Material.AIR));
        }
        ItemStack[] contents = player.getInventory().getContents();
        boolean changed = false;
        for (int i = 0; i < contents.length; i++) {
            if (isMarker(contents[i])) {
                contents[i] = null;
                changed = true;
            }
        }
        if (changed) player.getInventory().setContents(contents);
    }

    private void reopenMainMenu() {
        leaving = true;
        clearHeldCursor();
        player.closeInventory();
        new BulbManagerGUI(bulbManager, chestManager, categoryManager, player, showAllGroups, categoryName).open();
    }

    private void populateInventory() {
        inventory.clear();
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, entries.size());
        for (int i = start; i < end; i++) {
            inventory.setItem(i - start, entries.get(i).item().clone());
        }

        ItemStack border = namedItem(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY);
        for (int slot = 45; slot < SIZE; slot++) inventory.setItem(slot, border);
        if (currentPage > 0) {
            inventory.setItem(45, namedItem(Material.ARROW, "Previous Page", NamedTextColor.YELLOW));
        }
        if (end < entries.size()) {
            inventory.setItem(50, namedItem(Material.ARROW, "Next Page", NamedTextColor.YELLOW));
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / ITEMS_PER_PAGE));
        ItemStack info = namedItem(Material.BOOK, "Drag and drop entries", NamedTextColor.GOLD);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.lore(List.of(
                Component.text("Pick up an entry, then click or drag", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("it onto its new position.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Page " + (currentPage + 1) + "/" + totalPages, NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        info.setItemMeta(infoMeta);
        inventory.setItem(49, info);
        inventory.setItem(52, namedItem(Material.LIME_DYE, "Confirm Order", NamedTextColor.GREEN));
        inventory.setItem(53, namedItem(Material.RED_DYE, "Cancel", NamedTextColor.RED));
    }

    private ItemStack namedItem(Material material, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}
