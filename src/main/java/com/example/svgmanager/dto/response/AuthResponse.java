package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response with JWT tokens and user profile")
public class AuthResponse {

    @Schema(example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", description = "JWT Access Token")
    private String accessToken;

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000", description = "Refresh Token")
    private String refreshToken;

    @Schema(example = "Bearer", description = "Token type")
    private String tokenType = "Bearer";

    @Schema(example = "3600", description = "Access token validity period in seconds")
    private Long expiresIn;

    @Schema(description = "Authenticated user summary")
    private UserSummaryResponse user;

    public AuthResponse() {
    }

    public AuthResponse(String accessToken, String refreshToken, String tokenType, Long expiresIn, UserSummaryResponse user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType != null ? tokenType : "Bearer";
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private Long expiresIn;
        private UserSummaryResponse user;

        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public Builder expiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        public Builder user(UserSummaryResponse user) {
            this.user = user;
            return this;
        }

        public AuthResponse build() {
            return new AuthResponse(accessToken, refreshToken, tokenType, expiresIn, user);
        }
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserSummaryResponse getUser() {
        return user;
    }

    public void setUser(UserSummaryResponse user) {
        this.user = user;
    }
}
