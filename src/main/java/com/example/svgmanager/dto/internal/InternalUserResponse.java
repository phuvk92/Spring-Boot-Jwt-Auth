package com.example.svgmanager.dto.internal;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

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
        String role,

        @Schema(description = "Ngày hết hạn tài khoản (YYYY-MM-DD)", example = "2026-12-31")
        LocalDate expirationDate,

        @Schema(description = "Trạng thái tài khoản đã hết hạn hay chưa", example = "false")
        Boolean isExpired,

        @Schema(description = "Trạng thái tài khoản có đang hoạt động hay không", example = "true")
        Boolean enabled,

        @Schema(description = "Số ngày được cấp phép", example = "365")
        Integer allocatedDays
) {
    public InternalUserResponse(Long id, String username, String displayName, Long dealerId, String dealerName, String role) {
        this(id, username, displayName, dealerId, dealerName, role, null, false, true, 0);
    }
}
