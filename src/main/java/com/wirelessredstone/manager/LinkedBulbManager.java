package com.wirelessredstone.manager;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.item.BulbVariant;
import com.wirelessredstone.model.BulbGroup;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LinkedBulbManager {

    private final WirelessRedstonePlugin plugin;
    private final Map<UUID, BulbGroup> bulbGroups = new ConcurrentHashMap<>();
    private final Map<Location, UUID> locationToGroupId = new ConcurrentHashMap<>();
    
    public static final NamespacedKey WIRELESS_BULB_KEY;
    public static final NamespacedKey GROUP_ID_KEY;
    public static final NamespacedKey BULB_INDEX_KEY;
    public static final NamespacedKey BULB_TYPE_KEY;
    public static final NamespacedKey OWNER_KEY;
    public static final NamespacedKey GROUP_SIZE_KEY;

    static {
        WIRELESS_BULB_KEY = new NamespacedKey("wirelessredstone", "wireless_bulb");
        GROUP_ID_KEY = new NamespacedKey("wirelessredstone", "group_id");
        BULB_INDEX_KEY = new NamespacedKey("wirelessredstone", "bulb_index");
        BULB_TYPE_KEY = new NamespacedKey("wirelessredstone", "bulb_type");
        OWNER_KEY = new NamespacedKey("wirelessredstone", "owner");
        GROUP_SIZE_KEY = new NamespacedKey("wirelessredstone", "group_size");
    }

    public LinkedBulbManager(WirelessRedstonePlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

    /**
     * Normalizes a location to block coordinates only (removes yaw, pitch, and decimal parts).
     * This ensures consistent map key lookups regardless of how the Location was obtained.
     */
    private Location normalizeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public UUID createNewGroupId() {
        return UUID.randomUUID();
    }

    public boolean isWirelessBulb(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(WIRELESS_BULB_KEY, PersistentDataType.BYTE);
    }

    public Optional<UUID> getGroupId(ItemStack item) {
        if (!isWirelessBulb(item)) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        String groupIdStr = meta.getPersistentDataContainer().get(GROUP_ID_KEY, PersistentDataType.STRING);
        return groupIdStr != null ? Optional.of(UUID.fromString(groupIdStr)) : Optional.empty();
    }

    public Optional<Integer> getBulbIndex(ItemStack item) {
        if (!isWirelessBulb(item)) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        Integer index = meta.getPersistentDataContainer().get(BULB_INDEX_KEY, PersistentDataType.INTEGER);
        return Optional.ofNullable(index);
    }

    public Optional<Integer> getGroupSize(ItemStack item) {
        if (!isWirelessBulb(item)) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        Integer size = meta.getPersistentDataContainer().get(GROUP_SIZE_KEY, PersistentDataType.INTEGER);
        return Optional.ofNullable(size != null ? size : 2);
    }

    public Optional<BulbVariant.BulbType> getBulbType(ItemStack item) {
        if (!isWirelessBulb(item)) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        String typeStr = meta.getPersistentDataContainer().get(BULB_TYPE_KEY, PersistentDataType.STRING);
        if (typeStr == null) return Optional.of(BulbVariant.BulbType.COPPER_BULB);
        try {
            return Optional.of(BulbVariant.BulbType.valueOf(typeStr));
        } catch (IllegalArgumentException e) {
            return Optional.of(BulbVariant.BulbType.COPPER_BULB);
        }
    }

    public Optional<UUID> getOwnerUuid(ItemStack item) {
        if (!isWirelessBulb(item)) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        String ownerStr = meta.getPersistentDataContainer().get(OWNER_KEY, PersistentDataType.STRING);
        return ownerStr != null ? Optional.of(UUID.fromString(ownerStr)) : Optional.empty();
    }

    public void registerPlacedBulb(Location location, UUID groupId, int bulbIndex, UUID ownerUuid, BulbVariant.BulbType bulbType, int groupSize) {
        Location normalizedLoc = normalizeLocation(location);
        BulbGroup group = bulbGroups.computeIfAbsent(groupId, id -> new BulbGroup(id, groupSize, ownerUuid, bulbType));
        group.setLocation(bulbIndex, normalizedLoc);
        if (ownerUuid != null && group.getOwnerUuid() == null) {
            group.setOwnerUuid(ownerUuid);
        }
        if (bulbType != null) {
            group.setBulbType(bulbType);
        }
        locationToGroupId.put(normalizedLoc, groupId);
        saveData();
    }

    public void unregisterBulb(Location location) {
        Location normalizedLoc = normalizeLocation(location);
        UUID groupId = locationToGroupId.remove(normalizedLoc);
        if (groupId != null) {
            BulbGroup group = bulbGroups.get(groupId);
            if (group != null) {
                group.removeLocation(normalizedLoc);
                if (group.isEmpty()) {
                    bulbGroups.remove(groupId);
                }
            }
        }
        saveData();
    }

    /**
     * Unregisters a bulb and returns the group ID if the group was removed (empty after unregister).
     */
    public UUID unregisterBulbAndCheckGroupRemoval(Location location) {
        Location normalizedLoc = normalizeLocation(location);
        UUID groupId = locationToGroupId.remove(normalizedLoc);
        if (groupId != null) {
            BulbGroup group = bulbGroups.get(groupId);
            if (group != null) {
                group.removeLocation(normalizedLoc);
                if (group.isEmpty()) {
                    bulbGroups.remove(groupId);
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
        BulbGroup group = bulbGroups.remove(groupId);
        if (group != null) {
            for (Location loc : group.getLocations()) {
                if (loc != null) {
                    locationToGroupId.remove(normalizeLocation(loc));
                    if (removeBlocks && loc.isChunkLoaded()) {
                        loc.getBlock().setType(org.bukkit.Material.AIR);
                    }
                }
            }
        }
        saveData();
    }

    public Optional<BulbGroup> getGroupById(UUID groupId) {
        return Optional.ofNullable(bulbGroups.get(groupId));
    }

    public Optional<BulbGroup> getGroupByLocation(Location location) {
        UUID groupId = locationToGroupId.get(normalizeLocation(location));
        return groupId != null ? Optional.ofNullable(bulbGroups.get(groupId)) : Optional.empty();
    }

    public List<Location> getOtherBulbLocations(Location location) {
        return getGroupByLocation(location)
                .map(group -> group.getOtherLocations(location))
                .orElse(Collections.emptyList());
    }

    public boolean isWirelessBulbLocation(Location location) {
        return locationToGroupId.containsKey(normalizeLocation(location));
    }

    public Collection<BulbGroup> getAllGroups() {
        return bulbGroups.values();
    }

    public List<BulbGroup> getGroupsByOwner(UUID ownerUuid) {
        return bulbGroups.values().stream()
                .filter(group -> ownerUuid.equals(group.getOwnerUuid()))
                .collect(Collectors.toList());
    }

    public List<BulbGroup> getAllPlacedGroups() {
        return bulbGroups.values().stream()
                .filter(group -> group.getPlacedCount() > 0)
                .collect(Collectors.toList());
    }

    public void saveData() {
        File dataFile = new File(plugin.getDataFolder(), "bulbs.yml");
        FileConfiguration config = new YamlConfiguration();

        int index = 0;
        for (Map.Entry<UUID, BulbGroup> entry : bulbGroups.entrySet()) {
            BulbGroup group = entry.getValue();
            String basePath = "groups." + index;
            config.set(basePath + ".id", entry.getKey().toString());
            config.set(basePath + ".lit", group.isLit());
            config.set(basePath + ".bulbType", group.getBulbType().name());
            config.set(basePath + ".maxSize", group.getMaxSize());

            if (group.getOwnerUuid() != null) {
                config.set(basePath + ".owner", group.getOwnerUuid().toString());
            }
            if (group.getCustomName() != null) {
                config.set(basePath + ".customName", group.getCustomName());
            }
            if (group.getCustomIcon() != null) {
                config.set(basePath + ".customIcon", group.getCustomIcon().name());
            }
            
            List<Location> locations = group.getLocations();
            for (int i = 0; i < locations.size(); i++) {
                Location loc = locations.get(i);
                if (loc != null) {
                    config.set(basePath + ".locations." + i, serializeLocation(loc));
                }
            }
            index++;
        }

        try {
            plugin.getDataFolder().mkdirs();
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save bulb data: " + e.getMessage());
        }
    }

    public void loadData() {
        File dataFile = new File(plugin.getDataFolder(), "bulbs.yml");
        if (!dataFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        
        // Try loading new format (groups)
        var groupsSection = config.getConfigurationSection("groups");
        if (groupsSection != null) {
            loadNewFormat(config, groupsSection);
            return;
        }
        
        // Fall back to old format (pairs) for backwards compatibility
        var pairsSection = config.getConfigurationSection("pairs");
        if (pairsSection != null) {
            loadOldFormat(config, pairsSection);
        }
    }

    private void loadNewFormat(FileConfiguration config, org.bukkit.configuration.ConfigurationSection groupsSection) {
        for (String key : groupsSection.getKeys(false)) {
            String basePath = "groups." + key;
            String idStr = config.getString(basePath + ".id");
            if (idStr == null) continue;

            UUID groupId = UUID.fromString(idStr);
            
            String ownerStr = config.getString(basePath + ".owner");
            UUID ownerUuid = ownerStr != null ? UUID.fromString(ownerStr) : null;
            
            String bulbTypeStr = config.getString(basePath + ".bulbType", "COPPER_BULB");
            BulbVariant.BulbType bulbType;
            try {
                bulbType = BulbVariant.BulbType.valueOf(bulbTypeStr);
            } catch (IllegalArgumentException e) {
                bulbType = BulbVariant.BulbType.COPPER_BULB;
            }
            
            int maxSize = config.getInt(basePath + ".maxSize", 2);
            
            BulbGroup group = new BulbGroup(groupId, maxSize, ownerUuid, bulbType);
            group.setLit(config.getBoolean(basePath + ".lit", false));
            group.setCustomName(config.getString(basePath + ".customName"));
            
            String customIconStr = config.getString(basePath + ".customIcon");
            if (customIconStr != null) {
                try {
                    group.setCustomIcon(org.bukkit.Material.valueOf(customIconStr));
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

            if (!group.isEmpty()) {
                bulbGroups.put(groupId, group);
            }
        }
    }

    private void loadOldFormat(FileConfiguration config, org.bukkit.configuration.ConfigurationSection pairsSection) {
        // Backwards compatibility: load old pair format as groups of size 2
        for (String key : pairsSection.getKeys(false)) {
            String basePath = "pairs." + key;
            String idStr = config.getString(basePath + ".id");
            if (idStr == null) continue;

            UUID groupId = UUID.fromString(idStr);
            
            String ownerStr = config.getString(basePath + ".owner");
            UUID ownerUuid = ownerStr != null ? UUID.fromString(ownerStr) : null;
            
            String bulbTypeStr = config.getString(basePath + ".bulbType", "COPPER_BULB");
            BulbVariant.BulbType bulbType;
            try {
                bulbType = BulbVariant.BulbType.valueOf(bulbTypeStr);
            } catch (IllegalArgumentException e) {
                bulbType = BulbVariant.BulbType.COPPER_BULB;
            }
            
            BulbGroup group = new BulbGroup(groupId, 2, ownerUuid, bulbType);
            group.setLit(config.getBoolean(basePath + ".lit", false));
            group.setCustomName(config.getString(basePath + ".customName"));

            String loc1Str = config.getString(basePath + ".loc1");
            String loc2Str = config.getString(basePath + ".loc2");

            if (loc1Str != null) {
                Location loc1 = deserializeLocation(loc1Str);
                if (loc1 != null) {
                    group.setLocation(0, loc1);
                    locationToGroupId.put(loc1, groupId);
                }
            }
            if (loc2Str != null) {
                Location loc2 = deserializeLocation(loc2Str);
                if (loc2 != null) {
                    group.setLocation(1, loc2);
                    locationToGroupId.put(loc2, groupId);
                }
            }

            if (!group.isEmpty()) {
                bulbGroups.put(groupId, group);
            }
        }
        
        // Save in new format for future loads
        if (!bulbGroups.isEmpty()) {
            saveData();
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
