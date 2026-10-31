package com.example.svgmanager.util;

import com.example.svgmanager.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsernameUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {"admin123", "Admin123", "admin_123", "admin-123", "AGENT_001"})
    @DisplayName("Valid usernames matching ^[A-Za-z0-9_-]+$ are accepted")
    void validUsernames_AreAccepted(String input) {
        String result = UsernameUtils.validateUsername(input);
        assertThat(result).isEqualTo(input);
    }

    @Test
    @DisplayName("Username with leading/trailing spaces is trimmed and accepted")
    void leadingAndTrailingSpaces_AreTrimmed() {
        String result = UsernameUtils.validateUsername("  user123  ");
        assertThat(result).isEqualTo("user123");
    }

    @Test
    @DisplayName("Username containing internal whitespace is rejected with USERNAME_CONTAINS_WHITESPACE")
    void internalWhitespace_IsRejected() {
        assertThatThrownBy(() -> UsernameUtils.validateUsername("user 123"))
                .isInstanceOf(BadRequestException.class)
                .satisfies(e -> {
                    BadRequestException ex = (BadRequestException) e;
                    assertThat(ex.getCode()).isEqualTo("USERNAME_CONTAINS_WHITESPACE");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "user@123",
            "user#123",
            "user.name",
            "người_dùng",
            "user!123",
            "user$123",
            "user%123",
            "___",
            "---"
    })
    @DisplayName("Invalid characters, Vietnamese accents, or purely symbol usernames are rejected with USERNAME_INVALID_FORMAT")
    void invalidCharacters_AreRejected(String input) {
        assertThatThrownBy(() -> UsernameUtils.validateUsername(input))
                .isInstanceOf(BadRequestException.class)
                .satisfies(e -> {
                    BadRequestException ex = (BadRequestException) e;
                    assertThat(ex.getCode()).isEqualTo("USERNAME_INVALID_FORMAT");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    @DisplayName("Empty or blank username is rejected with USERNAME_REQUIRED")
    void emptyOrBlankUsername_IsRejected(String input) {
        assertThatThrownBy(() -> UsernameUtils.validateUsername(input))
                .isInstanceOf(BadRequestException.class)
                .satisfies(e -> {
                    BadRequestException ex = (BadRequestException) e;
                    assertThat(ex.getCode()).isEqualTo("USERNAME_REQUIRED");
                });
    }

    @Test
    @DisplayName("Null username is rejected with USERNAME_REQUIRED")
    void nullUsername_IsRejected() {
        assertThatThrownBy(() -> UsernameUtils.validateUsername(null))
                .isInstanceOf(BadRequestException.class)
                .satisfies(e -> {
                    BadRequestException ex = (BadRequestException) e;
                    assertThat(ex.getCode()).isEqualTo("USERNAME_REQUIRED");
                });
    }

    @Test
    @DisplayName("Username shorter than 3 characters is rejected with USERNAME_LENGTH_INVALID")
    void tooShortUsername_IsRejected() {
        assertThatThrownBy(() -> UsernameUtils.validateUsername("ab"))
                .isInstanceOf(BadRequestException.class)
                .satisfies(e -> {
                    BadRequestException ex = (BadRequestException) e;
                    assertThat(ex.getCode()).isEqualTo("USERNAME_LENGTH_INVALID");
                });
    }

    @Test
    @DisplayName("Username longer than 50 characters is rejected with USERNAME_LENGTH_INVALID")
    void tooLongUsername_IsRejected() {
        String longUsername = "a".repeat(51);
        assertThatThrownBy(() -> UsernameUtils.validateUsername(longUsername))
                .isInstanceOf(BadRequestException.class)
                .satisfies(e -> {
                    BadRequestException ex = (BadRequestException) e;
                    assertThat(ex.getCode()).isEqualTo("USERNAME_LENGTH_INVALID");
                });
    }

    @Test
    @DisplayName("validateOptionalUsername returns null for null or blank input, but validates non-blank input")
    void validateOptionalUsername_WorksCorrectly() {
        assertThat(UsernameUtils.validateOptionalUsername(null)).isNull();
        assertThat(UsernameUtils.validateOptionalUsername("")).isNull();
        assertThat(UsernameUtils.validateOptionalUsername("   ")).isNull();
        assertThat(UsernameUtils.validateOptionalUsername("  valid_user  ")).isEqualTo("valid_user");

        assertThatThrownBy(() -> UsernameUtils.validateOptionalUsername("invalid@user"))
                .isInstanceOf(BadRequestException.class);
    }
}
