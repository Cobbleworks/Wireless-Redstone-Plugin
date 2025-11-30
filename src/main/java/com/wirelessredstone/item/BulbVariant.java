package com.wirelessredstone.item;

import org.bukkit.Material;

public enum BulbVariant {
    COPPER("--copper", "Wireless Copper Bulb", Material.COPPER_BULB),
    EXPOSED("--exposed", "Wireless Exposed Copper Bulb", Material.EXPOSED_COPPER_BULB),
    WEATHERED("--weathered", "Wireless Weathered Copper Bulb", Material.WEATHERED_COPPER_BULB),
    OXIDIZED("--oxidized", "Wireless Oxidized Copper Bulb", Material.OXIDIZED_COPPER_BULB);

    private final String arg;
    private final String displayName;
    private final Material material;

    BulbVariant(String arg, String displayName, Material material) {
        this.arg = arg;
        this.displayName = displayName;
        this.material = material;
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

    public static BulbVariant fromArg(String arg) {
        for (BulbVariant variant : values()) {
            if (variant.arg.equalsIgnoreCase(arg)) {
                return variant;
            }
        }
        return null;
    }
}
