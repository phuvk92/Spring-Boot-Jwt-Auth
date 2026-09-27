package com.example.svgmanager.dto.internal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User profile summary for cutting machine client")
public record InternalUserResponse(
        @Schema(description = "ID người dùng", example = "1001")
        Long id,

        @Schema(description = "Tên đăng nhập", example = "dealer_user01")
        String username,

        @Schema(description = "Tên hiển thị", example = "Nguyen Van A")
        String displayName,

        @Schema(description = "ID đại lý trực thuộc (nếu có)", example = "20")
        Long dealerId,

        @Schema(description = "Tên đại lý trực thuộc (nếu có)", example = "ABC Auto")
        String dealerName,

        @Schema(description = "Vai trò người dùng", example = "USER")
        String role
) {}
