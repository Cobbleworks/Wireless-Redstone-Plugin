package com.wirelessredstone.manager;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.model.BaseGroup;
import com.wirelessredstone.util.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Shared registry and lifecycle for every kind of linked block group.
 * Type-specific managers only own item metadata, persistence and state syncing.
 */
public abstract class LinkedGroupManager<G extends BaseGroup> {

    protected final WirelessRedstonePlugin plugin;
    protected final Map<UUID, G> groups = new ConcurrentHashMap<>();
    protected final Map<Location, UUID> locationToGroupId = new ConcurrentHashMap<>();

    protected LinkedGroupManager(WirelessRedstonePlugin plugin) {
        this.plugin = plugin;
    }

    public final void reloadData() {
        groups.clear();
        locationToGroupId.clear();
        loadData();
    }

    public final UUID createNewGroupId() {
        return UUID.randomUUID();
    }

    protected final G registerLocation(Location location, UUID groupId, int index,
                                       int groupSize, UUID ownerUuid,
                                       GroupFactory<G> factory) {
        Location normalized = LocationUtils.normalize(location);
        G group = groups.computeIfAbsent(groupId,
                id -> factory.create(id, groupSize, ownerUuid));
        if (groupSize > group.getMaxSize()) {
            group.extendGroup(groupSize - group.getMaxSize());
        }
        group.setLocation(index, normalized);
        if (ownerUuid != null && group.getOwnerUuid() == null) {
            group.setOwnerUuid(ownerUuid);
        }
        locationToGroupId.put(normalized, groupId);
        return group;
    }

    protected final void putGroup(G group) {
        groups.put(group.getGroupId(), group);
    }

    public final void unregisterLocation(Location location) {
        unregisterLocationAndCheckGroupRemoval(location);
    }

    public final UUID unregisterLocationAndCheckGroupRemoval(Location location) {
        Location normalized = LocationUtils.normalize(location);
        UUID groupId = locationToGroupId.remove(normalized);
        boolean removed = false;
        if (groupId != null) {
            G group = groups.get(groupId);
            if (group != null) {
                group.removeLocation(normalized);
                if (group.isEmpty()) {
                    groups.remove(groupId);
                    removed = true;
                }
            }
        }
        saveData();
        return removed ? groupId : null;
    }

    public final void removeGroup(UUID groupId) {
        removeGroup(groupId, true);
    }

    public final void removeGroup(UUID groupId, boolean removeBlocks) {
        G group = groups.remove(groupId);
        if (group != null) {
            for (Location location : group.getPlacedLocations()) {
                locationToGroupId.remove(LocationUtils.normalize(location));
                if (removeBlocks && location.isChunkLoaded()) {
                    location.getBlock().setType(Material.AIR);
                }
            }
        }
        saveData();
    }

    public final Optional<G> getGroupById(UUID groupId) {
        return Optional.ofNullable(groups.get(groupId));
    }

    public final Optional<G> getGroupByLocation(Location location) {
        UUID groupId = locationToGroupId.get(LocationUtils.normalize(location));
        return groupId == null ? Optional.empty() : Optional.ofNullable(groups.get(groupId));
    }

    public final boolean isWirelessLocation(Location location) {
        return locationToGroupId.containsKey(LocationUtils.normalize(location));
    }

    public final Collection<G> getAllGroups() {
        return groups.values();
    }

    public final List<G> getGroupsByOwner(UUID ownerUuid) {
        return groups.values().stream()
                .filter(group -> ownerUuid.equals(group.getOwnerUuid()))
                .collect(Collectors.toList());
    }

    public final List<G> getAllPlacedGroups() {
        return groups.values().stream()
                .filter(group -> group.getPlacedCount() > 0)
                .collect(Collectors.toList());
    }

    public abstract void saveData();

    public abstract void loadData();

    @FunctionalInterface
    protected interface GroupFactory<G extends BaseGroup> {
        G create(UUID groupId, int size, UUID ownerUuid);
    }
}
