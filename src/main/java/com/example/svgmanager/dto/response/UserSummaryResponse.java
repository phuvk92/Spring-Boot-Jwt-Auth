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

    public UserSummaryResponse() {
    }

    public UserSummaryResponse(Long id, String username, String email, Role role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String username;
        private String email;
        private Role role;

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

        public UserSummaryResponse build() {
            return new UserSummaryResponse(id, username, email, role);
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
}
