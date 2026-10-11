package com.example.svgmanager.dto.internal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response for cutting machine client")
public record InternalLoginResponse(
        @Schema(description = "JWT Access Token", example = "eyJhbGciOiJSUzI1NiIsInR5c...")
        String accessToken,

        @Schema(description = "Refresh Token dùng để làm mới access token", example = "eyJhbGciOiJIUzUxMiIsInR5c...")
        String refreshToken,

        @Schema(description = "Loại Token", example = "Bearer")
        String tokenType,

        @Schema(description = "Thời gian hết hạn tính bằng giây", example = "3600")
        Long expiresIn,

        @Schema(description = "Hạn tuyệt đối của refresh token tính bằng giây (refresh_expires_in của Keycloak); 0 = offline token không hạn tuyệt đối (F-01)", example = "0")
        Long refreshExpiresIn,

        @Schema(description = "Thông tin tài khoản đăng nhập")
        InternalUserResponse user
) {
    public InternalLoginResponse(String accessToken, String tokenType, Long expiresIn, InternalUserResponse user) {
        this(accessToken, null, tokenType, expiresIn, null, user);
    }

    public InternalLoginResponse(String accessToken, String refreshToken, String tokenType, Long expiresIn,
                                 InternalUserResponse user) {
        this(accessToken, refreshToken, tokenType, expiresIn, null, user);
    }
}
