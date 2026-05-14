package com.wirelessredstone.manager;

import com.wirelessredstone.WirelessRedstonePlugin;
import com.wirelessredstone.model.Category;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CategoryManager {

    public static final String UNCATEGORIZED_ID = "uncategorized";
    
    private final WirelessRedstonePlugin plugin;
    private final Map<UUID, Category> categories = new ConcurrentHashMap<>();

    public CategoryManager(WirelessRedstonePlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

    /**
     * Reloads all category data from disk, clearing existing data first.
     */
    public void reloadData() {
        categories.clear();
        loadData();
    }

    public UUID createCategory(UUID ownerUuid, String name) {
        UUID categoryId = UUID.randomUUID();
        Category category = new Category(categoryId, ownerUuid, name);
        categories.put(categoryId, category);
        saveData();
        return categoryId;
    }

    public Optional<Category> getCategoryById(UUID categoryId) {
        return Optional.ofNullable(categories.get(categoryId));
    }

    public List<Category> getCategoriesByOwner(UUID ownerUuid) {
        return categories.values().stream()
                .filter(cat -> ownerUuid.equals(cat.getOwnerUuid()))
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<Category> getAllCategories() {
        return new ArrayList<>(categories.values());
    }

    public void renameCategory(UUID categoryId, String newName) {
        Category category = categories.get(categoryId);
        if (category != null) {
            category.setName(newName);
            saveData();
        }
    }

    public void setCategoryDescription(UUID categoryId, String description) {
        Category category = categories.get(categoryId);
        if (category != null) {
            category.setDescription(description);
            saveData();
        }
    }

    public void setCategoryIcon(UUID categoryId, Material icon) {
        Category category = categories.get(categoryId);
        if (category != null) {
            category.setIcon(icon);
            saveData();
        }
    }

    public void deleteCategory(UUID categoryId) {
        categories.remove(categoryId);
        saveData();
    }

    public boolean categoryExists(UUID categoryId) {
        return categories.containsKey(categoryId);
    }

    /**
     * Finds a category by name (case-insensitive match).
     * @param name The name to search for
     * @return Optional containing the category if found
     */
    public Optional<Category> getCategoryByName(String name) {
        return categories.values().stream()
                .filter(cat -> cat.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public void saveData() {
        File dataFile = new File(plugin.getDataFolder(), "categories.yml");
        FileConfiguration config = new YamlConfiguration();

        int index = 0;
        for (Map.Entry<UUID, Category> entry : categories.entrySet()) {
            Category category = entry.getValue();
            String basePath = "categories." + index;
            config.set(basePath + ".id", entry.getKey().toString());
            config.set(basePath + ".name", category.getName());
            if (category.getDescription() != null) {
                config.set(basePath + ".description", category.getDescription());
            }
            
            if (category.getOwnerUuid() != null) {
                config.set(basePath + ".owner", category.getOwnerUuid().toString());
            }
            if (category.getIcon() != null) {
                config.set(basePath + ".icon", category.getIcon().name());
            }
            index++;
        }

        try {
            plugin.getDataFolder().mkdirs();
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save category data: " + e.getMessage());
        }
    }

    public void loadData() {
        File dataFile = new File(plugin.getDataFolder(), "categories.yml");
        if (!dataFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        
        var categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection == null) return;

        for (String key : categoriesSection.getKeys(false)) {
            String basePath = "categories." + key;
            String idStr = config.getString(basePath + ".id");
            if (idStr == null) continue;

            UUID categoryId = UUID.fromString(idStr);
            String name = config.getString(basePath + ".name", "Unnamed");
            
            String ownerStr = config.getString(basePath + ".owner");
            UUID ownerUuid = ownerStr != null ? UUID.fromString(ownerStr) : null;
            
            Category category = new Category(categoryId, ownerUuid, name);
            category.setDescription(config.getString(basePath + ".description"));
            
            String iconStr = config.getString(basePath + ".icon");
            if (iconStr != null) {
                try {
                    category.setIcon(Material.valueOf(iconStr));
                } catch (IllegalArgumentException ignored) {}
            }
            
            categories.put(categoryId, category);
        }
    }
}
