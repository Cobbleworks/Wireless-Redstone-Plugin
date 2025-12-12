package com.wirelessredstone.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ChestGroup {

    private final UUID groupId;
    private final List<Location> locations;
    private final int maxSize;
    private UUID ownerUuid;
    private String customName;
    private Material customIcon;
    private ItemStack[] sharedInventory;

    public ChestGroup(UUID groupId, int maxSize) {
        this.groupId = groupId;
        this.maxSize = maxSize;
        this.locations = new ArrayList<>(Collections.nCopies(maxSize, null));
        this.sharedInventory = new ItemStack[27];
    }

    public ChestGroup(UUID groupId, int maxSize, UUID ownerUuid) {
        this.groupId = groupId;
        this.maxSize = maxSize;
        this.locations = new ArrayList<>(Collections.nCopies(maxSize, null));
        this.ownerUuid = ownerUuid;
        this.sharedInventory = new ItemStack[27];
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
            if (location.equals(locations.get(i))) {
                locations.set(i, null);
                break;
            }
        }
    }

    public List<Location> getOtherLocations(Location location) {
        List<Location> others = new ArrayList<>();
        for (Location loc : locations) {
            if (loc != null && !loc.equals(location)) {
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
        return locations.stream().anyMatch(loc -> location.equals(loc));
    }

    public int getLocationIndex(Location location) {
        for (int i = 0; i < locations.size(); i++) {
            if (location.equals(locations.get(i))) {
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

    public static String getIndexLabel(int index) {
        if (index < 0 || index > 25) return String.valueOf(index);
        return String.valueOf((char) ('A' + index));
    }
}
