package com.example.svgmanager.dto.internal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response change password for cutting machine client")
public record InternalChangePasswordResponse(
        @Schema(description = "Trạng thái thành công", example = "true")
        boolean success,

        @Schema(description = "Thông báo kết quả", example = "Password changed successfully")
        String message
) {}
