package easv.util;

import java.util.Locale;

/**
 * Shared string helpers used across controllers, services, and data access.
 * Lives in a foundational package so any layer can depend on it.
 */
public final class Strings {

    private Strings() {
    }

    /** Trims the value, treating {@code null} as an empty string. */
    public static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    /** Trimmed, lower-cased form for case-insensitive comparison. */
    public static String normalize(String value) {
        return clean(value).toLowerCase(Locale.ROOT);
    }

    /** Returns the cleaned value, or {@code fallback} when it is blank. */
    public static String displayText(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? fallback : cleaned;
    }

    /** Up to two uppercase initials from a name, or {@code fallback} when blank. */
    public static String initials(String name, String fallback) {
        String cleaned = clean(name);
        if (cleaned.isBlank()) {
            return fallback;
        }

        String[] parts = cleaned.split("\\s+");

        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }

        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }
}
