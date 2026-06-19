package com.wirelessredstone.model;

import com.wirelessredstone.item.ChestVariant;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents a group of linked wireless containers (chests, shulkers, copper chests, barrels).
 * Extends BaseGroup with container-specific functionality.
 */
public class ChestGroup extends BaseGroup {

    public static final int DEFAULT_INVENTORY_SIZE = 27;
    public static final int LARGE_CHEST_INVENTORY_SIZE = 54;

    private ItemStack[] sharedInventory;
    private int inventorySize;
    private ChestVariant.ContainerType containerType;
    private Material variantMaterial; // Specific material for the icon (e.g., BLUE_SHULKER_BOX, WAXED_EXPOSED_COPPER_CHEST)

    public ChestGroup(UUID groupId, int maxSize) {
        super(groupId, maxSize);
        this.sharedInventory = new ItemStack[DEFAULT_INVENTORY_SIZE];
        this.inventorySize = DEFAULT_INVENTORY_SIZE;
        this.containerType = ChestVariant.ContainerType.CHEST;
        this.variantMaterial = null;
    }

    public ChestGroup(UUID groupId, int maxSize, UUID ownerUuid) {
        super(groupId, maxSize, ownerUuid);
        this.sharedInventory = new ItemStack[DEFAULT_INVENTORY_SIZE];
        this.inventorySize = DEFAULT_INVENTORY_SIZE;
        this.containerType = ChestVariant.ContainerType.CHEST;
        this.variantMaterial = null;
    }

    public ChestGroup(UUID groupId, int maxSize, UUID ownerUuid, ChestVariant.ContainerType containerType) {
        super(groupId, maxSize, ownerUuid);
        this.sharedInventory = new ItemStack[DEFAULT_INVENTORY_SIZE];
        this.inventorySize = DEFAULT_INVENTORY_SIZE;
        this.containerType = containerType != null ? containerType : ChestVariant.ContainerType.CHEST;
        this.variantMaterial = null;
    }

    public ItemStack[] getSharedInventory() {
        return sharedInventory;
    }

    public void setSharedInventory(ItemStack[] inventory) {
        if (inventory == null) return;

        this.inventorySize = Math.max(DEFAULT_INVENTORY_SIZE, inventory.length);
        this.sharedInventory = cloneInventory(inventory, inventorySize);
    }

    public void updateSharedInventory(ItemStack[] inventory) {
        if (inventory == null) return;

        this.inventorySize = Math.max(DEFAULT_INVENTORY_SIZE, inventory.length);
        this.sharedInventory = cloneInventory(inventory, inventorySize);
    }

    public int getInventorySize() {
        return inventorySize;
    }

    public void setInventorySize(int inventorySize) {
        this.inventorySize = Math.max(DEFAULT_INVENTORY_SIZE, inventorySize);
        if (sharedInventory.length != this.inventorySize) {
            this.sharedInventory = cloneInventory(sharedInventory, this.inventorySize);
        }
    }

    private ItemStack[] cloneInventory(ItemStack[] inventory, int size) {
        ItemStack[] cloned = new ItemStack[size];
        for (int i = 0; i < Math.min(inventory.length, size); i++) {
            cloned[i] = inventory[i] != null ? inventory[i].clone() : null;
        }
        return cloned;
    }

    public ChestVariant.ContainerType getContainerType() {
        return containerType;
    }

    public void setContainerType(ChestVariant.ContainerType containerType) {
        this.containerType = containerType;
    }

    /**
     * Gets the specific variant material used for this group's icon.
     * @return The variant material, or null if not set
     */
    public Material getVariantMaterial() {
        return variantMaterial;
    }

    /**
     * Sets the specific variant material to use for this group's icon.
     * @param variantMaterial The material to use (e.g., BLUE_SHULKER_BOX, WAXED_EXPOSED_COPPER_CHEST)
     */
    public void setVariantMaterial(Material variantMaterial) {
        this.variantMaterial = variantMaterial;
    }

    @Override
    public Material getDefaultIcon() {
        // Use specific variant material if set, otherwise fall back to type-based defaults
        if (variantMaterial != null) {
            return variantMaterial;
        }
        if (containerType == null) return Material.CHEST;
        return switch (containerType) {
            case CHEST -> Material.CHEST;
            case SHULKER -> Material.SHULKER_BOX;
            case COPPER_CHEST -> Material.COPPER_CHEST;
            case BARREL -> Material.BARREL;
        };
    }
}
