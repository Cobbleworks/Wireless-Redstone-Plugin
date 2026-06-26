package com.wirelessredstone.gui;

import com.wirelessredstone.manager.CategoryManager;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.model.Category;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI for assigning a group to a category.
 * Shows all player's categories and allows clicking to assign.
 */
public class CategoryAssignmentGUI implements InventoryHolder {

    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;
    private static final int ITEMS_PER_PAGE = 28;

    private final CategoryManager categoryManager;
    private final LinkedBulbManager bulbManager;
    private final LinkedChestManager chestManager;
    private final Player player;
    private final Inventory inventory;
    private final GroupEntry targetGroup;
    private final UUID returnToCategoryId;
    private int currentPage = 0;
    private List<Category> categories;

    public CategoryAssignmentGUI(CategoryManager categoryManager, LinkedBulbManager bulbManager,
                                  LinkedChestManager chestManager, Player player,
                                  GroupEntry targetGroup, UUID returnToCategoryId) {
        this.categoryManager = categoryManager;
        this.bulbManager = bulbManager;
        this.chestManager = chestManager;
        this.player = player;
        this.targetGroup = targetGroup;
        this.returnToCategoryId = returnToCategoryId;
        
        String title = "Assign Category: " + truncate(targetGroup.getDisplayName(), 20);
        this.inventory = Bukkit.createInventory(this, SIZE,
            Component.text(title, NamedTextColor.DARK_PURPLE).decoration(TextDecoration.BOLD, true));
        refreshCategories();
        populateInventory();
    }

    private String truncate(String str, int maxLength) {
        return str.length() <= maxLength ? str : str.substring(0, maxLength - 3) + "...";
    }

    private void refreshCategories() {
        categories = categoryManager.getCategoriesByOwner(player.getUniqueId());
        if (player.hasPermission("wirelessredstone.admin")) {
            // Admins can see all categories
            categories = categoryManager.getAllCategories();
        }
    }

    private void populateInventory() {
        inventory.clear();
        fillBorder();

        // Uncategorized option at top center
        inventory.setItem(4, createUncategorizedItem());

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, categories.size());

        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;

            Category category = categories.get(i);
            inventory.setItem(slot, createCategoryItem(category));
            slot++;
        }

        // Navigation
        if (currentPage > 0) {
            inventory.setItem(48, createNavigationItem(Material.ARROW, "Previous Page", NamedTextColor.YELLOW));
        }

        inventory.setItem(49, createInfoItem());

        if (endIndex < categories.size()) {
            inventory.setItem(50, createNavigationItem(Material.ARROW, "Next Page", NamedTextColor.YELLOW));
        }

        // Back / Close
        inventory.setItem(52, createBackItem());
        inventory.setItem(53, createCloseItem());
    }

    private void fillBorder() {
        ItemStack border = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.empty());
        border.setItemMeta(meta);

        for (int i = 0; i < 9; i++) {
            if (i != 4) inventory.setItem(i, border);
        }
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }

    private ItemStack createCategoryItem(Category category) {
        Material material = category.getIcon() != null ? category.getIcon() : Category.DEFAULT_ICON;
        boolean isCurrentCategory = category.getCategoryId().equals(targetGroup.getCategoryId());

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        NamedTextColor nameColor = isCurrentCategory ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
        meta.displayName(Component.text(category.getDisplayName(), nameColor)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, isCurrentCategory));

        int bulbCount = countGroupsInCategory(category.getCategoryId(), true);
        int chestCount = countGroupsInCategory(category.getCategoryId(), false);

        List<Component> lore = new ArrayList<>();
        if (isCurrentCategory) {
            lore.add(Component.text("✓ Current category", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
        }
        lore.add(Component.text("Bulb Groups: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(bulbCount), NamedTextColor.AQUA))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Container Groups: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(chestCount), NamedTextColor.GOLD))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Click to assign", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createUncategorizedItem() {
        boolean isCurrentCategory = targetGroup.getCategoryId() == null;
        
        ItemStack item = new ItemStack(Material.GRAY_SHULKER_BOX);
        ItemMeta meta = item.getItemMeta();

        NamedTextColor nameColor = isCurrentCategory ? NamedTextColor.GREEN : NamedTextColor.GRAY;
        meta.displayName(Component.text("Uncategorized", nameColor)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, isCurrentCategory));

        int bulbCount = countGroupsInCategory(null, true);
        int chestCount = countGroupsInCategory(null, false);

        List<Component> lore = new ArrayList<>();
        if (isCurrentCategory) {
            lore.add(Component.text("✓ Current category", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
        }
        lore.add(Component.text("Groups without a category", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Bulb Groups: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(bulbCount), NamedTextColor.AQUA))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Container Groups: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(chestCount), NamedTextColor.GOLD))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Click to remove from category", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private int countGroupsInCategory(UUID categoryId, boolean bulbs) {
        if (bulbs) {
            return (int) bulbManager.getAllGroups().stream()
                    .filter(g -> Objects.equals(g.getCategoryId(), categoryId))
                    .filter(g -> player.hasPermission("wirelessredstone.admin") || player.getUniqueId().equals(g.getOwnerUuid()))
                    .count();
        } else {
            return chestManager == null ? 0 : (int) chestManager.getAllGroups().stream()
                    .filter(g -> Objects.equals(g.getCategoryId(), categoryId))
                    .filter(g -> player.hasPermission("wirelessredstone.admin") || player.getUniqueId().equals(g.getOwnerUuid()))
                    .count();
        }
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

        int totalPages = Math.max(1, (int) Math.ceil((double) categories.size() / ITEMS_PER_PAGE));
        meta.lore(List.of(
                Component.text("Page " + (currentPage + 1) + "/" + totalPages, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Categories: " + categories.size(), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("← Back", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Return without changes", NamedTextColor.GRAY)
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

    public void handleClick(int slot) {
        // Navigation
        if (slot == 48 && currentPage > 0) {
            currentPage--;
            populateInventory();
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) categories.size() / ITEMS_PER_PAGE));
        if (slot == 50 && currentPage < totalPages - 1) {
            currentPage++;
            populateInventory();
            return;
        }

        // Back
        if (slot == 52) {
            returnToGroupsGUI();
            return;
        }

        // Close
        if (slot == 53) {
            player.closeInventory();
            return;
        }

        // Uncategorized (remove from category)
        if (slot == 4) {
            assignCategory(null);
            return;
        }

        // Category selection
        int categoryIndex = getCategoryIndexFromSlot(slot);
        if (categoryIndex < 0 || categoryIndex >= categories.size()) {
            return;
        }

        Category category = categories.get(categoryIndex);
        assignCategory(category.getCategoryId());
    }

    private void assignCategory(UUID categoryId) {
        String oldCategoryName = targetGroup.getCategoryId() != null ?
                categoryManager.getCategoryById(targetGroup.getCategoryId())
                        .map(Category::getDisplayName).orElse("Unknown") :
                "Uncategorized";
        
        String newCategoryName = categoryId != null ?
                categoryManager.getCategoryById(categoryId)
                        .map(Category::getDisplayName).orElse("Unknown") :
                "Uncategorized";

        targetGroup.setCategoryId(categoryId);

        // Save the data
        if (targetGroup.getType() == GroupEntry.GroupType.BULB) {
            bulbManager.saveData();
        } else if (chestManager != null) {
            chestManager.saveData();
        }

        player.sendMessage(Component.text("Group ", NamedTextColor.GREEN)
                .append(Component.text(targetGroup.getDisplayName(), NamedTextColor.AQUA))
                .append(Component.text(" moved to ", NamedTextColor.GREEN))
                .append(Component.text(newCategoryName, NamedTextColor.YELLOW)));

        // Return to the category's group view (the new category, so user sees the group there)
        returnToGroupsGUI(categoryId);
    }

    private void returnToGroupsGUI() {
        returnToGroupsGUI(returnToCategoryId);
    }

    private void returnToGroupsGUI(UUID categoryId) {
        player.closeInventory();
        String categoryName = categoryId == null
                ? null
                : categoryManager.getCategoryById(categoryId).map(Category::getName).orElse(null);
        player.getServer().getScheduler().runTask(
                player.getServer().getPluginManager().getPlugin("WirelessRedstone"),
                () -> new BulbManagerGUI(bulbManager, chestManager, categoryManager, player, false, categoryName).open()
        );
    }

    private int getCategoryIndexFromSlot(int slot) {
        if (slot < 10 || slot > 43) return -1;
        if (slot % 9 == 0 || slot % 9 == 8) return -1;

        int row = slot / 9 - 1;
        int col = slot % 9 - 1;
        int indexInPage = row * 7 + col;

        return currentPage * ITEMS_PER_PAGE + indexInPage;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
    }
}
