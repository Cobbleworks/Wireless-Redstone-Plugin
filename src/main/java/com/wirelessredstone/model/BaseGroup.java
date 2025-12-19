package com.wirelessredstone.model;

import com.wirelessredstone.util.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.*;

/**
 * Abstract base class for wireless block groups.
 * Contains common functionality shared between BulbGroup and ChestGroup.
 */
public abstract class BaseGroup {

    protected final UUID groupId;
    protected final List<Location> locations;
    protected int maxSize;
    protected UUID ownerUuid;
    protected String customName;
    protected Material customIcon;
    protected UUID categoryId;

    protected BaseGroup(UUID groupId, int maxSize) {
        this.groupId = groupId;
        this.maxSize = maxSize;
        this.locations = new ArrayList<>(Collections.nCopies(maxSize, null));
    }

    protected BaseGroup(UUID groupId, int maxSize, UUID ownerUuid) {
        this(groupId, maxSize);
        this.ownerUuid = ownerUuid;
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
            if (LocationUtils.isSameBlock(location, locations.get(i))) {
                locations.set(i, null);
                break;
            }
        }
    }

    public List<Location> getOtherLocations(Location location) {
        List<Location> others = new ArrayList<>();
        for (Location loc : locations) {
            if (loc != null && !LocationUtils.isSameBlock(loc, location)) {
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
        return locations.stream().anyMatch(loc -> LocationUtils.isSameBlock(location, loc));
    }

    public int getLocationIndex(Location location) {
        for (int i = 0; i < locations.size(); i++) {
            if (LocationUtils.isSameBlock(location, locations.get(i))) {
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

    /**
     * Removes an empty slot from the group, compacting it.
     * If the slot at the given index is empty, it removes that slot.
     * If the slot is not empty, it clears it first, then removes it.
     * @param index The index of the slot to remove
     * @return true if the slot was removed, false if index is invalid
     */
    public boolean removeSlot(int index) {
        if (index < 0 || index >= maxSize || maxSize <= 1) return false;
        locations.remove(index);
        maxSize--;
        return true;
    }

    /**
     * Gets the display label for a group index (A-Z).
     */
    public static String getIndexLabel(int index) {
        if (index < 0 || index > 25) return String.valueOf(index);
        return String.valueOf((char) ('A' + index));
    }

    /**
     * Gets the default icon material for this group type.
     */
    public abstract Material getDefaultIcon();
}
