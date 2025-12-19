package com.wirelessredstone.item;

import com.wirelessredstone.manager.LinkedChestManager;
import com.wirelessredstone.model.ChestGroup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WirelessChestFactory {

    public static ItemStack[] createLinkedChests(UUID groupId, UUID ownerUuid, int count) {
        return createLinkedContainers(groupId, ChestVariant.CHEST, ownerUuid, count);
    }

    public static ItemStack[] createLinkedContainers(UUID groupId, ChestVariant variant, UUID ownerUuid, int count) {
        ItemStack[] containers = new ItemStack[count];
        for (int i = 0; i < count; i++) {
            String label = ChestGroup.getIndexLabel(i);
            containers[i] = createContainer(groupId, i, variant.getDisplayName() + " " + label, variant, ownerUuid, count);
        }
        return containers;
    }

    public static ItemStack[] createExtensionContainers(UUID groupId, ChestVariant variant, UUID ownerUuid,
                                                         int startIndex, int count, int newGroupSize) {
        ItemStack[] containers = new ItemStack[count];
        for (int i = 0; i < count; i++) {
            int index = startIndex + i;
            String label = ChestGroup.getIndexLabel(index);
            containers[i] = createContainer(groupId, index, variant.getDisplayName() + " " + label, variant, ownerUuid, newGroupSize);
        }
        return containers;
    }

    /**
     * Creates recovery containers for specific unplaced indices in an existing group.
     * Used to recover lost or missing wireless containers.
     */
    public static ItemStack[] createRecoveryContainers(UUID groupId, ChestVariant variant, UUID ownerUuid,
                                                        List<Integer> unplacedIndices, int groupSize) {
        ItemStack[] containers = new ItemStack[unplacedIndices.size()];
        for (int i = 0; i < unplacedIndices.size(); i++) {
            int index = unplacedIndices.get(i);
            String label = ChestGroup.getIndexLabel(index);
            containers[i] = createContainer(groupId, index, variant.getDisplayName() + " " + label, variant, ownerUuid, groupSize);
        }
        return containers;
    }

    private static ItemStack createContainer(UUID groupId, int index, String name, ChestVariant variant, UUID ownerUuid, int groupSize) {
        ItemStack container = new ItemStack(variant.getMaterial());
        ItemMeta meta = container.getItemMeta();

        NamedTextColor nameColor = switch (variant.getContainerType()) {
            case SHULKER -> NamedTextColor.LIGHT_PURPLE;
            case COPPER_CHEST -> NamedTextColor.AQUA;
            default -> NamedTextColor.GOLD;
        };

        meta.displayName(Component.text(name, nameColor)
                .decoration(TextDecoration.ITALIC, false));

        String typeDesc = switch (variant.getContainerType()) {
            case SHULKER -> "Linked Wireless Shulker Box";
            case COPPER_CHEST -> "Linked Wireless Copper Chest";
            default -> "Linked Wireless Chest";
        };

        meta.lore(List.of(
                Component.text(typeDesc, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Group ID: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(groupId.toString().substring(0, 8), nameColor))
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Group Size: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(String.valueOf(groupSize), nameColor))
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("⚡ Not yet placed", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("This container syncs contents with its group!", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        var pdc = meta.getPersistentDataContainer();
        pdc.set(LinkedChestManager.WIRELESS_CHEST_KEY, PersistentDataType.BYTE, (byte) 1);
        pdc.set(LinkedChestManager.CHEST_GROUP_ID_KEY, PersistentDataType.STRING, groupId.toString());
        pdc.set(LinkedChestManager.CHEST_INDEX_KEY, PersistentDataType.INTEGER, index);
        pdc.set(LinkedChestManager.CHEST_GROUP_SIZE_KEY, PersistentDataType.INTEGER, groupSize);
        pdc.set(LinkedChestManager.CHEST_CONTAINER_TYPE_KEY, PersistentDataType.STRING, variant.getContainerType().name());
        if (ownerUuid != null) {
            pdc.set(LinkedChestManager.CHEST_OWNER_KEY, PersistentDataType.STRING, ownerUuid.toString());
        }

        container.setItemMeta(meta);
        return container;
    }

    public static void updateLinkedContainerLore(ItemStack item, List<Location> linkedLocations, boolean isConnected, int placedCount, int groupSize, ChestVariant.ContainerType containerType) {
        if (item == null || !item.hasItemMeta()) return;
        
        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        
        if (!pdc.has(LinkedChestManager.WIRELESS_CHEST_KEY, PersistentDataType.BYTE)) return;
        
        String groupIdStr = pdc.get(LinkedChestManager.CHEST_GROUP_ID_KEY, PersistentDataType.STRING);
        Integer chestIndex = pdc.get(LinkedChestManager.CHEST_INDEX_KEY, PersistentDataType.INTEGER);
        
        if (groupIdStr == null || chestIndex == null) return;
        
        String containerLabel = ChestGroup.getIndexLabel(chestIndex);
        boolean isShulker = containerType == ChestVariant.ContainerType.SHULKER;
        boolean isCopperChest = containerType == ChestVariant.ContainerType.COPPER_CHEST;
        String containerName = isShulker ? "Shulker" : (isCopperChest ? "Copper Chest" : "Chest");
        String displayName = isConnected ? "⚡ Linked " + containerName + " " + containerLabel + " ⚡" : "Wireless " + containerName + " " + containerLabel;
        NamedTextColor nameColor = isConnected ? NamedTextColor.GREEN : (isShulker ? NamedTextColor.LIGHT_PURPLE : (isCopperChest ? NamedTextColor.AQUA : NamedTextColor.GOLD));
        
        meta.displayName(Component.text(displayName, nameColor)
                .decoration(TextDecoration.ITALIC, false));
        
        String typeDesc = isShulker ? "Linked Wireless Shulker Box" : (isCopperChest ? "Linked Wireless Copper Chest" : "Linked Wireless Chest");
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(typeDesc, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Group ID: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(groupIdStr.substring(0, 8), nameColor))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Placed: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(placedCount + "/" + groupSize, nameColor))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        
        if (isConnected && linkedLocations != null && !linkedLocations.isEmpty()) {
            lore.add(Component.text("⚡ Connected to " + linkedLocations.size() + " " + containerName.toLowerCase() + "(s):", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            int shown = 0;
            for (Location loc : linkedLocations) {
                if (shown >= 3) {
                    lore.add(Component.text("  ... and " + (linkedLocations.size() - shown) + " more", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                    break;
                }
                String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "Unknown";
                lore.add(Component.text("  " + worldName + ": ", NamedTextColor.GRAY)
                        .append(Component.text(loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(), NamedTextColor.WHITE))
                        .decoration(TextDecoration.ITALIC, false));
                shown++;
            }
        } else {
            lore.add(Component.text("⚡ No other " + containerName.toLowerCase() + "s placed yet", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("This container syncs contents with its group!", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    public static String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) return "Unknown";
        return String.format("%s (%d, %d, %d)", 
                location.getWorld().getName(),
                location.getBlockX(), 
                location.getBlockY(), 
                location.getBlockZ());
    }
}
