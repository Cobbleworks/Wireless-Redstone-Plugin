package com.wirelessredstone.util;

import com.wirelessredstone.item.BulbVariant;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class BulbUtils {

    public static boolean isCopperBulb(Block block) {
        return isCopperBulb(block.getType());
    }

    public static boolean isCopperBulb(Material type) {
        return type == Material.COPPER_BULB ||
               type == Material.EXPOSED_COPPER_BULB ||
               type == Material.WEATHERED_COPPER_BULB ||
               type == Material.OXIDIZED_COPPER_BULB ||
               type == Material.WAXED_COPPER_BULB ||
               type == Material.WAXED_EXPOSED_COPPER_BULB ||
               type == Material.WAXED_WEATHERED_COPPER_BULB ||
               type == Material.WAXED_OXIDIZED_COPPER_BULB;
    }

    public static boolean isRedstoneLamp(Block block) {
        return isRedstoneLamp(block.getType());
    }

    public static boolean isRedstoneLamp(Material type) {
        return type == Material.REDSTONE_LAMP;
    }

    public static boolean isWirelessCompatibleBlock(Block block) {
        return isCopperBulb(block) || isRedstoneLamp(block);
    }

    public static boolean isWirelessCompatibleBlock(Material type) {
        return isCopperBulb(type) || isRedstoneLamp(type);
    }

    /**
     * Gets the BulbType for a given material.
     * @return The BulbType, or null if the material is not a valid bulb/lamp.
     */
    public static BulbVariant.BulbType getBulbTypeFromMaterial(Material material) {
        return switch (material) {
            case COPPER_BULB, WAXED_COPPER_BULB,
                 EXPOSED_COPPER_BULB, WAXED_EXPOSED_COPPER_BULB,
                 WEATHERED_COPPER_BULB, WAXED_WEATHERED_COPPER_BULB,
                 OXIDIZED_COPPER_BULB, WAXED_OXIDIZED_COPPER_BULB -> BulbVariant.BulbType.COPPER_BULB;
            case REDSTONE_LAMP -> BulbVariant.BulbType.REDSTONE_LAMP;
            default -> null;
        };
    }

    /**
     * Converts a non-waxed copper bulb material to its waxed equivalent.
     * Returns the original material if it is already waxed or not a copper bulb.
     */
    public static Material toWaxedCopperBulb(Material material) {
        return switch (material) {
            case COPPER_BULB -> Material.WAXED_COPPER_BULB;
            case EXPOSED_COPPER_BULB -> Material.WAXED_EXPOSED_COPPER_BULB;
            case WEATHERED_COPPER_BULB -> Material.WAXED_WEATHERED_COPPER_BULB;
            case OXIDIZED_COPPER_BULB -> Material.WAXED_OXIDIZED_COPPER_BULB;
            default -> material;
        };
    }
}
