package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Bốn thẻ thống kê thiết bị trên đầu trang Quản lý thiết bị — GET /api/devices/stats (F-57).
 */
@Schema(description = "Thống kê thiết bị toàn hệ thống")
public record DeviceStatsResponse(
        @Schema(description = "Máy ACTIVE có lastSeenAt trong 15 phút gần nhất", example = "286")
        long activeNow,

        @Schema(description = "Tổng số máy ACTIVE", example = "648")
        long registered,

        @Schema(description = "Số tài khoản có số máy ACTIVE >= số máy tối đa", example = "3")
        long usersAtLimit,

        @Schema(description = "Máy ACTIVE không thấy quá 30 ngày (gợi ý gỡ để nhả chỗ)", example = "11")
        long staleDevices
) {
}
