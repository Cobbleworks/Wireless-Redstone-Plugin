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

    public BulbGroup(UUID groupId, int maxSize) {
        super(groupId, maxSize);
        this.lit = false;
        this.bulbType = BulbVariant.BulbType.COPPER_BULB;
    }

    public BulbGroup(UUID groupId, int maxSize, UUID ownerUuid, BulbVariant.BulbType bulbType) {
        super(groupId, maxSize, ownerUuid);
        this.lit = false;
        this.bulbType = bulbType != null ? bulbType : BulbVariant.BulbType.COPPER_BULB;
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

    @Override
    public Material getDefaultIcon() {
        return bulbType == BulbVariant.BulbType.REDSTONE_LAMP 
                ? Material.REDSTONE_LAMP 
                : Material.COPPER_BULB;
    }
}
