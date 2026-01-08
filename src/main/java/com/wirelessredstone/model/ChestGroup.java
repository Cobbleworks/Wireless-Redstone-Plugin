package com.wirelessredstone.model;

import com.wirelessredstone.item.ChestVariant;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents a group of linked wireless containers (chests, shulkers, copper chests).
 * Extends BaseGroup with container-specific functionality.
 */
public class ChestGroup extends BaseGroup {

    private ItemStack[] sharedInventory;
    private ChestVariant.ContainerType containerType;

    public ChestGroup(UUID groupId, int maxSize) {
        super(groupId, maxSize);
        this.sharedInventory = new ItemStack[27];
        this.containerType = ChestVariant.ContainerType.CHEST;
    }

    public ChestGroup(UUID groupId, int maxSize, UUID ownerUuid) {
        super(groupId, maxSize, ownerUuid);
        this.sharedInventory = new ItemStack[27];
        this.containerType = ChestVariant.ContainerType.CHEST;
    }

    public ChestGroup(UUID groupId, int maxSize, UUID ownerUuid, ChestVariant.ContainerType containerType) {
        super(groupId, maxSize, ownerUuid);
        this.sharedInventory = new ItemStack[27];
        this.containerType = containerType != null ? containerType : ChestVariant.ContainerType.CHEST;
    }

    public ItemStack[] getSharedInventory() {
        return sharedInventory;
    }

    public void setSharedInventory(ItemStack[] inventory) {
        if (inventory != null && inventory.length == 27) {
            this.sharedInventory = inventory;
        }
    }

    public void updateSharedInventory(ItemStack[] inventory) {
        if (inventory == null) return;
        for (int i = 0; i < Math.min(inventory.length, 27); i++) {
            this.sharedInventory[i] = inventory[i] != null ? inventory[i].clone() : null;
        }
    }

    public ChestVariant.ContainerType getContainerType() {
        return containerType;
    }

    public void setContainerType(ChestVariant.ContainerType containerType) {
        this.containerType = containerType;
    }

    @Override
    public Material getDefaultIcon() {
        if (containerType == null) return Material.CHEST;
        return switch (containerType) {
            case CHEST -> Material.CHEST;
            case SHULKER -> Material.SHULKER_BOX;
            case COPPER_CHEST -> Material.COPPER_BLOCK;
            case BARREL -> Material.BARREL;
        };
    }
}
