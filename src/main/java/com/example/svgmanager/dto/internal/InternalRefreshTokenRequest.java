package com.example.svgmanager.dto.internal;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Yêu cầu làm mới access token cho ứng dụng máy cắt")
public record InternalRefreshTokenRequest(
        @NotBlank(message = "Refresh token cannot be blank")
        @Schema(description = "Refresh token đã nhận khi đăng nhập hoặc refresh trước đó", requiredMode = Schema.RequiredMode.REQUIRED, example = "eyJhbGciOiJIUzUxMiIsInR5c...")
        String refreshToken,

        @Schema(description = "Thông tin thiết bị (Thiết bị)", example = "Cutter-Roland-GS24")
        @JsonAlias({"deviceName", "device_name", "thietBi", "thiet_bi"})
        String device,

        @Schema(description = "Địa chỉ IP máy client (Địa chỉ IP)", example = "192.168.1.100")
        @JsonAlias({"ip", "ip_address", "diaChiIp", "dia_chi_ip"})
        String ipAddress
) {
    public InternalRefreshTokenRequest(String refreshToken) {
        this(refreshToken, null, null);
    }
}
