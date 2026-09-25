package com.example.svgmanager.dto.request;

import com.example.svgmanager.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request object for updating an existing user")
public class UpdateUserRequest {

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    @Schema(example = "updated_user@example.com", description = "User email address")
    private String email;

    @Schema(example = "Nguyen Van A", description = "Full name of the user")
    @Size(max = 255, message = "Full name cannot exceed 255 characters")
    private String fullName;

    @Schema(example = "+84901234567", description = "Contact phone number")
    @Size(max = 50, message = "Phone number cannot exceed 50 characters")
    private String phone;

    @Schema(example = "USER", description = "User role (optional, ADMIN only for promotion)")
    private Role role;

    @Schema(example = "2", description = "Managing Agent ID when role is USER (optional, for Admin)")
    private Long agentId;

    @Schema(example = "true", description = "Account active status")
    private Boolean enabled = true;

    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
            message = "Password must be at least 8 characters long, contain at least one uppercase letter, one lowercase letter, one number, and one special character"
    )
    @Schema(example = "NewPassword123!", description = "Optional new password. Leave null or empty to keep current password.")
    private String password;

    public UpdateUserRequest() {
    }

    public UpdateUserRequest(String email, String fullName, String phone, Role role, Long agentId, Boolean enabled, String password) {
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
        this.agentId = agentId;
        this.enabled = enabled != null ? enabled : true;
        this.password = password;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String email;
        private String fullName;
        private String phone;
        private Role role;
        private Long agentId;
        private Boolean enabled = true;
        private String password;

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder agentId(Long agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public UpdateUserRequest build() {
            return new UpdateUserRequest(email, fullName, phone, role, agentId, enabled, password);
        }
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
