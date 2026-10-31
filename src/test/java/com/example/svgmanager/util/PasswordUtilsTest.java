package com.example.svgmanager.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordUtils Tests")
class PasswordUtilsTest {

    @Test
    @DisplayName("trimPassword should return null for null input")
    void testTrimPasswordNull() {
        assertNull(PasswordUtils.trimPassword(null));
    }

    @Test
    @DisplayName("trimPassword should trim leading and trailing spaces")
    void testTrimPasswordLeadingTrailing() {
        assertEquals("Abc@123", PasswordUtils.trimPassword("  Abc@123  "));
        assertEquals("Abc@123", PasswordUtils.trimPassword("Abc@123  "));
        assertEquals("Abc@123", PasswordUtils.trimPassword("  Abc@123"));
    }

    @Test
    @DisplayName("trimPassword should preserve internal spaces")
    void testTrimPasswordInternalSpaces() {
        assertEquals("Abc 123", PasswordUtils.trimPassword("  Abc 123  "));
        assertEquals("A B C 1 2 3 ! @ #", PasswordUtils.trimPassword("  A B C 1 2 3 ! @ #  "));
    }

    @Test
    @DisplayName("trimPassword should preserve case and special characters")
    void testTrimPasswordCaseAndSpecialChars() {
        assertEquals("Password123!@#$%", PasswordUtils.trimPassword(" \t Password123!@#$% \n "));
    }

    @Test
    @DisplayName("trimPassword should return empty string for whitespace-only input")
    void testTrimPasswordWhitespaceOnly() {
        assertEquals("", PasswordUtils.trimPassword("   "));
        assertEquals("", PasswordUtils.trimPassword(" \t \n "));
    }

    @Test
    @DisplayName("trimOptionalPassword should return null for null, empty or whitespace-only input")
    void testTrimOptionalPasswordBlank() {
        assertNull(PasswordUtils.trimOptionalPassword(null));
        assertNull(PasswordUtils.trimOptionalPassword(""));
        assertNull(PasswordUtils.trimOptionalPassword("   "));
        assertNull(PasswordUtils.trimOptionalPassword(" \t \r \n "));
    }

    @Test
    @DisplayName("trimOptionalPassword should return trimmed string when non-blank")
    void testTrimOptionalPasswordNonBlank() {
        assertEquals("NewPassword123!", PasswordUtils.trimOptionalPassword("  NewPassword123!  "));
        assertEquals("New Pass 123!", PasswordUtils.trimOptionalPassword("  New Pass 123!  "));
    }
}
