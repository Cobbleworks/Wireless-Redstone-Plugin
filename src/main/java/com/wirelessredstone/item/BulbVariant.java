package com.wirelessredstone.item;

import org.bukkit.Material;

public enum BulbVariant {
    COPPER("--copper", "Wireless Copper Bulb", Material.COPPER_BULB, BulbType.COPPER_BULB),
    EXPOSED("--exposed", "Wireless Exposed Copper Bulb", Material.EXPOSED_COPPER_BULB, BulbType.COPPER_BULB),
    WEATHERED("--weathered", "Wireless Weathered Copper Bulb", Material.WEATHERED_COPPER_BULB, BulbType.COPPER_BULB),
    OXIDIZED("--oxidized", "Wireless Oxidized Copper Bulb", Material.OXIDIZED_COPPER_BULB, BulbType.COPPER_BULB),
    REDSTONE_LAMP("--lamp", "Wireless Redstone Lamp", Material.REDSTONE_LAMP, BulbType.REDSTONE_LAMP);

    private final String arg;
    private final String displayName;
    private final Material material;
    private final BulbType bulbType;

    BulbVariant(String arg, String displayName, Material material, BulbType bulbType) {
        this.arg = arg;
        this.displayName = displayName;
        this.material = material;
        this.bulbType = bulbType;
    }

    public String getArg() {
        return arg;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public BulbType getBulbType() {
        return bulbType;
    }

    public static BulbVariant fromArg(String arg) {
        for (BulbVariant variant : values()) {
            if (variant.arg.equalsIgnoreCase(arg)) {
                return variant;
            }
        }
        return null;
    }

    public static BulbVariant fromMaterial(Material material) {
        for (BulbVariant variant : values()) {
            if (variant.material == material) {
                return variant;
            }
        }
        return null;
    }

    public enum BulbType {
        COPPER_BULB,
        REDSTONE_LAMP
    }
}
