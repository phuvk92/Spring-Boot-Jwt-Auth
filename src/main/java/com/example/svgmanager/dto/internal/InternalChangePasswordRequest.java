package com.example.svgmanager.dto.internal;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request change password for cutting machine client")
public record InternalChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        @Schema(description = "Mật khẩu hiện tại", example = "OldPass123!")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Schema(description = "Mật khẩu mới", example = "NewPass456!")
        String newPassword,

        @NotBlank(message = "Confirm password is required")
        @Schema(description = "Xác nhận mật khẩu mới", example = "NewPass456!")
        String confirmPassword,

        @Schema(description = "Thông tin thiết bị (Thiết bị)", example = "Cutter-Roland-GS24")
        @JsonAlias({"deviceName", "device_name", "thietBi", "thiet_bi"})
        String device,

        @Schema(description = "Địa chỉ IP máy client (Địa chỉ IP)", example = "192.168.1.100")
        @JsonAlias({"ip", "ip_address", "diaChiIp", "dia_chi_ip"})
        String ipAddress
) {
    public InternalChangePasswordRequest(String currentPassword, String newPassword, String confirmPassword) {
        this(currentPassword, newPassword, confirmPassword, null, null);
    }
}
