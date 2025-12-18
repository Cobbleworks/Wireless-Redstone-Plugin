package com.wirelessredstone.gui;

import com.wirelessredstone.item.BulbVariant;
import com.wirelessredstone.item.ChestVariant;
import com.wirelessredstone.model.BulbGroup;
import com.wirelessredstone.model.ChestGroup;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.List;
import java.util.UUID;

public class GroupEntry {

    public enum GroupType {
        BULB,
        CHEST
    }

    private final GroupType type;
    private final BulbGroup bulbGroup;
    private final ChestGroup chestGroup;

    public GroupEntry(BulbGroup bulbGroup) {
        this.type = GroupType.BULB;
        this.bulbGroup = bulbGroup;
        this.chestGroup = null;
    }

    public GroupEntry(ChestGroup chestGroup) {
        this.type = GroupType.CHEST;
        this.bulbGroup = null;
        this.chestGroup = chestGroup;
    }

    public GroupType getType() {
        return type;
    }

    public BulbGroup getBulbGroup() {
        return bulbGroup;
    }

    public ChestGroup getChestGroup() {
        return chestGroup;
    }

    public UUID getGroupId() {
        return type == GroupType.BULB ? bulbGroup.getGroupId() : chestGroup.getGroupId();
    }

    public UUID getOwnerUuid() {
        return type == GroupType.BULB ? bulbGroup.getOwnerUuid() : chestGroup.getOwnerUuid();
    }

    public String getDisplayName() {
        return type == GroupType.BULB ? bulbGroup.getDisplayName() : chestGroup.getDisplayName();
    }

    public String getCustomName() {
        return type == GroupType.BULB ? bulbGroup.getCustomName() : chestGroup.getCustomName();
    }

    public void setCustomName(String name) {
        if (type == GroupType.BULB) {
            bulbGroup.setCustomName(name);
        } else {
            chestGroup.setCustomName(name);
        }
    }

    public Material getCustomIcon() {
        return type == GroupType.BULB ? bulbGroup.getCustomIcon() : chestGroup.getCustomIcon();
    }

    public void setCustomIcon(Material icon) {
        if (type == GroupType.BULB) {
            bulbGroup.setCustomIcon(icon);
        } else {
            chestGroup.setCustomIcon(icon);
        }
    }

    public Material getDefaultIcon() {
        if (type == GroupType.BULB) {
            return bulbGroup.getBulbType() == BulbVariant.BulbType.REDSTONE_LAMP
                    ? Material.REDSTONE_LAMP
                    : Material.COPPER_BULB;
        } else {
            ChestVariant.ContainerType containerType = chestGroup.getContainerType();
            if (containerType == ChestVariant.ContainerType.CHEST) {
                return Material.CHEST;
            } else {
                return Material.SHULKER_BOX;
            }
        }
    }

    public String getTypeDisplayName() {
        if (type == GroupType.BULB) {
            return bulbGroup.getBulbType().name();
        } else {
            ChestVariant.ContainerType containerType = chestGroup.getContainerType();
            return containerType == ChestVariant.ContainerType.CHEST ? "CHEST" : "SHULKER_BOX";
        }
    }

    public String getStatusDisplay() {
        if (type == GroupType.BULB) {
            return bulbGroup.isLit() ? "ON" : "OFF";
        } else {
            return "SYNCED";
        }
    }

    public boolean isLit() {
        return type == GroupType.BULB && bulbGroup.isLit();
    }

    public int getPlacedCount() {
        return type == GroupType.BULB ? bulbGroup.getPlacedCount() : chestGroup.getPlacedCount();
    }

    public int getMaxSize() {
        return type == GroupType.BULB ? bulbGroup.getMaxSize() : chestGroup.getMaxSize();
    }

    public List<Location> getLocations() {
        return type == GroupType.BULB ? bulbGroup.getLocations() : chestGroup.getLocations();
    }

    public List<Location> getPlacedLocations() {
        return type == GroupType.BULB ? bulbGroup.getPlacedLocations() : chestGroup.getPlacedLocations();
    }

    public int getLocationIndex(Location location) {
        return type == GroupType.BULB ? bulbGroup.getLocationIndex(location) : chestGroup.getLocationIndex(location);
    }

    public UUID getCategoryId() {
        return type == GroupType.BULB ? bulbGroup.getCategoryId() : chestGroup.getCategoryId();
    }

    public void setCategoryId(UUID categoryId) {
        if (type == GroupType.BULB) {
            bulbGroup.setCategoryId(categoryId);
        } else {
            chestGroup.setCategoryId(categoryId);
        }
    }

    public static String getIndexLabel(int index) {
        if (index < 0 || index > 25) return String.valueOf(index);
        return String.valueOf((char) ('A' + index));
    }
}
