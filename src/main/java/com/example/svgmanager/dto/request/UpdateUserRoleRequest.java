package com.example.svgmanager.dto.request;

import com.example.svgmanager.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request object for updating user role")
public class UpdateUserRoleRequest {

    @NotNull(message = "Role must not be null")
    @Schema(example = "AGENT", description = "New role for the user (ADMIN, AGENT, USER)")
    private Role role;

    public UpdateUserRoleRequest() {
    }

    public UpdateUserRoleRequest(Role role) {
        this.role = role;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Role role;

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public UpdateUserRoleRequest build() {
            return new UpdateUserRoleRequest(role);
        }
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
