package com.example.svgmanager.service.internal.v2;

import com.example.svgmanager.dto.internal.v2.InternalApiV2AuditEvent;
import com.example.svgmanager.dto.response.AuditLogV2Response;
import com.example.svgmanager.dto.response.AuditLogV2StatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Optional;

public interface AuditLogV2Service {

    /**
     * Ghi nhận một bản ghi audit log cho Internal API V2 một cách an toàn và độc lập.
     */
    void recordAuditLog(InternalApiV2AuditEvent event);

    /**
     * Tra cứu danh sách audit log có phân trang và bộ lọc linh hoạt.
     */
    Page<AuditLogV2Response> getAuditLogs(
            String keyword,
            String clientId,
            String clientName,
            String username,
            Long userId,
            String role,
            Long dealerId,
            String endpoint,
            String httpMethod,
            Integer httpStatus,
            String result,
            String requestId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    /**
     * Lấy chi tiết một bản ghi audit log theo ID.
     */
    Optional<AuditLogV2Response> getAuditLogById(Long id);

    /**
     * Lấy dữ liệu thống kê phục vụ dashboard.
     */
    AuditLogV2StatsResponse getStats(LocalDateTime from, LocalDateTime to, String clientId, Long dealerId);

    /**
     * Xuất danh sách audit log ra định dạng CSV an toàn theo bộ lọc hiện tại.
     */
    void exportCsv(
            String keyword,
            String clientId,
            String clientName,
            String username,
            Long userId,
            String role,
            Long dealerId,
            String endpoint,
            String httpMethod,
            Integer httpStatus,
            String result,
            String requestId,
            LocalDateTime from,
            LocalDateTime to,
            PrintWriter writer
    );
}
