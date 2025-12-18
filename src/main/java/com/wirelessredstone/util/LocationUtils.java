package com.wirelessredstone.util;

import org.bukkit.Location;

/**
 * Utility class for common location operations.
 * Provides block-level comparison and normalization methods.
 */
public final class LocationUtils {

    private LocationUtils() {
        // Utility class, no instantiation
    }

    /**
     * Compares two locations by block coordinates only (ignores yaw, pitch, and decimal parts).
     * This ensures proper comparison when locations come from different sources.
     */
    public static boolean isSameBlock(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) return false;
        if (loc1.getWorld() == null || loc2.getWorld() == null) return false;
        return loc1.getWorld().equals(loc2.getWorld())
                && loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ();
    }

    /**
     * Normalizes a location to block coordinates only.
     * Removes any yaw, pitch, and decimal components.
     */
    public static Location normalize(Location location) {
        if (location == null || location.getWorld() == null) return null;
        return new Location(
                location.getWorld(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    /**
     * Formats a location as a human-readable string.
     * Format: "world (x, y, z)"
     */
    public static String format(Location location) {
        if (location == null || location.getWorld() == null) return "Unknown";
        return String.format("%s (%d, %d, %d)",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    /**
     * Formats a location as a compact coordinate string.
     * Format: "x, y, z"
     */
    public static String formatCoords(Location location) {
        if (location == null) return "Unknown";
        return String.format("%d, %d, %d",
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    /**
     * Gets the world name from a location safely.
     */
    public static String getWorldName(Location location) {
        if (location == null || location.getWorld() == null) return "Unknown";
        return location.getWorld().getName();
    }
}
