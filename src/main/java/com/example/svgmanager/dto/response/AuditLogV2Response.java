package com.example.svgmanager.dto.response;

import com.example.svgmanager.entity.v2.AuditLogV2;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Thông tin chi tiết bản ghi Audit Log Internal V2")
public record AuditLogV2Response(
        @Schema(description = "ID bản ghi", example = "101")
        Long id,

        @Schema(description = "Request ID duy nhất để truy vết", example = "a5e8f81a-297c-47bc-9f93-5494cb680a6b")
        String requestId,

        @Schema(description = "Trace ID", example = "b7280f94-9182-4f3b-a2eb-5bb3b19c8d62")
        String traceId,

        @Schema(description = "Client ID xác thực từ Keycloak", example = "cutting-client")
        String clientId,

        @Schema(description = "Tên hiển thị của Client/App", example = "Cutting Machine Client")
        String clientName,

        @Schema(description = "Loại Principal: USER, CLIENT_ONLY, ANONYMOUS", example = "USER")
        String principalType,

        @Schema(description = "ID user nội bộ", example = "12")
        Long userId,

        @Schema(description = "ID user Keycloak", example = "kc-usr-91023")
        String keycloakUserId,

        @Schema(description = "Username tại thời điểm gọi", example = "tho.cat01")
        String username,

        @Schema(description = "Role tại thời điểm gọi", example = "USER")
        String role,

        @Schema(description = "ID đại lý", example = "3")
        Long dealerId,

        @Schema(description = "Tên đại lý", example = "Đại lý Hà Nội")
        String dealerName,

        @Schema(description = "HTTP Method", example = "POST")
        String httpMethod,

        @Schema(description = "Endpoint API", example = "/api/internal/v2/user-files")
        String endpoint,

        @Schema(description = "Phiên bản API", example = "V2")
        String apiVersion,

        @Schema(description = "HTTP Status Code", example = "201")
        Integer httpStatus,

        @Schema(description = "Kết quả: SUCCESS hoặc FAIL", example = "SUCCESS")
        String result,

        @Schema(description = "Mã lỗi an toàn (nếu có)", example = "SVG_SECURITY_REJECTED")
        String errorCode,

        @Schema(description = "Thời gian xử lý API tính bằng ms", example = "42")
        Long executionTimeMs,

        @Schema(description = "Địa chỉ IP client", example = "192.168.1.100")
        String clientIp,

        @Schema(description = "User Agent", example = "CuttingApp/2.1 (Windows NT 10.0; Win64)")
        String userAgent,

        @Schema(description = "Kích thước request tính bằng byte", example = "12500")
        Long requestSizeBytes,

        @Schema(description = "Kích thước response tính bằng byte", example = "840")
        Long responseSizeBytes,

        @Schema(description = "Thời điểm ghi nhận audit", example = "2026-10-24T16:00:00")
        LocalDateTime createdAt
) {
    public static AuditLogV2Response fromEntity(AuditLogV2 entity) {
        if (entity == null) {
            return null;
        }

        return new AuditLogV2Response(
                entity.getId(),
                entity.getRequestId(),
                entity.getTraceId(),
                entity.getClientId(),
                entity.getClientName(),
                entity.getPrincipalType(),
                entity.getUserId(),
                entity.getKeycloakUserId(),
                entity.getUsername(),
                entity.getRole(),
                entity.getDealerId(),
                entity.getDealerName(),
                entity.getHttpMethod(),
                entity.getEndpoint(),
                entity.getApiVersion(),
                entity.getHttpStatus(),
                entity.getResult(),
                entity.getErrorCode(),
                entity.getExecutionTimeMs(),
                entity.getClientIp(),
                entity.getUserAgent(),
                entity.getRequestSizeBytes(),
                entity.getResponseSizeBytes(),
                entity.getCreatedAt()
        );
    }
}
