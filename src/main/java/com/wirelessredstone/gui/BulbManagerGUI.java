package com.wirelessredstone.gui;

import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.model.ChestGroup;
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

import java.util.*;

public class BulbManagerGUI implements InventoryHolder {

    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;
    private static final int ITEMS_PER_PAGE = 28;
    
    private static final Map<UUID, GroupEntry> pendingRenames = new HashMap<>();

    private final LinkedBulbManager bulbManager;
    private final LinkedChestManager chestManager;
    private final Player player;
    private final Inventory inventory;
    private int currentPage = 0;
    private List<GroupEntry> groups;
    private boolean showAllGroups;

    public BulbManagerGUI(LinkedBulbManager bulbManager, LinkedChestManager chestManager, Player player, boolean showAllGroups) {
        this.bulbManager = bulbManager;
        this.chestManager = chestManager;
        this.player = player;
        this.showAllGroups = showAllGroups;
        this.inventory = Bukkit.createInventory(this, SIZE, 
            Component.text("Wireless Redstone Manager", NamedTextColor.DARK_AQUA).decoration(TextDecoration.BOLD, true));
        refreshGroups();
        populateInventory();
    }

    private void refreshGroups() {
        groups = new ArrayList<>();
        
        List<BulbGroup> bulbGroups;
        List<ChestGroup> chestGroups;
        
        if (showAllGroups && player.hasPermission("wirelessredstone.admin")) {
            bulbGroups = bulbManager.getAllPlacedGroups();
            chestGroups = chestManager != null ? chestManager.getAllPlacedGroups() : Collections.emptyList();
        } else {
            bulbGroups = bulbManager.getGroupsByOwner(player.getUniqueId());
            chestGroups = chestManager != null ? chestManager.getGroupsByOwner(player.getUniqueId()) : Collections.emptyList();
        }
        
        for (BulbGroup bg : bulbGroups) {
            groups.add(new GroupEntry(bg));
        }
        for (ChestGroup cg : chestGroups) {
            groups.add(new GroupEntry(cg));
        }
    }

    private void populateInventory() {
        inventory.clear();

        fillBorder();

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, groups.size());

        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;

            GroupEntry group = groups.get(i);
            inventory.setItem(slot, createGroupItem(group));
            slot++;
        }

        if (currentPage > 0) {
            inventory.setItem(48, createNavigationItem(Material.ARROW, "Previous Page", NamedTextColor.YELLOW));
        }

        inventory.setItem(49, createInfoItem());

        if (endIndex < groups.size()) {
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

    private ItemStack createGroupItem(GroupEntry group) {
        Material material = group.getCustomIcon() != null ? group.getCustomIcon() : group.getDefaultIcon();
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String displayName = group.getDisplayName();
        NamedTextColor typeColor = group.getType() == GroupEntry.GroupType.BULB ? NamedTextColor.AQUA : NamedTextColor.GOLD;
        String typePrefix = group.getType() == GroupEntry.GroupType.BULB ? "Bulb" : "Container";
        meta.displayName(Component.text(typePrefix + " Group: " + displayName, typeColor)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        
        lore.add(Component.text("Type: ", NamedTextColor.GRAY)
                .append(Component.text(group.getTypeDisplayName(), NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        
        NamedTextColor statusColor;
        if (group.getType() == GroupEntry.GroupType.BULB) {
            statusColor = group.isLit() ? NamedTextColor.GREEN : NamedTextColor.RED;
        } else {
            statusColor = NamedTextColor.GREEN;
        }
        lore.add(Component.text("Status: ", NamedTextColor.GRAY)
                .append(Component.text(group.getStatusDisplay(), statusColor))
                .decoration(TextDecoration.ITALIC, false));
        
        lore.add(Component.text("Placed: ", NamedTextColor.GRAY)
                .append(Component.text(group.getPlacedCount() + "/" + group.getMaxSize(), NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        
        lore.add(Component.empty());
        
        List<Location> locations = group.getLocations();
        int displayedLocs = 0;
        for (int i = 0; i < locations.size() && displayedLocs < 5; i++) {
            Location loc = locations.get(i);
            String label = GroupEntry.getIndexLabel(i);
            if (loc != null) {
                lore.add(Component.text("Location " + label + ": ", NamedTextColor.GRAY)
                        .append(Component.text(formatLocation(loc), NamedTextColor.WHITE))
                        .decoration(TextDecoration.ITALIC, false));
                displayedLocs++;
            } else if (displayedLocs < 3) {
                lore.add(Component.text("Location " + label + ": ", NamedTextColor.GRAY)
                        .append(Component.text("Not placed", NamedTextColor.DARK_GRAY))
                        .decoration(TextDecoration.ITALIC, false));
            }
        }
        
        if (locations.size() > 5) {
            int remaining = (int) locations.stream().filter(Objects::nonNull).count() - displayedLocs;
            if (remaining > 0) {
                lore.add(Component.text("... and " + remaining + " more", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("Left-click: ", NamedTextColor.YELLOW)
                .append(Component.text("Teleport to first placed", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Right-click: ", NamedTextColor.YELLOW)
                .append(Component.text("Teleport to last placed", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Middle-click: ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text("Rename group", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shift+Right: ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text("Set icon to held item", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shift+Left: ", NamedTextColor.RED)
                .append(Component.text("Remove group", NamedTextColor.WHITE))
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
        
        int totalPages = Math.max(1, (int) Math.ceil((double) groups.size() / ITEMS_PER_PAGE));
        meta.lore(List.of(
                Component.text("Page " + (currentPage + 1) + "/" + totalPages, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Total groups: " + groups.size(), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createToggleViewItem() {
        ItemStack item = new ItemStack(showAllGroups ? Material.ENDER_EYE : Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(showAllGroups ? "Viewing: All Groups" : "Viewing: My Groups", NamedTextColor.LIGHT_PURPLE)
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

    public void handleClick(int slot, boolean isRightClick, boolean isShiftClick, boolean isMiddleClick) {
        if (slot == 48 && currentPage > 0) {
            currentPage--;
            populateInventory();
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) groups.size() / ITEMS_PER_PAGE));
        if (slot == 50 && currentPage < totalPages - 1) {
            currentPage++;
            populateInventory();
            return;
        }

        if (slot == 45 && player.hasPermission("wirelessredstone.admin")) {
            showAllGroups = !showAllGroups;
            currentPage = 0;
            refreshGroups();
            populateInventory();
            return;
        }

        if (slot == 53) {
            player.closeInventory();
            return;
        }

        int groupIndex = getGroupIndexFromSlot(slot);
        if (groupIndex < 0 || groupIndex >= groups.size()) {
            return;
        }

        GroupEntry group = groups.get(groupIndex);

        if (isShiftClick && isRightClick) {
            handleSetIcon(group);
        } else if (isShiftClick) {
            handleRemoveGroup(group);
        } else if (isMiddleClick) {
            handleStartRename(group);
        } else if (isRightClick) {
            List<Location> placed = group.getPlacedLocations();
            if (!placed.isEmpty()) {
                handleTeleport(placed.get(placed.size() - 1), GroupEntry.getIndexLabel(group.getLocationIndex(placed.get(placed.size() - 1))));
            } else {
                player.sendMessage(Component.text("No items are placed in this group!", NamedTextColor.RED));
            }
        } else {
            List<Location> placed = group.getPlacedLocations();
            if (!placed.isEmpty()) {
                handleTeleport(placed.get(0), GroupEntry.getIndexLabel(group.getLocationIndex(placed.get(0))));
            } else {
                player.sendMessage(Component.text("No items are placed in this group!", NamedTextColor.RED));
            }
        }
    }

    private void handleStartRename(GroupEntry group) {
        pendingRenames.put(player.getUniqueId(), group);
        player.closeInventory();
        player.sendMessage(Component.text("Enter a new name for the group (or 'cancel' to abort):", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Current name: " + group.getDisplayName(), NamedTextColor.GRAY));
    }

    private void handleSetIcon(GroupEntry group) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR || heldItem.getType() == null) {
            group.setCustomIcon(null);
            player.sendMessage(Component.text("Group icon reset to default!", NamedTextColor.YELLOW));
        } else {
            group.setCustomIcon(heldItem.getType());
            player.sendMessage(Component.text("Group icon set to ", NamedTextColor.GREEN)
                    .append(Component.text(heldItem.getType().name(), NamedTextColor.AQUA))
                    .append(Component.text("!", NamedTextColor.GREEN)));
        }
        
        if (group.getType() == GroupEntry.GroupType.BULB) {
            bulbManager.saveData();
        } else if (chestManager != null) {
            chestManager.saveData();
        }
        populateInventory();
    }

    public static boolean hasPendingRename(UUID playerUuid) {
        return pendingRenames.containsKey(playerUuid);
    }

    public static void processRename(Player player, String newName, LinkedBulbManager bulbManager, LinkedChestManager chestManager) {
        GroupEntry group = pendingRenames.remove(player.getUniqueId());
        if (group == null) return;

        if (newName.equalsIgnoreCase("cancel")) {
            player.sendMessage(Component.text("Rename cancelled.", NamedTextColor.GRAY));
            return;
        }

        if (newName.length() > 32) {
            newName = newName.substring(0, 32);
        }

        if (newName.equalsIgnoreCase("reset") || newName.equalsIgnoreCase("clear")) {
            group.setCustomName(null);
            player.sendMessage(Component.text("Group name reset to default: " + group.getDisplayName(), NamedTextColor.GREEN));
        } else {
            group.setCustomName(newName);
            player.sendMessage(Component.text("Group renamed to: " + newName, NamedTextColor.GREEN));
        }
        
        if (group.getType() == GroupEntry.GroupType.BULB) {
            bulbManager.saveData();
        } else if (chestManager != null) {
            chestManager.saveData();
        }
    }

    public static void cancelPendingRename(UUID playerUuid) {
        pendingRenames.remove(playerUuid);
    }

    private int getGroupIndexFromSlot(int slot) {
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

    private void handleRemoveGroup(GroupEntry group) {
        if (!player.hasPermission("wirelessredstone.remove")) {
            player.sendMessage(Component.text("You don't have permission to remove groups!", NamedTextColor.RED));
            return;
        }

        UUID ownerId = group.getOwnerUuid();
        if (ownerId != null && !ownerId.equals(player.getUniqueId()) && !player.hasPermission("wirelessredstone.admin")) {
            player.sendMessage(Component.text("You can only remove your own groups!", NamedTextColor.RED));
            return;
        }

        if (group.getType() == GroupEntry.GroupType.BULB) {
            bulbManager.removeGroup(group.getGroupId());
        } else if (chestManager != null) {
            chestManager.removeGroup(group.getGroupId());
        }
        player.sendMessage(Component.text("Group removed successfully!", NamedTextColor.GREEN));
        
        refreshGroups();
        
        int totalPages = Math.max(1, (int) Math.ceil((double) groups.size() / ITEMS_PER_PAGE));
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
