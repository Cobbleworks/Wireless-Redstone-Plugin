package com.wirelessredstone.manager;

import com.wirelessredstone.WirelessRedstonePlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Stores each player's preferred ordering for the entries in a menu context. */
public class MenuOrderManager {

    public static final String ROOT_CONTEXT = "root";

    private final WirelessRedstonePlugin plugin;
    private final Map<UUID, Map<String, List<String>>> orders = new HashMap<>();

    public MenuOrderManager(WirelessRedstonePlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

    public synchronized List<String> getOrder(UUID ownerUuid, String context) {
        List<String> order = orders
                .getOrDefault(ownerUuid, Collections.emptyMap())
                .get(normalizeContext(context));
        return order == null ? List.of() : List.copyOf(order);
    }

    public synchronized void setOrder(UUID ownerUuid, String context, List<String> entryKeys) {
        orders.computeIfAbsent(ownerUuid, ignored -> new HashMap<>())
                .put(normalizeContext(context), new ArrayList<>(entryKeys));
        saveData();
    }

    public synchronized void reloadData() {
        orders.clear();
        loadData();
    }

    public synchronized void saveData() {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, List<String>>> ownerEntry : orders.entrySet()) {
            for (Map.Entry<String, List<String>> contextEntry : ownerEntry.getValue().entrySet()) {
                String path = "orders." + ownerEntry.getKey() + "." + encodeContext(contextEntry.getKey());
                config.set(path, contextEntry.getValue());
            }
        }

        File dataFile = new File(plugin.getDataFolder(), "menu-orders.yml");
        try {
            plugin.getDataFolder().mkdirs();
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save menu order data: " + e.getMessage());
        }
    }

    private synchronized void loadData() {
        File dataFile = new File(plugin.getDataFolder(), "menu-orders.yml");
        if (!dataFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection ownersSection = config.getConfigurationSection("orders");
        if (ownersSection == null) return;

        for (String ownerKey : ownersSection.getKeys(false)) {
            UUID ownerUuid;
            try {
                ownerUuid = UUID.fromString(ownerKey);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            ConfigurationSection contextsSection = ownersSection.getConfigurationSection(ownerKey);
            if (contextsSection == null) continue;
            Map<String, List<String>> ownerOrders = orders.computeIfAbsent(ownerUuid, ignored -> new HashMap<>());
            for (String encodedContext : contextsSection.getKeys(false)) {
                try {
                    ownerOrders.put(decodeContext(encodedContext),
                            new ArrayList<>(contextsSection.getStringList(encodedContext)));
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Ignoring invalid menu order context for " + ownerUuid);
                }
            }
        }
    }

    private String normalizeContext(String context) {
        return context == null || context.isBlank() ? ROOT_CONTEXT : context;
    }

    private String encodeContext(String context) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(context.getBytes(StandardCharsets.UTF_8));
    }

    private String decodeContext(String context) {
        return new String(Base64.getUrlDecoder().decode(context), StandardCharsets.UTF_8);
    }
}
