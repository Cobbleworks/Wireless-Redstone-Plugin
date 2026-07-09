package com.wirelessredstone.util;

import java.util.Locale;

/**
 * Parses optional category prefixes from group display names.
 */
public final class GroupNameParser {

    private GroupNameParser() {
    }

    public record ParsedName(String categoryName, String groupName) {
        public boolean hasCategory() {
            return categoryName != null;
        }

        public String categoryKey() {
            return categoryName == null ? null : normalizeCategoryKey(categoryName);
        }
    }

    public static ParsedName parse(String displayName) {
        String fallbackName = displayName == null ? "" : displayName.trim();
        int slashIndex = fallbackName.indexOf('/');
        if (slashIndex <= 0 || slashIndex >= fallbackName.length() - 1) {
            return new ParsedName(null, fallbackName);
        }

        String categoryName = fallbackName.substring(0, slashIndex).trim();
        String groupName = fallbackName.substring(slashIndex + 1).trim();
        if (categoryName.isEmpty() || groupName.isEmpty()) {
            return new ParsedName(null, fallbackName);
        }

        return new ParsedName(categoryName, groupName);
    }

    public static String normalizeCategoryKey(String categoryName) {
        return categoryName.trim().toLowerCase(Locale.ROOT);
    }
}
