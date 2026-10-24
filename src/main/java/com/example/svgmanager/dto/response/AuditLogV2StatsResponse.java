package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Thống kê tổng quan tình hình gọi Internal API V2")
public record AuditLogV2StatsResponse(
        @Schema(description = "Tổng số request", example = "1250")
        long totalRequests,

        @Schema(description = "Số request thành công (HTTP 2xx, 3xx)", example = "1180")
        long successCount,

        @Schema(description = "Số request thất bại (HTTP 4xx, 5xx)", example = "70")
        long failCount,

        @Schema(description = "Tỷ lệ thành công (%)", example = "94.4")
        double successRate,

        @Schema(description = "Top các endpoint được gọi nhiều nhất")
        List<EndpointStat> topEndpoints,

        @Schema(description = "Top các client/ứng dụng gọi nhiều nhất")
        List<ClientStat> topClients,

        @Schema(description = "Top các tài khoản người dùng gọi nhiều nhất")
        List<UserStat> topUsers
) {
    public record EndpointStat(
            String endpoint,
            String httpMethod,
            long count,
            double avgLatencyMs
    ) {}

    public record ClientStat(
            String clientId,
            String clientName,
            long count
    ) {}

    public record UserStat(
            String username,
            String role,
            long count
    ) {}
}
