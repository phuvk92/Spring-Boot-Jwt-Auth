package com.example.svgmanager.dto.request;

import com.example.svgmanager.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request object for creating a new user by Admin or Agent")
public class CreateUserRequest {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    @Schema(example = "agent01", description = "Unique username")
    private String username;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    @Schema(example = "agent01@example.com", description = "Unique email address")
    private String email;

    @Schema(example = "Nguyen Van A", description = "Full name of the user")
    @Size(max = 255, message = "Full name cannot exceed 255 characters")
    private String fullName;

    @Schema(example = "+84901234567", description = "Contact phone number")
    @Size(max = 50, message = "Phone number cannot exceed 50 characters")
    private String phone;

    @NotBlank(message = "Password cannot be blank")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
            message = "Password must be at least 8 characters long, contain at least one uppercase letter, one lowercase letter, one number, and one special character"
    )
    @Schema(example = "Password123!", description = "Initial password")
    private String password;

    @NotNull(message = "Role is required")
    @Schema(example = "AGENT", description = "User role (ADMIN, AGENT, USER)")
    private Role role;

    @Schema(example = "2", description = "Managing Agent ID when role is USER (optional, for Admin)")
    private Long agentId;

    @Schema(example = "true", description = "Account status")
    private Boolean enabled = true;

    public CreateUserRequest() {
    }

    public CreateUserRequest(String username, String email, String fullName, String phone, String password, Role role, Long agentId, Boolean enabled) {
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.password = password;
        this.role = role;
        this.agentId = agentId;
        this.enabled = enabled != null ? enabled : true;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String username;
        private String email;
        private String fullName;
        private String phone;
        private String password;
        private Role role;
        private Long agentId;
        private Boolean enabled = true;

        public Builder username(String username) {
            this.username = username;
            return this;
        }

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

        public Builder password(String password) {
            this.password = password;
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

        public CreateUserRequest build() {
            return new CreateUserRequest(username, email, fullName, phone, password, role, agentId, enabled);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
}
