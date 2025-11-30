package com.wirelessredstone.item;

import com.wirelessredstone.manager.LinkedBulbManager;
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

    public static ItemStack[] createLinkedPair(UUID pairId, BulbVariant variant, UUID ownerUuid) {
        return new ItemStack[]{
                createBulb(pairId, 0, "Wireless Bulb A", variant, ownerUuid),
                createBulb(pairId, 1, "Wireless Bulb B", variant, ownerUuid)
        };
    }

    private static ItemStack createBulb(UUID pairId, int index, String name, BulbVariant variant, UUID ownerUuid) {
        ItemStack bulb = new ItemStack(variant.getMaterial());
        ItemMeta meta = bulb.getItemMeta();

        meta.displayName(Component.text(name, NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(List.of(
                Component.text("Linked " + variant.getDisplayName(), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Pair ID: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(pairId.toString().substring(0, 8), NamedTextColor.DARK_AQUA))
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("⚡ Not yet placed", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("This bulb syncs with its pair!", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        var pdc = meta.getPersistentDataContainer();
        pdc.set(LinkedBulbManager.WIRELESS_BULB_KEY, PersistentDataType.BYTE, (byte) 1);
        pdc.set(LinkedBulbManager.PAIR_ID_KEY, PersistentDataType.STRING, pairId.toString());
        pdc.set(LinkedBulbManager.BULB_INDEX_KEY, PersistentDataType.INTEGER, index);
        pdc.set(LinkedBulbManager.BULB_TYPE_KEY, PersistentDataType.STRING, variant.getBulbType().name());
        if (ownerUuid != null) {
            pdc.set(LinkedBulbManager.OWNER_KEY, PersistentDataType.STRING, ownerUuid.toString());
        }

        bulb.setItemMeta(meta);
        return bulb;
    }

    public static void updateLinkedBulbLore(ItemStack item, Location linkedLocation, boolean isConnected) {
        if (item == null || !item.hasItemMeta()) return;
        
        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        
        if (!pdc.has(LinkedBulbManager.WIRELESS_BULB_KEY, PersistentDataType.BYTE)) return;
        
        String pairIdStr = pdc.get(LinkedBulbManager.PAIR_ID_KEY, PersistentDataType.STRING);
        String bulbTypeStr = pdc.get(LinkedBulbManager.BULB_TYPE_KEY, PersistentDataType.STRING);
        Integer bulbIndex = pdc.get(LinkedBulbManager.BULB_INDEX_KEY, PersistentDataType.INTEGER);
        
        if (pairIdStr == null || bulbIndex == null) return;
        
        BulbVariant.BulbType bulbType = BulbVariant.BulbType.COPPER_BULB;
        if (bulbTypeStr != null) {
            try {
                bulbType = BulbVariant.BulbType.valueOf(bulbTypeStr);
            } catch (IllegalArgumentException ignored) {}
        }
        
        String variantName = bulbType == BulbVariant.BulbType.REDSTONE_LAMP ? "Redstone Lamp" : "Copper Bulb";
        
        // Update display name to show connection status
        String bulbLabel = bulbIndex == 0 ? "A" : "B";
        String displayName = isConnected ? "⚡ Linked Bulb " + bulbLabel + " ⚡" : "Wireless Bulb " + bulbLabel;
        NamedTextColor nameColor = isConnected ? NamedTextColor.GREEN : NamedTextColor.AQUA;
        
        meta.displayName(Component.text(displayName, nameColor)
                .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Linked Wireless " + variantName, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Pair ID: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(pairIdStr.substring(0, 8), NamedTextColor.DARK_AQUA))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        
        if (isConnected && linkedLocation != null) {
            String worldName = linkedLocation.getWorld() != null ? linkedLocation.getWorld().getName() : "Unknown";
            lore.add(Component.text("⚡ Connected to:", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("  World: ", NamedTextColor.GRAY)
                    .append(Component.text(worldName, NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("  X: ", NamedTextColor.GRAY)
                    .append(Component.text(String.valueOf(linkedLocation.getBlockX()), NamedTextColor.WHITE))
                    .append(Component.text(" Y: ", NamedTextColor.GRAY))
                    .append(Component.text(String.valueOf(linkedLocation.getBlockY()), NamedTextColor.WHITE))
                    .append(Component.text(" Z: ", NamedTextColor.GRAY))
                    .append(Component.text(String.valueOf(linkedLocation.getBlockZ()), NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("⚡ Partner not yet placed", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("This bulb syncs with its pair!", NamedTextColor.YELLOW)
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
