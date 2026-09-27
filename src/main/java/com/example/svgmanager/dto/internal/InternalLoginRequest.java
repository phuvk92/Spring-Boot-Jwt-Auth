package com.example.svgmanager.dto.internal;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request authentication payload for cutting machine client")
public record InternalLoginRequest(
        @NotBlank(message = "Tên đăng nhập hoặc Gmail là bắt buộc")
        @Schema(description = "Địa chỉ Gmail hoặc Tên đăng nhập", example = "dealer_user01@gmail.com")
        String username,

        @NotBlank(message = "Mật khẩu là bắt buộc")
        @Schema(description = "Mật khẩu", example = "Password123!")
        String password,

        @Schema(description = "Thông tin thiết bị (Thiết bị)", example = "Cutter-Roland-GS24")
        @JsonAlias({"deviceName", "device_name", "thietBi", "thiet_bi"})
        String device,

        @Schema(description = "Địa chỉ IP máy client (Địa chỉ IP)", example = "192.168.1.100")
        @JsonAlias({"ip", "ip_address", "diaChiIp", "dia_chi_ip"})
        String ipAddress
) {
    public InternalLoginRequest(String username, String password) {
        this(username, password, null, null);
    }
}
