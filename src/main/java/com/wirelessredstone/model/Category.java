package com.wirelessredstone.model;

import org.bukkit.Material;

import java.util.UUID;

public class Category {

    public static final Material DEFAULT_ICON = Material.ENDER_CHEST;

    private final UUID categoryId;
    private final UUID ownerUuid;
    private String name;
    private String description;
    private Material icon;

    public Category(UUID categoryId, UUID ownerUuid, String name) {
        this.categoryId = categoryId;
        this.ownerUuid = ownerUuid;
        this.name = name;
        this.description = null;
        this.icon = null;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null || description.isBlank() ? null : description;
    }

    public Material getIcon() {
        return icon;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
    }

    public String getDisplayName() {
        return name != null ? name : categoryId.toString().substring(0, 8);
    }
}
