package com.wirelessredstone.manager;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.item.BulbVariant;
import com.wirelessredstone.model.BulbPair;
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
    private final Map<UUID, BulbPair> bulbPairs = new ConcurrentHashMap<>();
    private final Map<Location, UUID> locationToPairId = new ConcurrentHashMap<>();
    
    public static final NamespacedKey WIRELESS_BULB_KEY;
    public static final NamespacedKey PAIR_ID_KEY;
    public static final NamespacedKey BULB_INDEX_KEY;
    public static final NamespacedKey BULB_TYPE_KEY;
    public static final NamespacedKey OWNER_KEY;

    static {
        WIRELESS_BULB_KEY = new NamespacedKey("wirelessredstone", "wireless_bulb");
        PAIR_ID_KEY = new NamespacedKey("wirelessredstone", "pair_id");
        BULB_INDEX_KEY = new NamespacedKey("wirelessredstone", "bulb_index");
        BULB_TYPE_KEY = new NamespacedKey("wirelessredstone", "bulb_type");
        OWNER_KEY = new NamespacedKey("wirelessredstone", "owner");
    }

    public LinkedBulbManager(WirelessRedstonePlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

    public UUID createNewPairId() {
        return UUID.randomUUID();
    }

    public boolean isWirelessBulb(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(WIRELESS_BULB_KEY, PersistentDataType.BYTE);
    }

    public Optional<UUID> getPairId(ItemStack item) {
        if (!isWirelessBulb(item)) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        String pairIdStr = meta.getPersistentDataContainer().get(PAIR_ID_KEY, PersistentDataType.STRING);
        return pairIdStr != null ? Optional.of(UUID.fromString(pairIdStr)) : Optional.empty();
    }

    public Optional<Integer> getBulbIndex(ItemStack item) {
        if (!isWirelessBulb(item)) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        Integer index = meta.getPersistentDataContainer().get(BULB_INDEX_KEY, PersistentDataType.INTEGER);
        return Optional.ofNullable(index);
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

    public void registerPlacedBulb(Location location, UUID pairId, int bulbIndex, UUID ownerUuid, BulbVariant.BulbType bulbType) {
        BulbPair pair = bulbPairs.computeIfAbsent(pairId, id -> new BulbPair(id, ownerUuid, bulbType));
        pair.setLocation(bulbIndex, location);
        if (ownerUuid != null && pair.getOwnerUuid() == null) {
            pair.setOwnerUuid(ownerUuid);
        }
        if (bulbType != null) {
            pair.setBulbType(bulbType);
        }
        locationToPairId.put(location, pairId);
        saveData();
    }

    public void unregisterBulb(Location location) {
        UUID pairId = locationToPairId.remove(location);
        if (pairId != null) {
            BulbPair pair = bulbPairs.get(pairId);
            if (pair != null) {
                pair.removeLocation(location);
                if (pair.isEmpty()) {
                    bulbPairs.remove(pairId);
                }
            }
        }
        saveData();
    }

    public void removePair(UUID pairId) {
        BulbPair pair = bulbPairs.remove(pairId);
        if (pair != null) {
            if (pair.getLocation1() != null) {
                locationToPairId.remove(pair.getLocation1());
            }
            if (pair.getLocation2() != null) {
                locationToPairId.remove(pair.getLocation2());
            }
        }
        saveData();
    }

    public Optional<BulbPair> getPairById(UUID pairId) {
        return Optional.ofNullable(bulbPairs.get(pairId));
    }

    public Optional<BulbPair> getPairByLocation(Location location) {
        UUID pairId = locationToPairId.get(location);
        return pairId != null ? Optional.ofNullable(bulbPairs.get(pairId)) : Optional.empty();
    }

    public Optional<Location> getLinkedBulbLocation(Location location) {
        return getPairByLocation(location).flatMap(pair -> pair.getOtherLocation(location));
    }

    public boolean isWirelessBulbLocation(Location location) {
        return locationToPairId.containsKey(location);
    }

    public Collection<BulbPair> getAllPairs() {
        return bulbPairs.values();
    }

    public List<BulbPair> getPairsByOwner(UUID ownerUuid) {
        return bulbPairs.values().stream()
                .filter(pair -> ownerUuid.equals(pair.getOwnerUuid()))
                .collect(Collectors.toList());
    }

    public List<BulbPair> getAllPlacedPairs() {
        return bulbPairs.values().stream()
                .filter(pair -> pair.getLocation1() != null || pair.getLocation2() != null)
                .collect(Collectors.toList());
    }

    public void saveData() {
        File dataFile = new File(plugin.getDataFolder(), "bulbs.yml");
        FileConfiguration config = new YamlConfiguration();

        int index = 0;
        for (Map.Entry<UUID, BulbPair> entry : bulbPairs.entrySet()) {
            BulbPair pair = entry.getValue();
            String basePath = "pairs." + index;
            config.set(basePath + ".id", entry.getKey().toString());
            config.set(basePath + ".lit", pair.isLit());
            config.set(basePath + ".bulbType", pair.getBulbType().name());
            config.set(basePath + ".showSyncMessages", pair.isShowSyncMessages());

            if (pair.getOwnerUuid() != null) {
                config.set(basePath + ".owner", pair.getOwnerUuid().toString());
            }
            if (pair.getCustomName() != null) {
                config.set(basePath + ".customName", pair.getCustomName());
            }
            if (pair.getLocation1() != null) {
                config.set(basePath + ".loc1", serializeLocation(pair.getLocation1()));
            }
            if (pair.getLocation2() != null) {
                config.set(basePath + ".loc2", serializeLocation(pair.getLocation2()));
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
        var pairsSection = config.getConfigurationSection("pairs");
        if (pairsSection == null) return;

        for (String key : pairsSection.getKeys(false)) {
            String basePath = "pairs." + key;
            String idStr = config.getString(basePath + ".id");
            if (idStr == null) continue;

            UUID pairId = UUID.fromString(idStr);
            
            String ownerStr = config.getString(basePath + ".owner");
            UUID ownerUuid = ownerStr != null ? UUID.fromString(ownerStr) : null;
            
            String bulbTypeStr = config.getString(basePath + ".bulbType", "COPPER_BULB");
            BulbVariant.BulbType bulbType;
            try {
                bulbType = BulbVariant.BulbType.valueOf(bulbTypeStr);
            } catch (IllegalArgumentException e) {
                bulbType = BulbVariant.BulbType.COPPER_BULB;
            }
            
            BulbPair pair = new BulbPair(pairId, ownerUuid, bulbType);
            pair.setLit(config.getBoolean(basePath + ".lit", false));
            pair.setShowSyncMessages(config.getBoolean(basePath + ".showSyncMessages", true));
            pair.setCustomName(config.getString(basePath + ".customName"));

            String loc1Str = config.getString(basePath + ".loc1");
            String loc2Str = config.getString(basePath + ".loc2");

            if (loc1Str != null) {
                Location loc1 = deserializeLocation(loc1Str);
                if (loc1 != null) {
                    pair.setLocation(0, loc1);
                    locationToPairId.put(loc1, pairId);
                }
            }
            if (loc2Str != null) {
                Location loc2 = deserializeLocation(loc2Str);
                if (loc2 != null) {
                    pair.setLocation(1, loc2);
                    locationToPairId.put(loc2, pairId);
                }
            }

            if (!pair.isEmpty()) {
                bulbPairs.put(pairId, pair);
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
