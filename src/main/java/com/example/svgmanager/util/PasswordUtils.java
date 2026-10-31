package com.example.svgmanager.util;

/**
 * Utility class for sanitizing and validating password inputs across the application.
 * Enforces business rules:
 * - Trims leading and trailing whitespace.
 * - Retains internal whitespace, casing, and special characters.
 * - Never logs passwords.
 */
public final class PasswordUtils {

    private PasswordUtils() {
        // Utility class
    }

    /**
     * Trims leading and trailing whitespace from a password.
     * Retains all internal whitespace.
     *
     * @param password input password string
     * @return trimmed password, or null if input is null
     */
    public static String trimPassword(String password) {
        if (password == null) {
            return null;
        }
        return password.trim();
    }

    /**
     * Trims optional password for update scenarios:
     * - Returns null if input is null, empty, or whitespace-only (indicating password is unchanged).
     * - Returns trimmed password if non-blank.
     *
     * @param password input password string
     * @return trimmed password or null if unchanged/blank
     */
    public static String trimOptionalPassword(String password) {
        if (password == null) {
            return null;
        }
        String trimmed = password.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
