package com.wirelessredstone.item;

import com.wirelessredstone.manager.LinkedBulbManager;
import com.wirelessredstone.model.BulbGroup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WirelessBulbFactory {

    public static ItemStack[] createLinkedPair(UUID groupId, BulbVariant variant, UUID ownerUuid) {
        return createLinkedGroup(groupId, variant, ownerUuid, 2);
    }

    public static ItemStack[] createLinkedGroup(UUID groupId, BulbVariant variant, UUID ownerUuid, int count) {
        ItemStack[] bulbs = new ItemStack[count];
        for (int i = 0; i < count; i++) {
            String label = BulbGroup.getIndexLabel(i);
            bulbs[i] = createBulb(groupId, i, "Wireless Bulb " + label, variant, ownerUuid, count);
        }
        return bulbs;
    }

    public static ItemStack[] createExtensionBulbs(UUID groupId, BulbVariant variant, UUID ownerUuid, 
                                                    int startIndex, int count, int newGroupSize) {
        ItemStack[] bulbs = new ItemStack[count];
        for (int i = 0; i < count; i++) {
            int index = startIndex + i;
            String label = BulbGroup.getIndexLabel(index);
            bulbs[i] = createBulb(groupId, index, "Wireless Bulb " + label, variant, ownerUuid, newGroupSize);
        }
        return bulbs;
    }

    private static ItemStack createBulb(UUID groupId, int index, String name, BulbVariant variant, UUID ownerUuid, int groupSize) {
        ItemStack bulb = new ItemStack(variant.getMaterial());
        ItemMeta meta = bulb.getItemMeta();

        meta.displayName(Component.text(name, NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(List.of(
                Component.text("Linked " + variant.getDisplayName(), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Group ID: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(groupId.toString().substring(0, 8), NamedTextColor.DARK_AQUA))
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Group Size: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(String.valueOf(groupSize), NamedTextColor.DARK_AQUA))
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("⚡ Not yet placed", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("This bulb syncs with its group!", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        var pdc = meta.getPersistentDataContainer();
        pdc.set(LinkedBulbManager.WIRELESS_BULB_KEY, PersistentDataType.BYTE, (byte) 1);
        pdc.set(LinkedBulbManager.GROUP_ID_KEY, PersistentDataType.STRING, groupId.toString());
        pdc.set(LinkedBulbManager.BULB_INDEX_KEY, PersistentDataType.INTEGER, index);
        pdc.set(LinkedBulbManager.BULB_TYPE_KEY, PersistentDataType.STRING, variant.getBulbType().name());
        pdc.set(LinkedBulbManager.GROUP_SIZE_KEY, PersistentDataType.INTEGER, groupSize);
        if (ownerUuid != null) {
            pdc.set(LinkedBulbManager.OWNER_KEY, PersistentDataType.STRING, ownerUuid.toString());
        }

        bulb.setItemMeta(meta);
        return bulb;
    }

    public static void updateLinkedBulbLore(ItemStack item, List<Location> linkedLocations, boolean isConnected, int placedCount, int groupSize) {
        if (item == null || !item.hasItemMeta()) return;
        
        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        
        if (!pdc.has(LinkedBulbManager.WIRELESS_BULB_KEY, PersistentDataType.BYTE)) return;
        
        String groupIdStr = pdc.get(LinkedBulbManager.GROUP_ID_KEY, PersistentDataType.STRING);
        String bulbTypeStr = pdc.get(LinkedBulbManager.BULB_TYPE_KEY, PersistentDataType.STRING);
        Integer bulbIndex = pdc.get(LinkedBulbManager.BULB_INDEX_KEY, PersistentDataType.INTEGER);
        
        if (groupIdStr == null || bulbIndex == null) return;
        
        BulbVariant.BulbType bulbType = BulbVariant.BulbType.COPPER_BULB;
        if (bulbTypeStr != null) {
            try {
                bulbType = BulbVariant.BulbType.valueOf(bulbTypeStr);
            } catch (IllegalArgumentException ignored) {}
        }
        
        String variantName = bulbType == BulbVariant.BulbType.REDSTONE_LAMP ? "Redstone Lamp" : "Copper Bulb";
        
        String bulbLabel = BulbGroup.getIndexLabel(bulbIndex);
        String displayName = isConnected ? "⚡ Linked Bulb " + bulbLabel + " ⚡" : "Wireless Bulb " + bulbLabel;
        NamedTextColor nameColor = isConnected ? NamedTextColor.GREEN : NamedTextColor.AQUA;
        
        meta.displayName(Component.text(displayName, nameColor)
                .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Linked Wireless " + variantName, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Group ID: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(groupIdStr.substring(0, 8), NamedTextColor.DARK_AQUA))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Placed: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(placedCount + "/" + groupSize, NamedTextColor.DARK_AQUA))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        
        if (isConnected && linkedLocations != null && !linkedLocations.isEmpty()) {
            lore.add(Component.text("⚡ Connected to " + linkedLocations.size() + " bulb(s):", NamedTextColor.GREEN)
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
            lore.add(Component.text("⚡ No other bulbs placed yet", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("This bulb syncs with its group!", NamedTextColor.YELLOW)
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
