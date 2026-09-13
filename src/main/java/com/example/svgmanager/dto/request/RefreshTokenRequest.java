package com.example.svgmanager.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request object for refreshing access token")
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token cannot be blank")
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000", description = "Valid refresh token")
    private String refreshToken;

    public RefreshTokenRequest() {
    }

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String refreshToken;

        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public RefreshTokenRequest build() {
            return new RefreshTokenRequest(refreshToken);
        }
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
