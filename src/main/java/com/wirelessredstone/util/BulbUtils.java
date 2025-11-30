package com.wirelessredstone.util;

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
}
