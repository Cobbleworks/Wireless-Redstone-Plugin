package com.wirelessredstone.item;

import org.bukkit.Material;

public enum ChestVariant {
    CHEST("--chest", "Wireless Chest", Material.CHEST, ContainerType.CHEST),
    // Copper Chests
    COPPER_CHEST("--copper", "Wireless Copper Chest", Material.COPPER_CHEST, ContainerType.COPPER_CHEST),
    EXPOSED_COPPER_CHEST("--copper-exposed", "Wireless Exposed Copper Chest", Material.EXPOSED_COPPER_CHEST, ContainerType.COPPER_CHEST),
    WEATHERED_COPPER_CHEST("--copper-weathered", "Wireless Weathered Copper Chest", Material.WEATHERED_COPPER_CHEST, ContainerType.COPPER_CHEST),
    OXIDIZED_COPPER_CHEST("--copper-oxidized", "Wireless Oxidized Copper Chest", Material.OXIDIZED_COPPER_CHEST, ContainerType.COPPER_CHEST),
    WAXED_COPPER_CHEST("--copper-waxed", "Wireless Waxed Copper Chest", Material.WAXED_COPPER_CHEST, ContainerType.COPPER_CHEST),
    WAXED_EXPOSED_COPPER_CHEST("--copper-waxed-exposed", "Wireless Waxed Exposed Copper Chest", Material.WAXED_EXPOSED_COPPER_CHEST, ContainerType.COPPER_CHEST),
    WAXED_WEATHERED_COPPER_CHEST("--copper-waxed-weathered", "Wireless Waxed Weathered Copper Chest", Material.WAXED_WEATHERED_COPPER_CHEST, ContainerType.COPPER_CHEST),
    WAXED_OXIDIZED_COPPER_CHEST("--copper-waxed-oxidized", "Wireless Waxed Oxidized Copper Chest", Material.WAXED_OXIDIZED_COPPER_CHEST, ContainerType.COPPER_CHEST),
    // Shulker Boxes
    WHITE_SHULKER("--white", "Wireless White Shulker Box", Material.WHITE_SHULKER_BOX, ContainerType.SHULKER),
    ORANGE_SHULKER("--orange", "Wireless Orange Shulker Box", Material.ORANGE_SHULKER_BOX, ContainerType.SHULKER),
    MAGENTA_SHULKER("--magenta", "Wireless Magenta Shulker Box", Material.MAGENTA_SHULKER_BOX, ContainerType.SHULKER),
    LIGHT_BLUE_SHULKER("--lightblue", "Wireless Light Blue Shulker Box", Material.LIGHT_BLUE_SHULKER_BOX, ContainerType.SHULKER),
    YELLOW_SHULKER("--yellow", "Wireless Yellow Shulker Box", Material.YELLOW_SHULKER_BOX, ContainerType.SHULKER),
    LIME_SHULKER("--lime", "Wireless Lime Shulker Box", Material.LIME_SHULKER_BOX, ContainerType.SHULKER),
    PINK_SHULKER("--pink", "Wireless Pink Shulker Box", Material.PINK_SHULKER_BOX, ContainerType.SHULKER),
    GRAY_SHULKER("--gray", "Wireless Gray Shulker Box", Material.GRAY_SHULKER_BOX, ContainerType.SHULKER),
    LIGHT_GRAY_SHULKER("--lightgray", "Wireless Light Gray Shulker Box", Material.LIGHT_GRAY_SHULKER_BOX, ContainerType.SHULKER),
    CYAN_SHULKER("--cyan", "Wireless Cyan Shulker Box", Material.CYAN_SHULKER_BOX, ContainerType.SHULKER),
    PURPLE_SHULKER("--purple", "Wireless Purple Shulker Box", Material.PURPLE_SHULKER_BOX, ContainerType.SHULKER),
    BLUE_SHULKER("--blue", "Wireless Blue Shulker Box", Material.BLUE_SHULKER_BOX, ContainerType.SHULKER),
    BROWN_SHULKER("--brown", "Wireless Brown Shulker Box", Material.BROWN_SHULKER_BOX, ContainerType.SHULKER),
    GREEN_SHULKER("--green", "Wireless Green Shulker Box", Material.GREEN_SHULKER_BOX, ContainerType.SHULKER),
    RED_SHULKER("--red", "Wireless Red Shulker Box", Material.RED_SHULKER_BOX, ContainerType.SHULKER),
    BLACK_SHULKER("--black", "Wireless Black Shulker Box", Material.BLACK_SHULKER_BOX, ContainerType.SHULKER),
    SHULKER("--shulker", "Wireless Shulker Box", Material.SHULKER_BOX, ContainerType.SHULKER);

    private final String arg;
    private final String displayName;
    private final Material material;
    private final ContainerType containerType;

    ChestVariant(String arg, String displayName, Material material, ContainerType containerType) {
        this.arg = arg;
        this.displayName = displayName;
        this.material = material;
        this.containerType = containerType;
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

    public ContainerType getContainerType() {
        return containerType;
    }

    public static ChestVariant fromArg(String arg) {
        for (ChestVariant variant : values()) {
            if (variant.arg.equalsIgnoreCase(arg)) {
                return variant;
            }
        }
        return null;
    }

    public static ChestVariant fromMaterial(Material material) {
        for (ChestVariant variant : values()) {
            if (variant.material == material) {
                return variant;
            }
        }
        return null;
    }

    public static boolean isWirelessContainerMaterial(Material material) {
        return fromMaterial(material) != null;
    }

    public static boolean isShulkerBox(Material material) {
        return material == Material.SHULKER_BOX ||
               material == Material.WHITE_SHULKER_BOX ||
               material == Material.ORANGE_SHULKER_BOX ||
               material == Material.MAGENTA_SHULKER_BOX ||
               material == Material.LIGHT_BLUE_SHULKER_BOX ||
               material == Material.YELLOW_SHULKER_BOX ||
               material == Material.LIME_SHULKER_BOX ||
               material == Material.PINK_SHULKER_BOX ||
               material == Material.GRAY_SHULKER_BOX ||
               material == Material.LIGHT_GRAY_SHULKER_BOX ||
               material == Material.CYAN_SHULKER_BOX ||
               material == Material.PURPLE_SHULKER_BOX ||
               material == Material.BLUE_SHULKER_BOX ||
               material == Material.BROWN_SHULKER_BOX ||
               material == Material.GREEN_SHULKER_BOX ||
               material == Material.RED_SHULKER_BOX ||
               material == Material.BLACK_SHULKER_BOX;
    }

    public static boolean isCopperChest(Material material) {
        return material == Material.COPPER_CHEST ||
               material == Material.EXPOSED_COPPER_CHEST ||
               material == Material.WEATHERED_COPPER_CHEST ||
               material == Material.OXIDIZED_COPPER_CHEST ||
               material == Material.WAXED_COPPER_CHEST ||
               material == Material.WAXED_EXPOSED_COPPER_CHEST ||
               material == Material.WAXED_WEATHERED_COPPER_CHEST ||
               material == Material.WAXED_OXIDIZED_COPPER_CHEST;
    }

    public enum ContainerType {
        CHEST,
        SHULKER,
        COPPER_CHEST
    }
}
