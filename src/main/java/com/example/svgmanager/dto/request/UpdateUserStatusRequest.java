package com.example.svgmanager.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request object for updating user enabled status")
public class UpdateUserStatusRequest {

    @NotNull(message = "Enabled status must not be null")
    @Schema(example = "false", description = "New account status (true for active, false for disabled/locked)")
    private Boolean enabled;

    public UpdateUserStatusRequest() {
    }

    public UpdateUserStatusRequest(Boolean enabled) {
        this.enabled = enabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Boolean enabled;

        public Builder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public UpdateUserStatusRequest build() {
            return new UpdateUserStatusRequest(enabled);
        }
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
