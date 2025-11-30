package com.wirelessredstone.manager;

import com.wirelessredstone.WirelessRedstonePlugin;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LinkedBulbManager {

    private final WirelessRedstonePlugin plugin;
    private final Map<UUID, BulbPair> bulbPairs = new ConcurrentHashMap<>();
    private final Map<Location, UUID> locationToPairId = new ConcurrentHashMap<>();
    
    public static final NamespacedKey WIRELESS_BULB_KEY;
    public static final NamespacedKey PAIR_ID_KEY;
    public static final NamespacedKey BULB_INDEX_KEY;

    static {
        WIRELESS_BULB_KEY = new NamespacedKey("wirelessredstone", "wireless_bulb");
        PAIR_ID_KEY = new NamespacedKey("wirelessredstone", "pair_id");
        BULB_INDEX_KEY = new NamespacedKey("wirelessredstone", "bulb_index");
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

    public void registerPlacedBulb(Location location, UUID pairId, int bulbIndex) {
        BulbPair pair = bulbPairs.computeIfAbsent(pairId, id -> new BulbPair(id));
        pair.setLocation(bulbIndex, location);
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

    public java.util.Collection<BulbPair> getAllPairs() {
        return bulbPairs.values();
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
            BulbPair pair = new BulbPair(pairId);
            pair.setLit(config.getBoolean(basePath + ".lit", false));

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
