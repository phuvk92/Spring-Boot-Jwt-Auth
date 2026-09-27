package com.example.svgmanager.dto.response;

import com.example.svgmanager.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Summary information of a user")
public class UserSummaryResponse {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "user01")
    private String username;

    @Schema(example = "user01@example.com")
    private String email;

    @Schema(example = "USER")
    private Role role;

    @Schema(example = "1", description = "ID của đại lý trực thuộc")
    private Long dealerId;

    @Schema(example = "Decal Ô Tô Sài Gòn", description = "Tên đại lý")
    private String dealerName;

    @Schema(example = "DL-0104", description = "Mã đại lý")
    private String dealerCode;

    public UserSummaryResponse() {
    }

    public UserSummaryResponse(Long id, String username, String email, Role role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public UserSummaryResponse(Long id, String username, String email, Role role, Long dealerId, String dealerName, String dealerCode) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.dealerId = dealerId;
        this.dealerName = dealerName;
        this.dealerCode = dealerCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String username;
        private String email;
        private Role role;
        private Long dealerId;
        private String dealerName;
        private String dealerCode;

        public Builder id(Long id) {
            this.id = id;
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

        public UserSummaryResponse build() {
            return new UserSummaryResponse(id, username, email, role, dealerId, dealerName, dealerCode);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
