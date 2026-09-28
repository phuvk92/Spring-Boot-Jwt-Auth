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

    @Schema(description = "Full name of the user", example = "John Doe")
    private String fullName;

    @Schema(description = "Contact phone number", example = "+84901234567")
    private String phone;

    @Schema(description = "Assigned user role", example = "USER")
    private Role role;

    @Schema(description = "ID of the managing Agent if role is USER", example = "2")
    private Long agentId;

    @Schema(description = "Username of the managing Agent if role is USER", example = "agent_smith")
    private String agentUsername;

    @Schema(description = "ID của đại lý / chi nhánh trực thuộc", example = "1")
    private Long dealerId;

    @Schema(description = "Tên đại lý / chi nhánh", example = "Decal Ô Tô Sài Gòn")
    private String dealerName;

    @Schema(description = "Mã đại lý", example = "DL-0104")
    private String dealerCode;

    @Schema(description = "Account enabled status", example = "true")
    private boolean enabled;

    @Schema(description = "Account creation timestamp", example = "2026-03-30T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Account last update timestamp", example = "2026-03-30T10:00:00")
    private LocalDateTime updatedAt;

    public UserResponse() {
    }

    public UserResponse(Long id, String keycloakUserId, String username, String email, String fullName, String phone,
                        Role role, Long agentId, String agentUsername, Long dealerId, String dealerName, String dealerCode,
                        boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.keycloakUserId = keycloakUserId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
        this.agentId = agentId;
        this.agentUsername = agentUsername;
        this.dealerId = dealerId;
        this.dealerName = dealerName;
        this.dealerCode = dealerCode;
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
        private String fullName;
        private String phone;
        private Role role;
        private Long agentId;
        private String agentUsername;
        private Long dealerId;
        private String dealerName;
        private String dealerCode;
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

        public Builder agentUsername(String agentUsername) {
            this.agentUsername = agentUsername;
            return this;
        }

        public Builder dealerId(Long dealerId) {
            this.dealerId = dealerId;
            return this;
        }

        public Builder dealerName(String dealerName) {
            this.dealerName = dealerName;
            return this;
        }

        public Builder dealerCode(String dealerCode) {
            this.dealerCode = dealerCode;
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
            return new UserResponse(id, keycloakUserId, username, email, fullName, phone, role, agentId, agentUsername,
                    dealerId, dealerName, dealerCode, enabled, createdAt, updatedAt);
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

    public String getAgentUsername() {
        return agentUsername;
    }

    public void setAgentUsername(String agentUsername) {
        this.agentUsername = agentUsername;
    }

    public Long getDealerId() {
        return dealerId;
    }

    public void setDealerId(Long dealerId) {
        this.dealerId = dealerId;
    }

    public String getDealerName() {
        return dealerName;
    }

    public void setDealerName(String dealerName) {
        this.dealerName = dealerName;
    }

    public String getDealerCode() {
        return dealerCode;
    }

    public void setDealerCode(String dealerCode) {
        this.dealerCode = dealerCode;
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

    @Schema(description = "Số máy tối đa riêng của tài khoản (F-57) — null = dùng mặc định hệ thống")
    private Integer maxDevices;

    @Schema(description = "Số máy tối đa đang áp dụng (đã tính mặc định)", example = "1")
    private int effectiveMaxDevices;

    public Integer getMaxDevices() {
        return maxDevices;
    }

    public void setMaxDevices(Integer maxDevices) {
        this.maxDevices = maxDevices;
    }

    public int getEffectiveMaxDevices() {
        return effectiveMaxDevices;
    }

    public void setEffectiveMaxDevices(int effectiveMaxDevices) {
        this.effectiveMaxDevices = effectiveMaxDevices;
    }
}
