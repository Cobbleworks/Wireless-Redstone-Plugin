package com.wirelessredstone.gui;

import com.wirelessredstone.manager.CategoryManager;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.item.CircuitAnalyserFactory;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.model.Category;
import com.wirelessredstone.model.ChestGroup;
import com.wirelessredstone.util.GroupNameParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
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
    private static final Map<UUID, GroupEntry> pendingCategoryChanges = new HashMap<>();

    private final LinkedBulbManager bulbManager;
    private final LinkedChestManager chestManager;
    private final CategoryManager categoryManager;
    private final Player player;
    private final Inventory inventory;
    private final String categoryName;
    private int currentPage = 0;
    private List<GroupEntry> groups;
    private List<GuiEntry> entries;
    private boolean showAllGroups;

    private interface GuiEntry {}
    private record CategoryEntry(String displayName, String key, int bulbCount, int chestCount, List<String> groupNames) implements GuiEntry {}
    private record GroupGuiEntry(GroupEntry group) implements GuiEntry {}

    public BulbManagerGUI(LinkedBulbManager bulbManager, LinkedChestManager chestManager, Player player, boolean showAllGroups) {
        this(bulbManager, chestManager, null, player, showAllGroups, null);
    }

    public BulbManagerGUI(LinkedBulbManager bulbManager, LinkedChestManager chestManager, CategoryManager categoryManager, 
                          Player player, boolean showAllGroups, String categoryName) {
        this.bulbManager = bulbManager;
        this.chestManager = chestManager;
        this.categoryManager = categoryManager;
        this.player = player;
        this.showAllGroups = showAllGroups;
        this.categoryName = categoryName;
        
        String title = categoryName == null ? "Wireless Redstone Manager" : categoryName;
        
        this.inventory = Bukkit.createInventory(this, SIZE, 
            Component.text(title, NamedTextColor.DARK_AQUA).decoration(TextDecoration.BOLD, true));
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

        groups.sort(Comparator.comparing(GroupEntry::getGroupDisplayName, String.CASE_INSENSITIVE_ORDER));

        entries = new ArrayList<>();
        if (categoryName == null) {
            Map<String, CategoryEntry> categoriesByKey = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (GroupEntry group : groups) {
                if (group.getCategoryName() == null) {
                    continue;
                }

                String key = group.getCategoryKey();
                CategoryEntry existing = categoriesByKey.get(key);
                int bulbCount = existing == null ? 0 : existing.bulbCount();
                int chestCount = existing == null ? 0 : existing.chestCount();
                List<String> groupNames = existing == null ? new ArrayList<>() : new ArrayList<>(existing.groupNames());
                if (group.getType() == GroupEntry.GroupType.BULB) {
                    bulbCount++;
                } else {
                    chestCount++;
                }
                groupNames.add(group.getGroupDisplayName());
                groupNames.sort(String.CASE_INSENSITIVE_ORDER);
                categoriesByKey.put(key, new CategoryEntry(group.getCategoryName(), key, bulbCount, chestCount, groupNames));
            }
            entries.addAll(categoriesByKey.values());
            groups.stream()
                    .filter(group -> group.getCategoryName() == null)
                    .forEach(group -> entries.add(new GroupGuiEntry(group)));
            return;
        }

        String targetKey = GroupNameParser.normalizeCategoryKey(categoryName);
        groups = groups.stream()
                .filter(group -> targetKey.equals(group.getCategoryKey()))
                .toList();
        groups.forEach(group -> entries.add(new GroupGuiEntry(group)));
    }

    private void populateInventory() {
        inventory.clear();

        fillBorder();

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, entries.size());

        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;

            GuiEntry entry = entries.get(i);
            if (entry instanceof CategoryEntry categoryEntry) {
                inventory.setItem(slot, createCategoryItem(categoryEntry));
            } else if (entry instanceof GroupGuiEntry groupEntry) {
                inventory.setItem(slot, createGroupItem(groupEntry.group()));
            }
            slot++;
        }

        if (currentPage > 0) {
            inventory.setItem(48, createNavigationItem(Material.ARROW, "Previous Page", NamedTextColor.YELLOW));
        }

        inventory.setItem(49, createInfoItem());

        if (endIndex < entries.size()) {
            inventory.setItem(50, createNavigationItem(Material.ARROW, "Next Page", NamedTextColor.YELLOW));
        }

        if (player.hasPermission("wirelessredstone.admin")) {
            inventory.setItem(45, createToggleViewItem());
        }

        inventory.setItem(46, createConnectorToolItem());
        inventory.setItem(47, createCircuitAnalyserItem());

        if (categoryName != null) {
            inventory.setItem(52, createBackToAllGroupsItem());
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

        String displayName = group.getGroupDisplayName();
        NamedTextColor typeColor = group.getType() == GroupEntry.GroupType.BULB ? NamedTextColor.AQUA : NamedTextColor.GOLD;
        meta.displayName(Component.text(displayName, typeColor)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        if (categoryName == null && group.getCategoryName() != null) {
            lore.add(Component.text("Category: ", NamedTextColor.GRAY)
                    .append(Component.text(group.getCategoryName(), NamedTextColor.YELLOW))
                    .decoration(TextDecoration.ITALIC, false));
        }
        
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
                .append(Component.text("Show group details", NamedTextColor.WHITE))
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

    private ItemStack createCategoryItem(CategoryEntry category) {
        ItemStack item = new ItemStack(Category.DEFAULT_ICON);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(category.displayName(), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Category", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Bulb Groups: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(category.bulbCount()), NamedTextColor.AQUA))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Container Groups: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(category.chestCount()), NamedTextColor.GOLD))
                .decoration(TextDecoration.ITALIC, false));
        if (!category.groupNames().isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.text("Groups:", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            int shown = 0;
            for (String groupName : category.groupNames()) {
                if (shown >= 8) {
                    break;
                }
                lore.add(Component.text("- ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(groupName, NamedTextColor.WHITE))
                        .decoration(TextDecoration.ITALIC, false));
                shown++;
            }
            int remaining = category.groupNames().size() - shown;
            if (remaining > 0) {
                lore.add(Component.text("... and " + remaining + " more", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }
        lore.add(Component.empty());
        lore.add(Component.text("Click to view", NamedTextColor.YELLOW)
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
        
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / ITEMS_PER_PAGE));
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

    private ItemStack createBackToAllGroupsItem() {
        ItemStack item = new ItemStack(Material.DARK_OAK_DOOR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Back to All Groups", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Click to return to the full list", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createConnectorToolItem() {
        ItemStack item = new ItemStack(Material.SHEARS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Get Connector Tool", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        meta.lore(List.of(
                Component.text("Click to create a connector", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("tool for a new/existing group", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCircuitAnalyserItem() {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Get Circuit Analyser", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        meta.lore(List.of(
                Component.text("Click to receive the analyser", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
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
        handleClick(slot, isRightClick, isShiftClick, isMiddleClick, false);
    }

    public void handleClick(int slot, boolean isRightClick, boolean isShiftClick, boolean isMiddleClick, boolean isDrop) {
        if (slot == 48 && currentPage > 0) {
            currentPage--;
            populateInventory();
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / ITEMS_PER_PAGE));
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

        if (slot == 52 && categoryName != null) {
            BulbManagerGUI categoryGUI = new BulbManagerGUI(bulbManager, chestManager, categoryManager, player, showAllGroups, null);
            player.closeInventory();
            categoryGUI.open();
            return;
        }

        if (slot == 53) {
            player.closeInventory();
            return;
        }

        if (slot == 46) {
            CategorySelectionGUI.startConnectorToolPrompt(player, categoryName);
            return;
        }

        if (slot == 47) {
            giveItemToPlayer(CircuitAnalyserFactory.createCircuitAnalyser());
            player.sendMessage(Component.text("You received a Circuit Analyser!", NamedTextColor.GREEN));
            return;
        }

        int entryIndex = getEntryIndexFromSlot(slot);
        if (entryIndex < 0 || entryIndex >= entries.size()) {
            return;
        }

        GuiEntry entry = entries.get(entryIndex);
        if (entry instanceof CategoryEntry categoryEntry) {
            BulbManagerGUI categoryGUI = new BulbManagerGUI(bulbManager, chestManager, categoryManager, player, showAllGroups, categoryEntry.displayName());
            player.closeInventory();
            categoryGUI.open();
            return;
        }

        if (!(entry instanceof GroupGuiEntry groupEntry)) {
            return;
        }

        GroupEntry group = groupEntry.group();

        if (isShiftClick && isRightClick) {
            handleSetIcon(group);
        } else if (isShiftClick) {
            handleRemoveGroup(group);
        } else if (isMiddleClick) {
            handleStartRename(group);
        } else if (isRightClick) {
            handleShowDetails(group);
        } else {
            List<Location> placed = group.getPlacedLocations();
            if (!placed.isEmpty()) {
                handleTeleport(placed.get(0), GroupEntry.getIndexLabel(group.getLocationIndex(placed.get(0))));
            } else {
                player.sendMessage(Component.text("No items are placed in this group!", NamedTextColor.RED));
            }
        }
    }

    private void handleShowDetails(GroupEntry group) {
        player.closeInventory();

        NamedTextColor typeColor = group.getType() == GroupEntry.GroupType.BULB
                ? NamedTextColor.AQUA
                : NamedTextColor.GOLD;
        String groupType = group.getType() == GroupEntry.GroupType.BULB ? "bulb" : "chest";
        UUID groupId = group.getGroupId();

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("═══ ⚡ Wireless Group ⚡ ═══", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true));

        String fullName = group.getDisplayName();
        String displayName = group.getGroupDisplayName();
        player.sendMessage(Component.text("Name: ", NamedTextColor.GRAY)
                .append(Component.text(displayName, NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true)
                        .hoverEvent(HoverEvent.showText(Component.text("Click to rename", NamedTextColor.YELLOW)))
                        .clickEvent(ClickEvent.runCommand("/wireless analyser-rename " + groupId + " " + groupType)))
                .append(Component.text(" ✎", NamedTextColor.DARK_GRAY)));

        player.sendMessage(Component.text("Category: ", NamedTextColor.GRAY)
                .append(Component.text(getCategoryDisplayName(group), NamedTextColor.YELLOW)));

        player.sendMessage(Component.text("Type: ", NamedTextColor.GRAY)
                .append(Component.text(group.getTypeDisplayName(), typeColor)));

        NamedTextColor countColor = group.getPlacedCount() == group.getMaxSize()
                ? NamedTextColor.GREEN
                : NamedTextColor.YELLOW;
        Component placedLine = Component.text("Placed: ", NamedTextColor.GRAY)
                .append(Component.text(group.getPlacedCount() + "/" + group.getMaxSize(), countColor));
        if (group.getType() == GroupEntry.GroupType.BULB) {
            placedLine = placedLine
                    .append(Component.text(" | State: ", NamedTextColor.GRAY))
                    .append(Component.text(group.isLit() ? "ON" : "OFF",
                            group.isLit() ? NamedTextColor.GREEN : NamedTextColor.RED));
        }
        player.sendMessage(placedLine);

        player.sendMessage(Component.text("[Get Connector Tool]", NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .hoverEvent(HoverEvent.showText(Component.text("Click to receive a Connector Tool for this group", NamedTextColor.YELLOW)))
                .clickEvent(ClickEvent.runCommand("/wireless create " + quoteCommandArgument(fullName))));

        player.sendMessage(Component.text("Blocks:", NamedTextColor.GRAY)
                .decoration(TextDecoration.UNDERLINED, true));

        List<Location> locations = group.getLocations();
        for (int i = 0; i < locations.size(); i++) {
            Location loc = locations.get(i);
            String label = GroupEntry.getIndexLabel(i);
            if (loc == null) {
                player.sendMessage(Component.text("[" + label + "] ", NamedTextColor.DARK_AQUA)
                        .append(Component.text("Not placed", NamedTextColor.RED)));
                continue;
            }

            String coords = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            String command = "/wireless teleport " + groupId + " " + groupType + " " + i;
            player.sendMessage(Component.text("[" + label + "] ", NamedTextColor.DARK_AQUA)
                    .append(Component.text(coords, NamedTextColor.WHITE)
                            .hoverEvent(HoverEvent.showText(Component.text("Click to teleport", NamedTextColor.YELLOW)))
                            .clickEvent(ClickEvent.runCommand(command))));
        }

        player.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.DARK_GRAY));
    }

    private String getCategoryDisplayName(GroupEntry group) {
        return group.getCategoryName() == null ? "Uncategorized" : group.getCategoryName();
    }

    private String quoteCommandArgument(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private void handleStartRename(GroupEntry group) {
        pendingRenames.put(player.getUniqueId(), group);
        player.closeInventory();
        player.sendMessage(Component.text("Enter a new name for the group (or 'cancel' to abort):", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Use category/group-name to create or move it into a category.", NamedTextColor.GRAY));
        player.sendMessage(Component.text("Current name: " + group.getDisplayName(), NamedTextColor.GRAY));
    }

    private void handleChangeCategory(GroupEntry group) {
        player.sendMessage(Component.text("Rename the group with a prefix like factory/" + group.getGroupDisplayName() + " to set its category.", NamedTextColor.YELLOW));
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

    public static void processRename(Player player, String newName, LinkedBulbManager bulbManager, 
                                       LinkedChestManager chestManager, CategoryManager categoryManager) {
        GroupEntry group = pendingRenames.remove(player.getUniqueId());
        if (group == null) return;

        if (newName.equalsIgnoreCase("cancel")) {
            player.sendMessage(Component.text("Rename cancelled.", NamedTextColor.GRAY));
            reopenCategoryGUI(player, bulbManager, chestManager, categoryManager);
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
        
        reopenCategoryGUI(player, bulbManager, chestManager, categoryManager);
    }

    public static void cancelPendingRename(UUID playerUuid) {
        pendingRenames.remove(playerUuid);
    }

    public static boolean hasPendingCategoryChange(UUID playerUuid) {
        return pendingCategoryChanges.containsKey(playerUuid);
    }

    public static void processCategoryChange(Player player, String input, LinkedBulbManager bulbManager, 
                                              LinkedChestManager chestManager, CategoryManager categoryManager) {
        GroupEntry group = pendingCategoryChanges.remove(player.getUniqueId());
        if (group == null) return;

        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(Component.text("Category change cancelled.", NamedTextColor.GRAY));
            reopenCategoryGUI(player, bulbManager, chestManager, categoryManager);
            return;
        }

        if (input.equalsIgnoreCase("none") || input.equalsIgnoreCase("uncategorized")) {
            group.setCategoryId(null);
            player.sendMessage(Component.text("Group moved to Uncategorized.", NamedTextColor.GREEN));
        } else {
            List<Category> playerCategories = categoryManager.getCategoriesByOwner(player.getUniqueId());
            Category targetCategory = null;
            
            // Try to parse as number
            try {
                int index = Integer.parseInt(input);
                if (index >= 1 && index <= playerCategories.size()) {
                    targetCategory = playerCategories.get(index - 1);
                }
            } catch (NumberFormatException ignored) {}
            
            // Try to find by name
            if (targetCategory == null) {
                for (Category cat : playerCategories) {
                    if (cat.getName().equalsIgnoreCase(input) || cat.getDisplayName().equalsIgnoreCase(input)) {
                        targetCategory = cat;
                        break;
                    }
                }
            }
            
            if (targetCategory == null) {
                player.sendMessage(Component.text("Category not found: " + input, NamedTextColor.RED));
                reopenCategoryGUI(player, bulbManager, chestManager, categoryManager);
                return;
            }
            
            group.setCategoryId(targetCategory.getCategoryId());
            player.sendMessage(Component.text("Group moved to category: " + targetCategory.getDisplayName(), NamedTextColor.GREEN));
        }
        
        if (group.getType() == GroupEntry.GroupType.BULB) {
            bulbManager.saveData();
        } else if (chestManager != null) {
            chestManager.saveData();
        }
        
        reopenCategoryGUI(player, bulbManager, chestManager, categoryManager);
    }

    private static void reopenCategoryGUI(Player player, LinkedBulbManager bulbManager, 
                                           LinkedChestManager chestManager, CategoryManager categoryManager) {
        // Schedule to run on next tick to avoid issues with chat event handling
        player.getServer().getScheduler().runTask(
            player.getServer().getPluginManager().getPlugin("WirelessRedstone"),
            () -> new BulbManagerGUI(bulbManager, chestManager, categoryManager, player, false, null).open()
        );
    }

    public static void cancelPendingCategoryChange(UUID playerUuid) {
        pendingCategoryChanges.remove(playerUuid);
    }

    private int getEntryIndexFromSlot(int slot) {
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

    private void giveItemToPlayer(ItemStack item) {
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
    }
}
