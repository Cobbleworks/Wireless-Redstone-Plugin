package com.wirelessredstone.model;

import com.wirelessredstone.item.BulbVariant;
import org.bukkit.Material;

import java.util.UUID;

/**
 * Represents a group of linked wireless bulbs/lamps.
 * Extends BaseGroup with bulb-specific functionality.
 */
public class BulbGroup extends BaseGroup {

    private boolean lit;
    private BulbVariant.BulbType bulbType;
    private Material variantMaterial; // Specific material for the icon (e.g., WAXED_EXPOSED_COPPER_BULB)

    public BulbGroup(UUID groupId, int maxSize) {
        super(groupId, maxSize);
        this.lit = false;
        this.bulbType = BulbVariant.BulbType.COPPER_BULB;
        this.variantMaterial = null;
    }

    public BulbGroup(UUID groupId, int maxSize, UUID ownerUuid, BulbVariant.BulbType bulbType) {
        super(groupId, maxSize, ownerUuid);
        this.lit = false;
        this.bulbType = bulbType != null ? bulbType : BulbVariant.BulbType.COPPER_BULB;
        this.variantMaterial = null;
    }

    public boolean isLit() {
        return lit;
    }

    public void setLit(boolean lit) {
        this.lit = lit;
    }

    public BulbVariant.BulbType getBulbType() {
        return bulbType;
    }

    public void setBulbType(BulbVariant.BulbType bulbType) {
        this.bulbType = bulbType;
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
     * @param variantMaterial The material to use (e.g., WAXED_EXPOSED_COPPER_BULB)
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
        return bulbType == BulbVariant.BulbType.REDSTONE_LAMP 
                ? Material.REDSTONE_LAMP 
                : Material.COPPER_BULB;
    }
}
