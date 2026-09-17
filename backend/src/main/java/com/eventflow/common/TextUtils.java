package com.eventflow.common;

public final class TextUtils {

    private TextUtils() {
    }

    public static String normalizeName(String value) {
        if (value == null) {
            return null;
        }
        return value.strip();
    }

    public static String normalizeEmail(String value) {
        if (value == null) {
            return null;
        }
        return value.strip().toLowerCase(java.util.Locale.ROOT);
    }

    public static String normalizeDescription(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
