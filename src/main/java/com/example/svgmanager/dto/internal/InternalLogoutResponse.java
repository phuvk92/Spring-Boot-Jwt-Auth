package com.example.svgmanager.dto.internal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Kết quả đăng xuất cho ứng dụng máy cắt")
public record InternalLogoutResponse(
        @Schema(description = "Trạng thái thành công", example = "true")
        boolean success,

        @Schema(description = "Thông báo kết quả", example = "Logged out successfully")
        String message
) {}
