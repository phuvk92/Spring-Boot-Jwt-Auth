package com.example.svgmanager.util;

import com.example.svgmanager.exception.BadRequestException;

import java.util.regex.Pattern;

/**
 * Utility for username standardization and validation across cutting-admin.
 * Enforces:
 * - Whitelist characters: A-Z, a-z, 0-9, _, - (Pattern: ^[A-Za-z0-9_-]+$)
 * - At least one alphanumeric character (cannot be only symbols like ___ or ---)
 * - Length: 3 to 50 characters
 * - Trim leading and trailing whitespace
 * - Reject any whitespace inside username
 * - Specific error codes:
 *   - USERNAME_REQUIRED
 *   - USERNAME_CONTAINS_WHITESPACE
 *   - USERNAME_LENGTH_INVALID
 *   - USERNAME_INVALID_FORMAT
 */
public final class UsernameUtils {

    public static final String USERNAME_REGEX = "^[A-Za-z0-9_-]+$";
    public static final Pattern USERNAME_PATTERN = Pattern.compile(USERNAME_REGEX);
    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 50;

    private UsernameUtils() {
    }

    /**
     * Trim leading and trailing whitespace.
     */
    public static String trimUsername(String username) {
        return username != null ? username.trim() : null;
    }

    /**
     * Validates and normalizes username according to system rules.
     *
     * @param rawUsername the raw input username
     * @return the trimmed, valid username
     * @throws BadRequestException if validation fails with specific error code
     */
    public static String validateUsername(String rawUsername) {
        return validateUsername(rawUsername, "username");
    }

    /**
     * Validates and normalizes username according to system rules for a specific form field.
     *
     * @param rawUsername the raw input username
     * @param fieldName the form field name (e.g. "username" or "agentUsername")
     * @return the trimmed, valid username
     * @throws BadRequestException if validation fails with specific error code and field
     */
    public static String validateUsername(String rawUsername, String fieldName) {
        String effectiveField = (fieldName != null && !fieldName.isBlank()) ? fieldName : "username";

        if (rawUsername == null) {
            throw new BadRequestException("Tên đăng nhập không được để trống", "USERNAME_REQUIRED", effectiveField);
        }

        String trimmed = rawUsername.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("Tên đăng nhập không được để trống", "USERNAME_REQUIRED", effectiveField);
        }

        // Check if username contains any internal whitespace
        if (trimmed.matches(".*\\s+.*")) {
            throw new BadRequestException("Tên đăng nhập không được chứa khoảng trắng", "USERNAME_CONTAINS_WHITESPACE", effectiveField);
        }

        // Check length: 3 to 50 characters
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            throw new BadRequestException("Tên đăng nhập phải từ " + MIN_LENGTH + " đến " + MAX_LENGTH + " ký tự", "USERNAME_LENGTH_INVALID", effectiveField);
        }

        // Check character whitelist: only A-Z, a-z, 0-9, _, -
        if (!USERNAME_PATTERN.matcher(trimmed).matches()) {
            throw new BadRequestException("Chỉ được sử dụng chữ cái tiếng Anh, số, dấu gạch dưới (_) và dấu gạch ngang (-)", "USERNAME_INVALID_FORMAT", effectiveField);
        }

        // Must contain at least one alphanumeric character (cannot be only ___ or ---)
        if (!trimmed.matches(".*[A-Za-z0-9].*")) {
            throw new BadRequestException("Chỉ được sử dụng chữ cái tiếng Anh, số, dấu gạch dưới (_) và dấu gạch ngang (-)", "USERNAME_INVALID_FORMAT", effectiveField);
        }

        return trimmed;
    }

    /**
     * Validates an optional username (e.g. during edit where username might be omitted or null).
     *
     * @param rawUsername the raw input username
     * @return the trimmed valid username, or null if empty/null
     */
    public static String validateOptionalUsername(String rawUsername) {
        return validateOptionalUsername(rawUsername, "username");
    }

    public static String validateOptionalUsername(String rawUsername, String fieldName) {
        if (rawUsername == null || rawUsername.trim().isEmpty()) {
            return null;
        }
        return validateUsername(rawUsername, fieldName);
    }
}
