package com.wirelessredstone.model;

import com.wirelessredstone.item.ChestVariant;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ChestGroup {

    private final UUID groupId;
    private final List<Location> locations;
    private int maxSize;
    private UUID ownerUuid;
    private String customName;
    private Material customIcon;
    private ItemStack[] sharedInventory;
    private ChestVariant.ContainerType containerType;
    private UUID categoryId;

    public ChestGroup(UUID groupId, int maxSize) {
        this.groupId = groupId;
        this.maxSize = maxSize;
        this.locations = new ArrayList<>(Collections.nCopies(maxSize, null));
        this.sharedInventory = new ItemStack[27];
        this.containerType = ChestVariant.ContainerType.CHEST;
    }

    public ChestGroup(UUID groupId, int maxSize, UUID ownerUuid) {
        this.groupId = groupId;
        this.maxSize = maxSize;
        this.locations = new ArrayList<>(Collections.nCopies(maxSize, null));
        this.ownerUuid = ownerUuid;
        this.sharedInventory = new ItemStack[27];
        this.containerType = ChestVariant.ContainerType.CHEST;
    }

    public ChestGroup(UUID groupId, int maxSize, UUID ownerUuid, ChestVariant.ContainerType containerType) {
        this.groupId = groupId;
        this.maxSize = maxSize;
        this.locations = new ArrayList<>(Collections.nCopies(maxSize, null));
        this.ownerUuid = ownerUuid;
        this.sharedInventory = new ItemStack[27];
        this.containerType = containerType != null ? containerType : ChestVariant.ContainerType.CHEST;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public List<Location> getLocations() {
        return Collections.unmodifiableList(locations);
    }

    public Location getLocation(int index) {
        if (index < 0 || index >= maxSize) return null;
        return locations.get(index);
    }

    public void setLocation(int index, Location location) {
        if (index >= 0 && index < maxSize) {
            locations.set(index, location);
        }
    }

    public void removeLocation(Location location) {
        for (int i = 0; i < locations.size(); i++) {
            if (isSameBlock(location, locations.get(i))) {
                locations.set(i, null);
                break;
            }
        }
    }

    public List<Location> getOtherLocations(Location location) {
        List<Location> others = new ArrayList<>();
        for (Location loc : locations) {
            if (loc != null && !isSameBlock(loc, location)) {
                others.add(loc);
            }
        }
        return others;
    }

    public List<Location> getPlacedLocations() {
        List<Location> placed = new ArrayList<>();
        for (Location loc : locations) {
            if (loc != null) {
                placed.add(loc);
            }
        }
        return placed;
    }

    public boolean isEmpty() {
        return locations.stream().allMatch(Objects::isNull);
    }

    public boolean hasLocation(Location location) {
        return locations.stream().anyMatch(loc -> isSameBlock(location, loc));
    }

    public int getLocationIndex(Location location) {
        for (int i = 0; i < locations.size(); i++) {
            if (isSameBlock(location, locations.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public int getPlacedCount() {
        return (int) locations.stream().filter(Objects::nonNull).count();
    }

    public boolean isComplete() {
        return locations.stream().noneMatch(Objects::isNull);
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public String getDisplayName() {
        return customName != null ? customName : groupId.toString().substring(0, 8);
    }

    public Material getCustomIcon() {
        return customIcon;
    }

    public void setCustomIcon(Material customIcon) {
        this.customIcon = customIcon;
    }

    public ItemStack[] getSharedInventory() {
        return sharedInventory;
    }

    public void setSharedInventory(ItemStack[] inventory) {
        if (inventory != null && inventory.length == 27) {
            this.sharedInventory = inventory;
        }
    }

    public void updateSharedInventory(ItemStack[] inventory) {
        if (inventory == null) return;
        for (int i = 0; i < Math.min(inventory.length, 27); i++) {
            this.sharedInventory[i] = inventory[i] != null ? inventory[i].clone() : null;
        }
    }

    public ChestVariant.ContainerType getContainerType() {
        return containerType;
    }

    public void setContainerType(ChestVariant.ContainerType containerType) {
        this.containerType = containerType;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public void extendGroup(int extraSlots) {
        for (int i = 0; i < extraSlots; i++) {
            locations.add(null);
        }
        maxSize += extraSlots;
    }

    public static String getIndexLabel(int index) {
        if (index < 0 || index > 25) return String.valueOf(index);
        return String.valueOf((char) ('A' + index));
    }

    /**
     * Compares two locations by block coordinates only (ignores yaw, pitch, and decimal parts).
     * This ensures proper comparison when locations come from different sources.
     */
    private static boolean isSameBlock(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) return false;
        if (loc1.getWorld() == null || loc2.getWorld() == null) return false;
        return loc1.getWorld().equals(loc2.getWorld())
                && loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ();
    }
}
