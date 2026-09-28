package com.example.svgmanager.dto.response;

import com.example.svgmanager.entity.UserDevice;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Máy đã đăng ký của một tài khoản (F-57)")
public record UserDeviceResponse(
        @Schema(description = "ID bản ghi đăng ký — dùng để gỡ máy") Long id,
        @Schema(description = "Định danh máy rút gọn (12 ký tự đầu)", example = "3f9a1c7e02b4") String deviceIdShort,
        @Schema(description = "Tên máy app gửi lên", example = "XUONG-01") String deviceName,
        @Schema(example = "Windows") String platform,
        @Schema(description = "ACTIVE | REVOKED") String status,
        LocalDateTime firstSeenAt,
        LocalDateTime lastSeenAt,
        String lastIp,
        LocalDateTime revokedAt,
        String revokedBy,
        @Schema(description = "Máy đang gọi API này") boolean current
) {
    public static UserDeviceResponse of(UserDevice d, String currentSessionId) {
        String id = d.getDeviceId();
        return new UserDeviceResponse(
                d.getId(),
                id != null && id.length() > 12 ? id.substring(0, 12) : id,
                d.getDeviceName(),
                d.getPlatform(),
                d.getStatus().name(),
                d.getFirstSeenAt(),
                d.getLastSeenAt(),
                d.getLastIp(),
                d.getRevokedAt(),
                d.getRevokedBy(),
                currentSessionId != null && currentSessionId.equals(d.getKeycloakSessionId())
        );
    }
}
