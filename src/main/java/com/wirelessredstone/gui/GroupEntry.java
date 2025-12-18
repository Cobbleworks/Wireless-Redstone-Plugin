package com.wirelessredstone.gui;

import com.wirelessredstone.model.BaseGroup;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.model.ChestGroup;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.List;
import java.util.UUID;

/**
 * Wrapper class for displaying both BulbGroup and ChestGroup in GUIs.
 * Provides a unified interface for accessing common group properties.
 */
public class GroupEntry {

    public enum GroupType {
        BULB,
        CHEST
    }

    private final GroupType type;
    private final BaseGroup group;

    public GroupEntry(BulbGroup bulbGroup) {
        this.type = GroupType.BULB;
        this.group = bulbGroup;
    }

    public GroupEntry(ChestGroup chestGroup) {
        this.type = GroupType.CHEST;
        this.group = chestGroup;
    }

    public GroupType getType() {
        return type;
    }

    public BulbGroup getBulbGroup() {
        return type == GroupType.BULB ? (BulbGroup) group : null;
    }

    public ChestGroup getChestGroup() {
        return type == GroupType.CHEST ? (ChestGroup) group : null;
    }

    // Delegated methods to BaseGroup - now much simpler!
    
    public UUID getGroupId() {
        return group.getGroupId();
    }

    public UUID getOwnerUuid() {
        return group.getOwnerUuid();
    }

    public String getDisplayName() {
        return group.getDisplayName();
    }

    public String getCustomName() {
        return group.getCustomName();
    }

    public void setCustomName(String name) {
        group.setCustomName(name);
    }

    public Material getCustomIcon() {
        return group.getCustomIcon();
    }

    public void setCustomIcon(Material icon) {
        group.setCustomIcon(icon);
    }

    public Material getDefaultIcon() {
        return group.getDefaultIcon();
    }

    public int getPlacedCount() {
        return group.getPlacedCount();
    }

    public int getMaxSize() {
        return group.getMaxSize();
    }

    public List<Location> getLocations() {
        return group.getLocations();
    }

    public List<Location> getPlacedLocations() {
        return group.getPlacedLocations();
    }

    public int getLocationIndex(Location location) {
        return group.getLocationIndex(location);
    }

    public UUID getCategoryId() {
        return group.getCategoryId();
    }

    public void setCategoryId(UUID categoryId) {
        group.setCategoryId(categoryId);
    }

    // Type-specific methods that need special handling

    public String getTypeDisplayName() {
        if (type == GroupType.BULB) {
            return ((BulbGroup) group).getBulbType().name();
        } else {
            var containerType = ((ChestGroup) group).getContainerType();
            return containerType.name();
        }
    }

    public String getStatusDisplay() {
        if (type == GroupType.BULB) {
            return ((BulbGroup) group).isLit() ? "ON" : "OFF";
        } else {
            return "SYNCED";
        }
    }

    public boolean isLit() {
        return type == GroupType.BULB && ((BulbGroup) group).isLit();
    }

    public static String getIndexLabel(int index) {
        return BaseGroup.getIndexLabel(index);
    }
}
