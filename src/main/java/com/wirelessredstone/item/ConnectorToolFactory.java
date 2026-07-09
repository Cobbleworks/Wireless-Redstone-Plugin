package com.wirelessredstone.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

/**
 * Factory for creating Circuit Tool items.
 * Circuit Tools are used to analyze groups and add/remove blocks from wireless groups.
 */
public class ConnectorToolFactory {

    public static final NamespacedKey CONNECTOR_TOOL_KEY = new NamespacedKey("wirelessredstone", "connector_tool");
    public static final NamespacedKey CONNECTOR_GROUP_ID_KEY = new NamespacedKey("wirelessredstone", "connector_group_id");
    public static final NamespacedKey CONNECTOR_GROUP_TYPE_KEY = new NamespacedKey("wirelessredstone", "connector_group_type");
    public static final NamespacedKey CONNECTOR_GROUP_NAME_KEY = new NamespacedKey("wirelessredstone", "connector_group_name");
    public static final NamespacedKey CONNECTOR_CREATION_MODE_KEY = new NamespacedKey("wirelessredstone", "connector_creation_mode");
    public static final NamespacedKey CONNECTOR_CATEGORY_NAME_KEY = new NamespacedKey("wirelessredstone", "connector_category_name");

    /**
     * Group type enum to distinguish between bulb and chest groups.
     */
    public enum GroupType {
        BULB, CHEST
    }

    /**
     * Creates a new Circuit Tool item for a specific group.
     */
    public static ItemStack createConnectorTool(UUID groupId, String groupName, GroupType groupType) {
        ItemStack item = new ItemStack(Material.SHEARS);
        ItemMeta meta = item.getItemMeta();

        NamedTextColor typeColor = groupType == GroupType.BULB ? NamedTextColor.AQUA : NamedTextColor.GOLD;
        String typeLabel = groupType == GroupType.BULB ? "Bulb/Lamp" : "Container";

        meta.displayName(Component.text("Circuit Tool ", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text("(" + groupName + ")", typeColor)
                        .decoration(TextDecoration.BOLD, false)));

        meta.lore(List.of(
                Component.text("Connect blocks to: ", NamedTextColor.GRAY)
                        .append(Component.text(groupName, typeColor))
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Type: ", NamedTextColor.GRAY)
                        .append(Component.text(typeLabel, typeColor))
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Right-click ", NamedTextColor.YELLOW)
                        .append(Component.text("a matching block to add it", NamedTextColor.GRAY))
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Left-click ", NamedTextColor.YELLOW)
                        .append(Component.text("a group block to remove it", NamedTextColor.GRAY))
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Left-click ", NamedTextColor.YELLOW)
                        .append(Component.text("another group to analyze it", NamedTextColor.GRAY))
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Hold ", NamedTextColor.YELLOW)
                        .append(Component.text("to reveal wireless circuits", NamedTextColor.GRAY))
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("ID: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(groupId.toString().substring(0, 8), NamedTextColor.DARK_GRAY))
                        .decoration(TextDecoration.ITALIC, false)
        ));

        meta.getPersistentDataContainer().set(CONNECTOR_TOOL_KEY, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(CONNECTOR_GROUP_ID_KEY, PersistentDataType.STRING, groupId.toString());
        meta.getPersistentDataContainer().set(CONNECTOR_GROUP_TYPE_KEY, PersistentDataType.STRING, groupType.name());
        meta.getPersistentDataContainer().set(CONNECTOR_GROUP_NAME_KEY, PersistentDataType.STRING, groupName);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a new Circuit Tool in "creation mode" for creating a new group.
     * The first block added will determine the group type (bulb/lamp or chest).
     */
    public static ItemStack createCreationModeConnectorTool(String groupName) {
        return createCreationModeConnectorTool(groupName, null);
    }

    /**
     * Creates a new Circuit Tool in "creation mode" for creating a new group with an optional category.
     * The first block added will determine the group type (bulb/lamp or chest).
     */
    public static ItemStack createCreationModeConnectorTool(String groupName, String categoryName) {
        ItemStack item = new ItemStack(Material.SHEARS);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Circuit Tool ", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text("(" + groupName + ")", NamedTextColor.LIGHT_PURPLE)
                        .decoration(TextDecoration.BOLD, false)));

        List<Component> loreLines = new java.util.ArrayList<>();
        loreLines.add(Component.text("Create new group: ", NamedTextColor.GRAY)
                .append(Component.text(groupName, NamedTextColor.LIGHT_PURPLE))
                .decoration(TextDecoration.ITALIC, false));
        loreLines.add(Component.text("Type: ", NamedTextColor.GRAY)
                .append(Component.text("Auto-detect on first block", NamedTextColor.LIGHT_PURPLE))
                .decoration(TextDecoration.ITALIC, false));
        if (categoryName != null && !categoryName.isEmpty()) {
            loreLines.add(Component.text("Category: ", NamedTextColor.GRAY)
                    .append(Component.text(categoryName, NamedTextColor.YELLOW))
                    .decoration(TextDecoration.ITALIC, false));
        }
        loreLines.add(Component.empty());
        loreLines.add(Component.text("Right-click ", NamedTextColor.YELLOW)
                .append(Component.text("a bulb/lamp/chest to create group", NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));
        loreLines.add(Component.text("Hold ", NamedTextColor.YELLOW)
                .append(Component.text("to reveal wireless circuits", NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));
        loreLines.add(Component.empty());
        loreLines.add(Component.text("⚡ ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text("Creation Mode", NamedTextColor.LIGHT_PURPLE))
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(loreLines);

        meta.getPersistentDataContainer().set(CONNECTOR_TOOL_KEY, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(CONNECTOR_CREATION_MODE_KEY, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(CONNECTOR_GROUP_NAME_KEY, PersistentDataType.STRING, groupName);
        if (categoryName != null && !categoryName.isEmpty()) {
            meta.getPersistentDataContainer().set(CONNECTOR_CATEGORY_NAME_KEY, PersistentDataType.STRING, categoryName);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Checks if an item is a Circuit Tool.
     */
    public static boolean isConnectorTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(CONNECTOR_TOOL_KEY, PersistentDataType.BYTE);
    }

    /**
     * Gets the group ID from a Circuit Tool.
     */
    public static UUID getGroupId(ItemStack item) {
        if (!isConnectorTool(item)) return null;
        String idStr = item.getItemMeta().getPersistentDataContainer().get(CONNECTOR_GROUP_ID_KEY, PersistentDataType.STRING);
        return idStr != null ? UUID.fromString(idStr) : null;
    }

    /**
     * Gets the group type from a Circuit Tool.
     */
    public static GroupType getGroupType(ItemStack item) {
        if (!isConnectorTool(item)) return null;
        String typeStr = item.getItemMeta().getPersistentDataContainer().get(CONNECTOR_GROUP_TYPE_KEY, PersistentDataType.STRING);
        if (typeStr == null) return null;
        try {
            return GroupType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Checks if the Circuit Tool is in creation mode (no group exists yet).
     */
    public static boolean isCreationMode(ItemStack item) {
        if (!isConnectorTool(item)) return false;
        return item.getItemMeta().getPersistentDataContainer().has(CONNECTOR_CREATION_MODE_KEY, PersistentDataType.BYTE);
    }

    /**
     * Gets the group name from a Circuit Tool (works for both regular and creation mode).
     */
    public static String getGroupName(ItemStack item) {
        if (!isConnectorTool(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(CONNECTOR_GROUP_NAME_KEY, PersistentDataType.STRING);
    }

    /**
     * Gets the category name from a creation-mode Circuit Tool.
     * Returns null if no category is set or if not in creation mode.
     */
    public static String getCategoryName(ItemStack item) {
        if (!isConnectorTool(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(CONNECTOR_CATEGORY_NAME_KEY, PersistentDataType.STRING);
    }
}
