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

        @Schema(description = "Định danh máy — bền qua các lần chạy app. Bắt buộc khi bật giới hạn thiết bị (F-57)", example = "3f9a1c7e02b4d8e6a1f0c2b3d4e5f601")
        @JsonAlias({"deviceName", "device_name", "thietBi", "thiet_bi"})
        String device,

        @Schema(description = "Địa chỉ IP máy client (Địa chỉ IP)", example = "192.168.1.100")
        @JsonAlias({"ip", "ip_address", "diaChiIp", "dia_chi_ip"})
        String ipAddress,
        @Schema(description = "Tên máy để quản trị nhận ra trong danh sách thiết bị (không bắt buộc)", example = "XUONG-01")
        @JsonAlias({"machineName", "machine_name", "tenMay", "ten_may"})
        String deviceLabel,
        @Schema(description = "Hệ điều hành (không bắt buộc)", example = "Windows")
        String platform
) {
    public InternalLoginRequest(String username, String password) {
        this(username, password, null, null, null, null);
    }

    public InternalLoginRequest(String username, String password, String device, String ipAddress) {
        this(username, password, device, ipAddress, null, null);
    }
}
