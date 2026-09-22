package com.example.svgmanager.dto.response;

import com.example.svgmanager.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "User details response")
public class UserResponse {

    @Schema(description = "Unique user ID", example = "1")
    private Long id;

    @Schema(description = "Keycloak User ID", example = "4c529cf1-0c58-45e3-9366-07ceb2cb1ec5")
    private String keycloakUserId;

    @Schema(description = "Username", example = "john_doe")
    private String username;

    @Schema(description = "Email address", example = "john@example.com")
    private String email;

    @Schema(description = "Assigned user role", example = "USER")
    private Role role;

    @Schema(description = "ID of the managing Agent if role is USER", example = "2")
    private Long agentId;

    @Schema(description = "Username of the managing Agent if role is USER", example = "agent_smith")
    private String agentUsername;

    @Schema(description = "Account enabled status", example = "true")
    private boolean enabled;

    @Schema(description = "Account creation timestamp", example = "2026-03-30T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Account last update timestamp", example = "2026-03-30T10:00:00")
    private LocalDateTime updatedAt;

    public UserResponse() {
    }

    public UserResponse(Long id, String keycloakUserId, String username, String email, Role role, Long agentId, String agentUsername, boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.keycloakUserId = keycloakUserId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.agentId = agentId;
        this.agentUsername = agentUsername;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String keycloakUserId;
        private String username;
        private String email;
        private Role role;
        private Long agentId;
        private String agentUsername;
        private boolean enabled;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder keycloakUserId(String keycloakUserId) {
            this.keycloakUserId = keycloakUserId;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
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

        public Builder agentUsername(String agentUsername) {
            this.agentUsername = agentUsername;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public UserResponse build() {
            return new UserResponse(id, keycloakUserId, username, email, role, agentId, agentUsername, enabled, createdAt, updatedAt);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKeycloakUserId() {
        return keycloakUserId;
    }

    public void setKeycloakUserId(String keycloakUserId) {
        this.keycloakUserId = keycloakUserId;
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

    public String getAgentUsername() {
        return agentUsername;
    }

    public void setAgentUsername(String agentUsername) {
        this.agentUsername = agentUsername;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
