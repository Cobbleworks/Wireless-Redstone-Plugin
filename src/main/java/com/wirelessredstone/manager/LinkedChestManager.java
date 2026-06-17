package com.wirelessredstone.manager;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.item.ChestVariant;
import com.wirelessredstone.model.ChestGroup;
import com.wirelessredstone.util.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LinkedChestManager {

    private final WirelessRedstonePlugin plugin;
    private final Map<UUID, ChestGroup> chestGroups = new ConcurrentHashMap<>();
    private final Map<Location, UUID> locationToGroupId = new ConcurrentHashMap<>();
    
    public static final NamespacedKey WIRELESS_CHEST_KEY;
    public static final NamespacedKey CHEST_GROUP_ID_KEY;
    public static final NamespacedKey CHEST_INDEX_KEY;
    public static final NamespacedKey CHEST_OWNER_KEY;
    public static final NamespacedKey CHEST_GROUP_SIZE_KEY;
    public static final NamespacedKey CHEST_CONTAINER_TYPE_KEY;

    static {
        WIRELESS_CHEST_KEY = new NamespacedKey("wirelessredstone", "wireless_chest");
        CHEST_GROUP_ID_KEY = new NamespacedKey("wirelessredstone", "chest_group_id");
        CHEST_INDEX_KEY = new NamespacedKey("wirelessredstone", "chest_index");
        CHEST_OWNER_KEY = new NamespacedKey("wirelessredstone", "chest_owner");
        CHEST_GROUP_SIZE_KEY = new NamespacedKey("wirelessredstone", "chest_group_size");
        CHEST_CONTAINER_TYPE_KEY = new NamespacedKey("wirelessredstone", "chest_container_type");
    }

    public LinkedChestManager(WirelessRedstonePlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

    /**
     * Reloads all chest data from disk, clearing existing data first.
     */
    public void reloadData() {
        chestGroups.clear();
        locationToGroupId.clear();
        loadData();
    }

    public UUID createNewGroupId() {
        return UUID.randomUUID();
    }

    public boolean isWirelessChest(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(WIRELESS_CHEST_KEY, PersistentDataType.BYTE);
    }

    public Optional<UUID> getGroupId(ItemStack item) {
        if (!isWirelessChest(item)) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        String groupIdStr = meta.getPersistentDataContainer().get(CHEST_GROUP_ID_KEY, PersistentDataType.STRING);
        return groupIdStr != null ? Optional.of(UUID.fromString(groupIdStr)) : Optional.empty();
    }

    public Optional<Integer> getChestIndex(ItemStack item) {
        if (!isWirelessChest(item)) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        Integer index = meta.getPersistentDataContainer().get(CHEST_INDEX_KEY, PersistentDataType.INTEGER);
        return Optional.ofNullable(index);
    }

    public Optional<Integer> getGroupSize(ItemStack item) {
        if (!isWirelessChest(item)) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        Integer size = meta.getPersistentDataContainer().get(CHEST_GROUP_SIZE_KEY, PersistentDataType.INTEGER);
        return Optional.ofNullable(size != null ? size : 2);
    }

    public Optional<UUID> getOwnerUuid(ItemStack item) {
        if (!isWirelessChest(item)) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        String ownerStr = meta.getPersistentDataContainer().get(CHEST_OWNER_KEY, PersistentDataType.STRING);
        return ownerStr != null ? Optional.of(UUID.fromString(ownerStr)) : Optional.empty();
    }

    public Optional<ChestVariant.ContainerType> getContainerType(ItemStack item) {
        if (!isWirelessChest(item)) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        String typeStr = meta.getPersistentDataContainer().get(CHEST_CONTAINER_TYPE_KEY, PersistentDataType.STRING);
        if (typeStr == null) return Optional.of(ChestVariant.ContainerType.CHEST);
        try {
            return Optional.of(ChestVariant.ContainerType.valueOf(typeStr));
        } catch (IllegalArgumentException e) {
            return Optional.of(ChestVariant.ContainerType.CHEST);
        }
    }

    public void registerPlacedChest(Location location, UUID groupId, int chestIndex, UUID ownerUuid, int groupSize, ChestVariant.ContainerType containerType) {
        Location normalizedLoc = LocationUtils.normalize(location);
        ChestGroup group = chestGroups.computeIfAbsent(groupId, id -> new ChestGroup(id, groupSize, ownerUuid, containerType));
        
        // If the item indicates a larger group size (from extension), expand the group
        if (groupSize > group.getMaxSize()) {
            group.extendGroup(groupSize - group.getMaxSize());
        }
        
        group.setLocation(chestIndex, normalizedLoc);
        if (ownerUuid != null && group.getOwnerUuid() == null) {
            group.setOwnerUuid(ownerUuid);
        }
        if (containerType != null && group.getContainerType() == null) {
            group.setContainerType(containerType);
        }
        locationToGroupId.put(normalizedLoc, groupId);
        saveData();
    }

    public void unregisterChest(Location location) {
        Location normalizedLoc = LocationUtils.normalize(location);
        UUID groupId = locationToGroupId.remove(normalizedLoc);
        if (groupId != null) {
            ChestGroup group = chestGroups.get(groupId);
            if (group != null) {
                group.removeLocation(normalizedLoc);
                if (group.isEmpty()) {
                    chestGroups.remove(groupId);
                }
            }
        }
        saveData();
    }

    public UUID unregisterChestAndCheckGroupRemoval(Location location) {
        Location normalizedLoc = LocationUtils.normalize(location);
        UUID groupId = locationToGroupId.remove(normalizedLoc);
        if (groupId != null) {
            ChestGroup group = chestGroups.get(groupId);
            if (group != null) {
                group.removeLocation(normalizedLoc);
                if (group.isEmpty()) {
                    chestGroups.remove(groupId);
                    saveData();
                    return groupId;
                }
            }
        }
        saveData();
        return null;
    }

    public void removeGroup(UUID groupId) {
        removeGroup(groupId, true);
    }

    public void removeGroup(UUID groupId, boolean removeBlocks) {
        ChestGroup group = chestGroups.remove(groupId);
        if (group != null) {
            for (Location loc : group.getLocations()) {
                if (loc != null) {
                    locationToGroupId.remove(LocationUtils.normalize(loc));
                    if (removeBlocks && loc.isChunkLoaded()) {
                        loc.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
        saveData();
    }

    /**
     * Pre-registers a group with custom name and category before any chests are placed.
     * This ensures the group has these properties when the first chest is placed.
     */
    public void preRegisterGroup(UUID groupId, int maxSize, UUID ownerUuid, ChestVariant.ContainerType containerType,
                                  String customName, UUID categoryId) {
        preRegisterGroup(groupId, maxSize, ownerUuid, containerType, customName, categoryId, null);
    }

    /**
     * Pre-registers a group with custom name, category, and variant material before any chests are placed.
     * This ensures the group has these properties when the first chest is placed.
     */
    public void preRegisterGroup(UUID groupId, int maxSize, UUID ownerUuid, ChestVariant.ContainerType containerType,
                                  String customName, UUID categoryId, Material variantMaterial) {
        ChestGroup group = new ChestGroup(groupId, maxSize, ownerUuid, containerType);
        if (customName != null) {
            group.setCustomName(customName);
        }
        if (categoryId != null) {
            group.setCategoryId(categoryId);
        }
        if (variantMaterial != null) {
            group.setVariantMaterial(variantMaterial);
        }
        chestGroups.put(groupId, group);
        saveData();
    }

    public Optional<ChestGroup> getGroupById(UUID groupId) {
        return Optional.ofNullable(chestGroups.get(groupId));
    }

    public Optional<ChestGroup> getGroupByLocation(Location location) {
        UUID groupId = locationToGroupId.get(LocationUtils.normalize(location));
        return groupId != null ? Optional.ofNullable(chestGroups.get(groupId)) : Optional.empty();
    }

    public boolean isWirelessChestLocation(Location location) {
        return locationToGroupId.containsKey(LocationUtils.normalize(location));
    }

    public Collection<ChestGroup> getAllGroups() {
        return chestGroups.values();
    }

    public List<ChestGroup> getGroupsByOwner(UUID ownerUuid) {
        return chestGroups.values().stream()
                .filter(group -> ownerUuid.equals(group.getOwnerUuid()))
                .collect(Collectors.toList());
    }

    public List<ChestGroup> getAllPlacedGroups() {
        return chestGroups.values().stream()
                .filter(group -> group.getPlacedCount() > 0)
                .collect(Collectors.toList());
    }

    public void syncInventoryToGroup(Location sourceLocation, ItemStack[] contents) {
        var groupOpt = getGroupByLocation(sourceLocation);
        if (groupOpt.isEmpty()) return;
        
        ChestGroup group = groupOpt.get();
        group.updateSharedInventory(contents);
        
        for (Location loc : group.getOtherLocations(sourceLocation)) {
            applySharedInventory(loc, group);
        }
        
        saveData();
    }

    public void syncGroupToPlacedContainers(UUID groupId) {
        ChestGroup group = chestGroups.get(groupId);
        if (group == null) return;

        for (Location loc : group.getPlacedLocations()) {
            applySharedInventory(loc, group);
        }
    }

    public void applySharedInventory(Location location, ChestGroup group) {
        if (location == null || !location.isChunkLoaded()) return;

        var state = location.getBlock().getState();
        if (!(state instanceof org.bukkit.block.Container container)) return;

        Inventory inventory = container.getInventory();
        ItemStack[] shared = group.getSharedInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, i < shared.length && shared[i] != null ? shared[i].clone() : null);
        }
    }

    public void saveData() {
        File dataFile = new File(plugin.getDataFolder(), "chests.yml");
        FileConfiguration config = new YamlConfiguration();

        int index = 0;
        for (Map.Entry<UUID, ChestGroup> entry : chestGroups.entrySet()) {
            ChestGroup group = entry.getValue();
            String basePath = "groups." + index;
            config.set(basePath + ".id", entry.getKey().toString());
            config.set(basePath + ".maxSize", group.getMaxSize());
            config.set(basePath + ".containerType", group.getContainerType().name());

            if (group.getOwnerUuid() != null) {
                config.set(basePath + ".owner", group.getOwnerUuid().toString());
            }
            if (group.getCustomName() != null) {
                config.set(basePath + ".customName", group.getCustomName());
            }
            if (group.getCustomIcon() != null) {
                config.set(basePath + ".customIcon", group.getCustomIcon().name());
            }
            if (group.getCategoryId() != null) {
                config.set(basePath + ".categoryId", group.getCategoryId().toString());
            }
            if (group.getVariantMaterial() != null) {
                config.set(basePath + ".variantMaterial", group.getVariantMaterial().name());
            }
            
            List<Location> locations = group.getLocations();
            for (int i = 0; i < locations.size(); i++) {
                Location loc = locations.get(i);
                if (loc != null) {
                    config.set(basePath + ".locations." + i, serializeLocation(loc));
                }
            }
            
            ItemStack[] inventory = group.getSharedInventory();
            for (int i = 0; i < inventory.length; i++) {
                if (inventory[i] != null) {
                    config.set(basePath + ".inventory." + i, inventory[i]);
                }
            }
            
            index++;
        }

        try {
            plugin.getDataFolder().mkdirs();
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save chest data: " + e.getMessage());
        }
    }

    public void loadData() {
        File dataFile = new File(plugin.getDataFolder(), "chests.yml");
        if (!dataFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        
        var groupsSection = config.getConfigurationSection("groups");
        if (groupsSection == null) return;

        for (String key : groupsSection.getKeys(false)) {
            String basePath = "groups." + key;
            String idStr = config.getString(basePath + ".id");
            if (idStr == null) continue;

            UUID groupId = UUID.fromString(idStr);
            
            String ownerStr = config.getString(basePath + ".owner");
            UUID ownerUuid = ownerStr != null ? UUID.fromString(ownerStr) : null;
            
            int maxSize = config.getInt(basePath + ".maxSize", 2);
            
            String containerTypeStr = config.getString(basePath + ".containerType", "CHEST");
            ChestVariant.ContainerType containerType;
            try {
                containerType = ChestVariant.ContainerType.valueOf(containerTypeStr);
            } catch (IllegalArgumentException e) {
                containerType = ChestVariant.ContainerType.CHEST;
            }
            
            ChestGroup group = new ChestGroup(groupId, maxSize, ownerUuid, containerType);
            group.setCustomName(config.getString(basePath + ".customName"));
            
            String customIconStr = config.getString(basePath + ".customIcon");
            if (customIconStr != null) {
                try {
                    group.setCustomIcon(Material.valueOf(customIconStr));
                } catch (IllegalArgumentException ignored) {}
            }
            
            String categoryIdStr = config.getString(basePath + ".categoryId");
            if (categoryIdStr != null) {
                group.setCategoryId(UUID.fromString(categoryIdStr));
            }
            
            String variantMaterialStr = config.getString(basePath + ".variantMaterial");
            if (variantMaterialStr != null) {
                try {
                    group.setVariantMaterial(Material.valueOf(variantMaterialStr));
                } catch (IllegalArgumentException ignored) {}
            }

            var locationsSection = config.getConfigurationSection(basePath + ".locations");
            if (locationsSection != null) {
                for (String locKey : locationsSection.getKeys(false)) {
                    int locIndex = Integer.parseInt(locKey);
                    String locStr = config.getString(basePath + ".locations." + locKey);
                    if (locStr != null) {
                        Location loc = deserializeLocation(locStr);
                        if (loc != null) {
                            group.setLocation(locIndex, loc);
                            locationToGroupId.put(loc, groupId);
                        }
                    }
                }
            }
            
            var inventorySection = config.getConfigurationSection(basePath + ".inventory");
            if (inventorySection != null) {
                int inventorySize = ChestGroup.DEFAULT_INVENTORY_SIZE;
                for (String invKey : inventorySection.getKeys(false)) {
                    int slot = Integer.parseInt(invKey);
                    inventorySize = Math.max(inventorySize, slot + 1);
                }

                ItemStack[] inventory = new ItemStack[inventorySize];
                for (String invKey : inventorySection.getKeys(false)) {
                    int slot = Integer.parseInt(invKey);
                    if (slot >= 0 && slot < inventory.length) {
                        inventory[slot] = config.getItemStack(basePath + ".inventory." + invKey);
                    }
                }
                group.setSharedInventory(inventory);
            }

            if (!group.isEmpty()) {
                chestGroups.put(groupId, group);
            }
        }
    }

    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location deserializeLocation(String str) {
        String[] parts = str.split(",");
        if (parts.length != 4) return null;
        var world = plugin.getServer().getWorld(parts[0]);
        if (world == null) return null;
        return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }
}
