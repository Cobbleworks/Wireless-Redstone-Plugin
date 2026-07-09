package com.wirelessredstone.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Factory for creating Circuit Analyser items.
 * Circuit Analysers are used to analyze wireless blocks and display detailed information.
 */
public class CircuitAnalyserFactory {

    public static final NamespacedKey CIRCUIT_ANALYSER_KEY = new NamespacedKey("wirelessredstone", "circuit_analyser");

    /**
     * Creates a new Circuit Analyser item.
     */
    public static ItemStack createCircuitAnalyser() {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Circuit Analyser", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        meta.lore(List.of(
                Component.text("A diagnostic tool for wireless circuits", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Right-click ", NamedTextColor.YELLOW)
                        .append(Component.text("a wireless block to analyze it", NamedTextColor.GRAY))
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Hold ", NamedTextColor.YELLOW)
                        .append(Component.text("to reveal all wireless blocks", NamedTextColor.GRAY))
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("with color-coded glowing outlines", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Triggered circuits draw connection lines", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        meta.getPersistentDataContainer().set(CIRCUIT_ANALYSER_KEY, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Checks if an item is a Circuit Analyser.
     */
    public static boolean isCircuitAnalyser(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(CIRCUIT_ANALYSER_KEY, PersistentDataType.BYTE);
    }
}
