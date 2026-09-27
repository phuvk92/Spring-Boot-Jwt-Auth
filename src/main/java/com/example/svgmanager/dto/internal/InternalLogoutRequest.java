package com.example.svgmanager.dto.internal;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Yêu cầu đăng xuất cho ứng dụng máy cắt")
public record InternalLogoutRequest(
        @Schema(description = "Refresh Token cần thu hồi để hủy phiên làm việc trong Keycloak", example = "eyJhbGciOiJIUzUxMiIsInR5c...")
        String refreshToken,

        @Schema(description = "Thông tin thiết bị (Thiết bị)", example = "Cutter-Roland-GS24")
        @JsonAlias({"deviceName", "device_name", "thietBi", "thiet_bi"})
        String device,

        @Schema(description = "Địa chỉ IP máy client (Địa chỉ IP)", example = "192.168.1.100")
        @JsonAlias({"ip", "ip_address", "diaChiIp", "dia_chi_ip"})
        String ipAddress
) {
    public InternalLogoutRequest(String refreshToken) {
        this(refreshToken, null, null);
    }
}
