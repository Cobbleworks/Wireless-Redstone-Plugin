package com.wirelessredstone.model;

import org.bukkit.Material;

import java.util.UUID;

public class Category {

    private final UUID categoryId;
    private final UUID ownerUuid;
    private String name;
    private Material icon;

    public Category(UUID categoryId, UUID ownerUuid, String name) {
        this.categoryId = categoryId;
        this.ownerUuid = ownerUuid;
        this.name = name;
        this.icon = Material.CHEST;
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
