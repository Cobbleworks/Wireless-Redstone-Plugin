package com.wirelessredstone.model;

import java.util.UUID;

public class Category {

    private final UUID categoryId;
    private final UUID ownerUuid;
    private String name;
    private String description;

    public Category(UUID categoryId, UUID ownerUuid, String name) {
        this.categoryId = categoryId;
        this.ownerUuid = ownerUuid;
        this.name = name;
        this.description = null;
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

    public String getDisplayName() {
        return name != null ? name : categoryId.toString().substring(0, 8);
    }
}
