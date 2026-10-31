package com.example.svgmanager.dto.request;

import com.example.svgmanager.util.PasswordUtils;
import com.fasterxml.jackson.annotation.JsonSetter;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request object for changing user password")
public class ChangePasswordRequest {

    @NotBlank(message = "Current password cannot be blank")
    @Schema(example = "Password123!", description = "Current active password")
    private String currentPassword;

    @NotBlank(message = "New password cannot be blank")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
            message = "Password must be at least 8 characters long, contain at least one uppercase letter, one lowercase letter, one number, and one special character"
    )
    @Schema(example = "NewPassword123!", description = "New secure password complying with complexity rules")
    private String newPassword;

    @Schema(example = "NewPassword123!", description = "Optional confirmation of new password")
    private String confirmPassword;

    public ChangePasswordRequest() {
    }

    public ChangePasswordRequest(String currentPassword, String newPassword, String confirmPassword) {
        this.currentPassword = PasswordUtils.trimPassword(currentPassword);
        this.newPassword = PasswordUtils.trimPassword(newPassword);
        this.confirmPassword = PasswordUtils.trimPassword(confirmPassword);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String currentPassword;
        private String newPassword;
        private String confirmPassword;

        public Builder currentPassword(String currentPassword) {
            this.currentPassword = PasswordUtils.trimPassword(currentPassword);
            return this;
        }

        public Builder newPassword(String newPassword) {
            this.newPassword = PasswordUtils.trimPassword(newPassword);
            return this;
        }

        public Builder confirmPassword(String confirmPassword) {
            this.confirmPassword = PasswordUtils.trimPassword(confirmPassword);
            return this;
        }

        public ChangePasswordRequest build() {
            return new ChangePasswordRequest(currentPassword, newPassword, confirmPassword);
        }
    }

    public String getCurrentPassword() {
        return PasswordUtils.trimPassword(currentPassword);
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = PasswordUtils.trimPassword(currentPassword);
    }

    public String getNewPassword() {
        return PasswordUtils.trimPassword(newPassword);
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = PasswordUtils.trimPassword(newPassword);
    }

    public String getConfirmPassword() {
        return PasswordUtils.trimPassword(confirmPassword);
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = PasswordUtils.trimPassword(confirmPassword);
    }
}
