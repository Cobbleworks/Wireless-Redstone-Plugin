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
        ItemStack[] chests = new ItemStack[count];
        for (int i = 0; i < count; i++) {
            String label = ChestGroup.getIndexLabel(i);
            chests[i] = createChest(groupId, i, "Wireless Chest " + label, ownerUuid, count);
        }
        return chests;
    }

    private static ItemStack createChest(UUID groupId, int index, String name, UUID ownerUuid, int groupSize) {
        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta meta = chest.getItemMeta();

        meta.displayName(Component.text(name, NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(List.of(
                Component.text("Linked Wireless Chest", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Group ID: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(groupId.toString().substring(0, 8), NamedTextColor.GOLD))
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Group Size: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(String.valueOf(groupSize), NamedTextColor.GOLD))
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("⚡ Not yet placed", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("This chest syncs contents with its group!", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        var pdc = meta.getPersistentDataContainer();
        pdc.set(LinkedChestManager.WIRELESS_CHEST_KEY, PersistentDataType.BYTE, (byte) 1);
        pdc.set(LinkedChestManager.CHEST_GROUP_ID_KEY, PersistentDataType.STRING, groupId.toString());
        pdc.set(LinkedChestManager.CHEST_INDEX_KEY, PersistentDataType.INTEGER, index);
        pdc.set(LinkedChestManager.CHEST_GROUP_SIZE_KEY, PersistentDataType.INTEGER, groupSize);
        if (ownerUuid != null) {
            pdc.set(LinkedChestManager.CHEST_OWNER_KEY, PersistentDataType.STRING, ownerUuid.toString());
        }

        chest.setItemMeta(meta);
        return chest;
    }

    public static void updateLinkedChestLore(ItemStack item, List<Location> linkedLocations, boolean isConnected, int placedCount, int groupSize) {
        if (item == null || !item.hasItemMeta()) return;
        
        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        
        if (!pdc.has(LinkedChestManager.WIRELESS_CHEST_KEY, PersistentDataType.BYTE)) return;
        
        String groupIdStr = pdc.get(LinkedChestManager.CHEST_GROUP_ID_KEY, PersistentDataType.STRING);
        Integer chestIndex = pdc.get(LinkedChestManager.CHEST_INDEX_KEY, PersistentDataType.INTEGER);
        
        if (groupIdStr == null || chestIndex == null) return;
        
        String chestLabel = ChestGroup.getIndexLabel(chestIndex);
        String displayName = isConnected ? "⚡ Linked Chest " + chestLabel + " ⚡" : "Wireless Chest " + chestLabel;
        NamedTextColor nameColor = isConnected ? NamedTextColor.GREEN : NamedTextColor.GOLD;
        
        meta.displayName(Component.text(displayName, nameColor)
                .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Linked Wireless Chest", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Group ID: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(groupIdStr.substring(0, 8), NamedTextColor.GOLD))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Placed: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(placedCount + "/" + groupSize, NamedTextColor.GOLD))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        
        if (isConnected && linkedLocations != null && !linkedLocations.isEmpty()) {
            lore.add(Component.text("⚡ Connected to " + linkedLocations.size() + " chest(s):", NamedTextColor.GREEN)
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
            lore.add(Component.text("⚡ No other chests placed yet", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("This chest syncs contents with its group!", NamedTextColor.YELLOW)
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
