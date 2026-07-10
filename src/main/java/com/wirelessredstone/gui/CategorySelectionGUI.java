package com.wirelessredstone.gui;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.manager.CategoryManager;
import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.manager.WireViewManager;
import com.wirelessredstone.item.ConnectorToolFactory;
import com.wirelessredstone.model.BaseGroup;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.model.Category;
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

public class CategorySelectionGUI implements InventoryHolder {

    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;
    private static final int ITEMS_PER_PAGE = 36;
    
    private static final Map<UUID, PendingAction> pendingActions = new HashMap<>();
    private static final Map<UUID, String> pendingConnectorCategoryNames = new HashMap<>();

    public enum PendingActionType {
        RENAME_CATEGORY,
        CREATE_CATEGORY,
        SET_CATEGORY_DESCRIPTION,
        CREATE_CONNECTOR_TOOL
    }

    public record PendingAction(PendingActionType type, UUID categoryId) {}

    private final CategoryManager categoryManager;
    private final LinkedBulbManager bulbManager;
    private final LinkedChestManager chestManager;
    private final Player player;
    private final Inventory inventory;
    private int currentPage = 0;
    private List<Category> categories;
    private boolean showAllCategories;

    public CategorySelectionGUI(CategoryManager categoryManager, LinkedBulbManager bulbManager, 
                                 LinkedChestManager chestManager, Player player, boolean showAllCategories) {
        this.categoryManager = categoryManager;
        this.bulbManager = bulbManager;
        this.chestManager = chestManager;
        this.player = player;
        this.showAllCategories = showAllCategories;
        this.inventory = Bukkit.createInventory(this, SIZE, 
            Component.text("Select Category", NamedTextColor.DARK_GREEN).decoration(TextDecoration.BOLD, true));
        refreshCategories();
        populateInventory();
    }

    private void refreshCategories() {
        if (showAllCategories && player.hasPermission("wirelessredstone.admin")) {
            categories = categoryManager.getAllCategories();
        } else {
            categories = categoryManager.getCategoriesByOwner(player.getUniqueId());
        }
    }

    private void populateInventory() {
        inventory.clear();

        fillBorder();

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, categories.size());

        for (int i = startIndex; i < endIndex; i++) {
            Category category = categories.get(i);
            int slot = i - startIndex;
            inventory.setItem(slot, createCategoryItem(category));
        }

        // Uncategorized option
        inventory.setItem(52, createUncategorizedItem());

        if (currentPage > 0) {
            inventory.setItem(48, createNavigationItem(Material.ARROW, "Previous Page", NamedTextColor.YELLOW));
        }

        inventory.setItem(49, createInfoItem());

        if (endIndex < categories.size()) {
            inventory.setItem(50, createNavigationItem(Material.ARROW, "Next Page", NamedTextColor.YELLOW));
        }

        if (player.hasPermission("wirelessredstone.admin")) {
            inventory.setItem(45, createToggleViewItem());
        }

        inventory.setItem(46, createConnectorToolItem());

        // Create category button
        inventory.setItem(51, createNewCategoryItem());

        inventory.setItem(53, createCloseItem());
    }

    private void fillBorder() {
        ItemStack border = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.empty());
        border.setItemMeta(meta);

        for (int i = 36; i < 54; i++) {
            inventory.setItem(i, border);
        }
    }

    private ItemStack createCategoryItem(Category category) {
        Material material = category.getIcon() != null ? category.getIcon() : Category.DEFAULT_ICON;
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(category.getDisplayName(), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        int bulbCount = countGroupsInCategory(category.getCategoryId(), true);
        int chestCount = countGroupsInCategory(category.getCategoryId(), false);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (category.getDescription() != null) {
            lore.add(Component.text(category.getDescription(), NamedTextColor.GRAY)
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
        lore.add(Component.text("Left-click: ", NamedTextColor.YELLOW)
                .append(Component.text("View groups", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Right-click: ", NamedTextColor.YELLOW)
                .append(Component.text("Set description", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Middle-click: ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text("Rename category", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shift+Right: ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text("Set icon to held item", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shift+Left: ", NamedTextColor.RED)
                .append(Component.text("Delete category", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createConnectorToolItem() {
        ItemStack item = new ItemStack(Material.SHEARS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Get Circuit Tool", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        meta.lore(List.of(
                Component.text("Click to create a circuit", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("tool for a new/existing group", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createUncategorizedItem() {
        ItemStack item = new ItemStack(Material.GRAY_SHULKER_BOX);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Uncategorized", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        int bulbCount = countGroupsInCategory(null, true);
        int chestCount = countGroupsInCategory(null, false);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
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
        lore.add(Component.text("Click to view", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNewCategoryItem() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Create New Category", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Click to create a new category", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private int countGroupsInCategory(UUID categoryId, boolean bulbs) {
        if (bulbs) {
            return (int) bulbManager.getAllGroups().stream()
                    .filter(g -> Objects.equals(g.getCategoryId(), categoryId))
                    .filter(g -> showAllCategories || player.getUniqueId().equals(g.getOwnerUuid()))
                    .count();
        } else {
            return chestManager == null ? 0 : (int) chestManager.getAllGroups().stream()
                    .filter(g -> Objects.equals(g.getCategoryId(), categoryId))
                    .filter(g -> showAllCategories || player.getUniqueId().equals(g.getOwnerUuid()))
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
                Component.text("Total categories: " + categories.size(), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createToggleViewItem() {
        ItemStack item = new ItemStack(showAllCategories ? Material.ENDER_EYE : Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(showAllCategories ? "Viewing: All Categories" : "Viewing: My Categories", NamedTextColor.LIGHT_PURPLE)
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

    public void handleClick(int slot, boolean isRightClick, boolean isShiftClick, boolean isMiddleClick) {
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

        // Toggle view
        if (slot == 45 && player.hasPermission("wirelessredstone.admin")) {
            showAllCategories = !showAllCategories;
            currentPage = 0;
            refreshCategories();
            populateInventory();
            return;
        }

        // Create new category
        if (slot == 51) {
            handleCreateCategory();
            return;
        }

        // Close
        if (slot == 53) {
            player.closeInventory();
            return;
        }

        if (slot == 46) {
            startConnectorToolPrompt(player, null, categoryManager);
            return;
        }

        // Uncategorized
        if (slot == 52) {
            openGroupsGUI(null);
            return;
        }

        // Category selection
        int categoryIndex = getCategoryIndexFromSlot(slot);
        if (categoryIndex < 0 || categoryIndex >= categories.size()) {
            return;
        }

        Category category = categories.get(categoryIndex);

        if (isShiftClick && isRightClick) {
            handleSetIcon(category);
        } else if (isShiftClick) {
            handleDeleteCategory(category);
        } else if (isMiddleClick) {
            handleRenameCategory(category);
        } else if (isRightClick) {
            handleSetDescription(category);
        } else {
            openGroupsGUI(category.getCategoryId());
        }
    }

    private void handleCreateCategory() {
        pendingActions.put(player.getUniqueId(), new PendingAction(PendingActionType.CREATE_CATEGORY, null));
        player.closeInventory();
        player.sendMessage(Component.text("Enter a name for the new category (or 'cancel' to abort):", NamedTextColor.YELLOW));
    }

    private void handleRenameCategory(Category category) {
        pendingActions.put(player.getUniqueId(), new PendingAction(PendingActionType.RENAME_CATEGORY, category.getCategoryId()));
        player.closeInventory();
        player.sendMessage(Component.text("Enter a new name for the category (or 'cancel' to abort):", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Current name: " + category.getDisplayName(), NamedTextColor.GRAY));
    }

    private void handleSetDescription(Category category) {
        pendingActions.put(player.getUniqueId(), new PendingAction(PendingActionType.SET_CATEGORY_DESCRIPTION, category.getCategoryId()));
        player.closeInventory();
        player.sendMessage(Component.text("Enter a category description (or 'clear' to remove, 'cancel' to abort):", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Current description: " + (category.getDescription() == null ? "None" : category.getDescription()), NamedTextColor.GRAY));
    }

    private void handleSetIcon(Category category) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR || heldItem.getType() == null) {
            categoryManager.setCategoryIcon(category.getCategoryId(), null);
            player.sendMessage(Component.text("Category icon reset to default!", NamedTextColor.YELLOW));
        } else {
            categoryManager.setCategoryIcon(category.getCategoryId(), heldItem.getType());
            player.sendMessage(Component.text("Category icon set to ", NamedTextColor.GREEN)
                    .append(Component.text(heldItem.getType().name(), NamedTextColor.AQUA))
                    .append(Component.text("!", NamedTextColor.GREEN)));
        }
        populateInventory();
    }

    private void handleDeleteCategory(Category category) {
        UUID ownerId = category.getOwnerUuid();
        if (ownerId != null && !ownerId.equals(player.getUniqueId()) && !player.hasPermission("wirelessredstone.admin")) {
            player.sendMessage(Component.text("You can only delete your own categories!", NamedTextColor.RED));
            return;
        }

        // Move all groups in this category to uncategorized
        UUID categoryId = category.getCategoryId();
        bulbManager.getAllGroups().stream()
                .filter(g -> categoryId.equals(g.getCategoryId()))
                .forEach(g -> g.setCategoryId(null));
        if (chestManager != null) {
            chestManager.getAllGroups().stream()
                    .filter(g -> categoryId.equals(g.getCategoryId()))
                    .forEach(g -> g.setCategoryId(null));
        }
        bulbManager.saveData();
        if (chestManager != null) chestManager.saveData();

        categoryManager.deleteCategory(categoryId);
        player.sendMessage(Component.text("Category deleted! Groups moved to Uncategorized.", NamedTextColor.GREEN));
        
        refreshCategories();
        int newTotalPages = Math.max(1, (int) Math.ceil((double) categories.size() / ITEMS_PER_PAGE));
        if (currentPage >= newTotalPages) {
            currentPage = Math.max(0, newTotalPages - 1);
        }
        populateInventory();
    }

    private void openGroupsGUI(UUID categoryId) {
        String categoryName = categoryId == null
                ? null
                : categoryManager.getCategoryById(categoryId).map(Category::getName).orElse(null);
        BulbManagerGUI groupsGUI = new BulbManagerGUI(bulbManager, chestManager, categoryManager, player, showAllCategories, categoryName);
        player.closeInventory();
        groupsGUI.open();
    }

    private int getCategoryIndexFromSlot(int slot) {
        if (slot < 0 || slot >= ITEMS_PER_PAGE) return -1;
        return currentPage * ITEMS_PER_PAGE + slot;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
    }

    public static boolean hasPendingAction(UUID playerUuid) {
        return pendingActions.containsKey(playerUuid);
    }

    public static PendingAction getPendingAction(UUID playerUuid) {
        return pendingActions.get(playerUuid);
    }

    public static boolean hasPendingConnectorToolPrompt(UUID playerUuid) {
        PendingAction action = pendingActions.get(playerUuid);
        return action != null && action.type() == PendingActionType.CREATE_CONNECTOR_TOOL;
    }

    public static boolean processPendingConnectorToolSelection(Player player, Location location,
                                                               LinkedBulbManager bulbManager, LinkedChestManager chestManager) {
        if (!hasPendingConnectorToolPrompt(player.getUniqueId())) {
            return false;
        }

        Optional<BulbGroup> bulbGroup = bulbManager.getGroupByLocation(location);
        if (bulbGroup.isPresent()) {
            if (giveExistingGroupConnectorTool(player, bulbGroup.get(), ConnectorToolFactory.GroupType.BULB, NamedTextColor.AQUA)) {
                pendingActions.remove(player.getUniqueId());
            }
            return true;
        }

        if (chestManager != null) {
            Optional<ChestGroup> chestGroup = chestManager.getGroupByLocation(location);
            if (chestGroup.isPresent()) {
                if (giveExistingGroupConnectorTool(player, chestGroup.get(), ConnectorToolFactory.GroupType.CHEST, NamedTextColor.GOLD)) {
                    pendingActions.remove(player.getUniqueId());
                }
                return true;
            }
        }

        return false;
    }

    private static boolean giveExistingGroupConnectorTool(Player player, BaseGroup group,
                                                          ConnectorToolFactory.GroupType groupType, NamedTextColor groupColor) {
        if (!player.hasPermission("wirelessredstone.admin")
                && !player.getUniqueId().equals(group.getOwnerUuid())) {
            player.sendMessage(Component.text("You can only get circuit tools for your own groups.", NamedTextColor.RED));
            return false;
        }

        giveItemToPlayer(player, ConnectorToolFactory.createConnectorTool(
                group.getGroupId(),
                group.getDisplayName(),
                groupType,
                groupType == ConnectorToolFactory.GroupType.BULB
                        ? WireViewManager.getBulbGroupTextColor(group.getGroupId(), WirelessRedstonePlugin.getInstance().getBulbManager().getAllPlacedGroups())
                        : WireViewManager.getChestGroupTextColor(group.getGroupId(), WirelessRedstonePlugin.getInstance().getChestManager().getAllPlacedGroups())
        ));
        player.sendMessage(Component.text("You received a Circuit Tool for group ", NamedTextColor.GREEN)
                .append(Component.text(group.getDisplayName(), groupColor))
                .append(Component.text(".", NamedTextColor.GREEN)));
        return true;
    }

    public static void processPendingAction(Player player, String input, CategoryManager categoryManager,
                                             LinkedBulbManager bulbManager, LinkedChestManager chestManager) {
        PendingAction action = pendingActions.remove(player.getUniqueId());
        if (action == null) return;
        input = input.trim();

        if (input.equalsIgnoreCase("cancel")) {
            pendingConnectorCategoryNames.remove(player.getUniqueId());
            player.sendMessage(Component.text("Action cancelled.", NamedTextColor.GRAY));
            reopenCategoryGUI(player, categoryManager, bulbManager, chestManager);
            return;
        }

        switch (action.type()) {
            case CREATE_CATEGORY -> {
                String name = input.length() > 32 ? input.substring(0, 32) : input;
                categoryManager.createCategory(player.getUniqueId(), name);
                player.sendMessage(Component.text("Category created: " + name, NamedTextColor.GREEN));
            }
            case RENAME_CATEGORY -> {
                if (action.categoryId() != null) {
                    String name = input.length() > 32 ? input.substring(0, 32) : input;
                    categoryManager.renameCategory(action.categoryId(), name);
                    player.sendMessage(Component.text("Category renamed to: " + name, NamedTextColor.GREEN));
                }
            }
            case SET_CATEGORY_DESCRIPTION -> {
                if (action.categoryId() != null) {
                    String description = input.equalsIgnoreCase("clear") || input.equalsIgnoreCase("reset")
                            ? null
                            : input.length() > 96 ? input.substring(0, 96) : input;
                    categoryManager.setCategoryDescription(action.categoryId(), description);
                    player.sendMessage(Component.text(description == null ? "Category description cleared." : "Category description updated.", NamedTextColor.GREEN));
                }
            }
            case CREATE_CONNECTOR_TOOL -> {
                String groupName = input.length() > 32 ? input.substring(0, 32) : input;
                String categoryName = pendingConnectorCategoryNames.remove(player.getUniqueId());
                if (categoryName == null) {
                    categoryName = action.categoryId() == null
                        ? null
                        : categoryManager.getCategoryById(action.categoryId()).map(Category::getName).orElse(null);
                }
                String storedGroupName = categoryName != null && !categoryName.isBlank() && !groupName.contains("/")
                        ? categoryName + "/" + groupName
                        : groupName;
                giveItemToPlayer(player, ConnectorToolFactory.createCreationModeConnectorTool(storedGroupName));
                player.sendMessage(Component.text("You received a Circuit Tool for new group ", NamedTextColor.GREEN)
                        .append(Component.text(storedGroupName, NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text(".", NamedTextColor.GREEN)));
                return;
            }
        }
        
        reopenCategoryGUI(player, categoryManager, bulbManager, chestManager);
    }

    private static void reopenCategoryGUI(Player player, CategoryManager categoryManager,
                                           LinkedBulbManager bulbManager, LinkedChestManager chestManager) {
        player.getServer().getScheduler().runTask(
            player.getServer().getPluginManager().getPlugin("WirelessRedstone"),
            () -> new CategorySelectionGUI(categoryManager, bulbManager, chestManager, player, false).open()
        );
    }

    public static void cancelPendingAction(UUID playerUuid) {
        pendingActions.remove(playerUuid);
        pendingConnectorCategoryNames.remove(playerUuid);
    }

    public static void startConnectorToolPrompt(Player player, UUID categoryId, CategoryManager categoryManager) {
        pendingActions.put(player.getUniqueId(), new PendingAction(PendingActionType.CREATE_CONNECTOR_TOOL, categoryId));
        pendingConnectorCategoryNames.remove(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(Component.text("Enter a name for the new wireless group (or 'cancel' to abort):", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Or right-click an existing wireless block to get a connector for its group.", NamedTextColor.GRAY));
        if (categoryId != null && categoryManager != null) {
            categoryManager.getCategoryById(categoryId).ifPresent(category ->
                    player.sendMessage(Component.text("Category: ", NamedTextColor.GRAY)
                            .append(Component.text(category.getDisplayName(), NamedTextColor.YELLOW))));
        }
    }

    public static void startConnectorToolPrompt(Player player, String categoryName) {
        pendingActions.put(player.getUniqueId(), new PendingAction(PendingActionType.CREATE_CONNECTOR_TOOL, null));
        if (categoryName == null || categoryName.isBlank()) {
            pendingConnectorCategoryNames.remove(player.getUniqueId());
        } else {
            pendingConnectorCategoryNames.put(player.getUniqueId(), categoryName);
        }
        player.closeInventory();
        player.sendMessage(Component.text("Enter a name for the new wireless group (or 'cancel' to abort):", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Or right-click an existing wireless block to get a connector for its group.", NamedTextColor.GRAY));
        if (categoryName != null && !categoryName.isBlank()) {
            player.sendMessage(Component.text("Category: ", NamedTextColor.GRAY)
                    .append(Component.text(categoryName, NamedTextColor.YELLOW)));
        }
    }

    private static void giveItemToPlayer(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
    }
}
