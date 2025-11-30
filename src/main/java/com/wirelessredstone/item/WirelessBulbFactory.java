package com.wirelessredstone.item;

import com.wirelessredstone.manager.LinkedBulbManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

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
}
