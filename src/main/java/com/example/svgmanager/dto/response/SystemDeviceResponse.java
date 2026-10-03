package com.example.svgmanager.dto.response;

import com.example.svgmanager.entity.UserDevice;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Một dòng danh sách thiết bị toàn hệ thống — GET /api/devices (F-57).
 */
@Schema(description = "Thông tin thiết bị trong danh sách toàn hệ thống")
public record SystemDeviceResponse(
        @Schema(description = "ID bản ghi đăng ký thiết bị (user_devices.id) — dùng để gỡ máy")
        Long deviceRegId,

        @Schema(description = "ID người dùng sở hữu thiết bị")
        Long userId,

        @Schema(description = "Tên đăng nhập của người dùng", example = "worker_01")
        String username,

        @Schema(description = "Họ và tên của người dùng", example = "Nguyễn Văn A")
        String fullName,

        @Schema(description = "ID đại lý của người dùng (nếu có)")
        Long dealerId,

        @Schema(description = "Tên đại lý của người dùng (nếu có)", example = "Decal Ô Tô Sài Gòn")
        String dealerName,

        @Schema(description = "Tên thiết bị do app gửi lên", example = "PC xưởng")
        String deviceName,

        @Schema(description = "Hệ điều hành / nền tảng", example = "Windows")
        String platform,

        @Schema(description = "Địa chỉ IP lần cuối ghi nhận", example = "113.161.44.2")
        String lastIp,

        @Schema(description = "Thời điểm ghi nhận lần đầu")
        LocalDateTime firstSeenAt,

        @Schema(description = "Thời điểm ghi nhận lần cuối")
        LocalDateTime lastSeenAt,

        @Schema(description = "Trạng thái thiết bị: ACTIVE | REVOKED", example = "ACTIVE")
        String status,

        @Schema(description = "Thời điểm bị gỡ/thu hồi")
        LocalDateTime revokedAt,

        @Schema(description = "Người thực hiện gỡ/thu hồi")
        String revokedBy
) {
    public static SystemDeviceResponse from(UserDevice d) {
        Long dealerId = null;
        String dealerName = null;
        if (d.getUser() != null && d.getUser().getDealer() != null) {
            dealerId = d.getUser().getDealer().getId();
            dealerName = d.getUser().getDealer().getName();
        }

        return new SystemDeviceResponse(
                d.getId(),
                d.getUser() != null ? d.getUser().getId() : null,
                d.getUser() != null ? d.getUser().getUsername() : null,
                d.getUser() != null ? d.getUser().getFullName() : null,
                dealerId,
                dealerName,
                d.getDeviceName(),
                d.getPlatform(),
                d.getLastIp(),
                d.getFirstSeenAt(),
                d.getLastSeenAt(),
                d.getStatus() != null ? d.getStatus().name() : null,
                d.getRevokedAt(),
                d.getRevokedBy()
        );
    }
}
