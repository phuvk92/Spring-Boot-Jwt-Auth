package com.example.svgmanager.controller.admin.v2;

import com.example.svgmanager.dto.response.AuditLogV2Response;
import com.example.svgmanager.dto.response.AuditLogV2StatsResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.service.internal.v2.AuditLogV2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;

@RestController
@RequestMapping(value = {"/api/admin/audit/v2", "/api/admin/v2/audit-logs"})
@Tag(name = "Admin Audit Log V2", description = "Quản lý và tra cứu Audit Log Internal API V2 từ client bên ngoài (Dành riêng cho ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogV2Controller {

    private final AuditLogV2Service auditLogV2Service;

    public AdminAuditLogV2Controller(AuditLogV2Service auditLogV2Service) {
        this.auditLogV2Service = auditLogV2Service;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách Audit Log Internal V2 có phân trang và lọc",
            description = "Cho phép ADMIN xem toàn bộ lịch sử gọi API Internal V2 của client máy cắt và ứng dụng tích hợp.")
    public ResponseEntity<PageResponse<AuditLogV2Response>> getAuditLogs(
            @Parameter(description = "Từ khoá tìm kiếm chung (requestId, endpoint, username, clientId...)") @RequestParam(required = false) String keyword,
            @Parameter(description = "Client ID xác thực từ Keycloak") @RequestParam(required = false) String clientId,
            @Parameter(description = "Tên hiển thị Client") @RequestParam(required = false) String clientName,
            @Parameter(description = "Tên tài khoản (username)") @RequestParam(required = false) String username,
            @Parameter(description = "ID User nội bộ") @RequestParam(required = false) Long userId,
            @Parameter(description = "Vai trò (USER, AGENT, ADMIN)") @RequestParam(required = false) String role,
            @Parameter(description = "ID đại lý") @RequestParam(required = false) Long dealerId,
            @Parameter(description = "Đường dẫn API (endpoint)") @RequestParam(required = false) String endpoint,
            @Parameter(description = "HTTP Method (GET, POST, PUT, DELETE)") @RequestParam(required = false) String httpMethod,
            @Parameter(description = "HTTP Status Code (200, 201, 400, 401...)") @RequestParam(required = false) Integer httpStatus,
            @Parameter(description = "Kết quả (SUCCESS hoặc FAIL)") @RequestParam(required = false) String result,
            @Parameter(description = "Request ID") @RequestParam(required = false) String requestId,
            @Parameter(description = "Từ thời điểm (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Đến thời điểm (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        int safeSize = Math.min(Math.max(1, size), 100);
        int safePage = Math.max(0, page);

        String safeSortBy = "createdAt";
        if (sortBy != null && !sortBy.isBlank()) {
            safeSortBy = switch (sortBy) {
                case "id", "requestId", "clientId", "username", "httpMethod", "endpoint", "httpStatus", "result", "executionTimeMs", "createdAt" -> sortBy;
                default -> "createdAt";
            };
        }

        Sort sort = Sort.by(
                "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC,
                safeSortBy
        );
        Pageable pageable = PageRequest.of(safePage, safeSize, sort);

        Page<AuditLogV2Response> responsePage = auditLogV2Service.getAuditLogs(
                keyword, clientId, clientName, username, userId, role,
                dealerId, endpoint, httpMethod, httpStatus, result, requestId,
                from, to, pageable
        );

        return ResponseEntity.ok(PageResponse.of(responsePage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một bản ghi Audit Log Internal V2 theo ID")
    public ResponseEntity<AuditLogV2Response> getAuditLogById(@PathVariable Long id) {
        return auditLogV2Service.getAuditLogById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    @Operation(summary = "Thống kê tổng quan Audit Log Internal V2 phục vụ Dashboard",
            description = "Trả về tổng request, tỷ lệ thành công/thất bại, top endpoint, top client, top user.")
    public ResponseEntity<AuditLogV2StatsResponse> getStats(
            @Parameter(description = "Từ thời điểm (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Đến thời điểm (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "Lọc theo Client ID") @RequestParam(required = false) String clientId,
            @Parameter(description = "Lọc theo Đại lý") @RequestParam(required = false) Long dealerId
    ) {
        return ResponseEntity.ok(auditLogV2Service.getStats(from, to, clientId, dealerId));
    }

    @GetMapping("/export")
    @Operation(summary = "Xuất danh sách Audit Log Internal V2 ra file CSV",
            description = "Xuất dữ liệu theo bộ lọc hiện tại dưới định dạng CSV hỗ trợ mở bằng Microsoft Excel.")
    public void exportCsv(
            @Parameter(description = "Từ khoá tìm kiếm chung") @RequestParam(required = false) String keyword,
            @Parameter(description = "Client ID") @RequestParam(required = false) String clientId,
            @Parameter(description = "Tên hiển thị Client") @RequestParam(required = false) String clientName,
            @Parameter(description = "Tên tài khoản (username)") @RequestParam(required = false) String username,
            @Parameter(description = "ID User nội bộ") @RequestParam(required = false) Long userId,
            @Parameter(description = "Vai trò") @RequestParam(required = false) String role,
            @Parameter(description = "ID đại lý") @RequestParam(required = false) Long dealerId,
            @Parameter(description = "Đường dẫn API (endpoint)") @RequestParam(required = false) String endpoint,
            @Parameter(description = "HTTP Method") @RequestParam(required = false) String httpMethod,
            @Parameter(description = "HTTP Status Code") @RequestParam(required = false) Integer httpStatus,
            @Parameter(description = "Kết quả (SUCCESS hoặc FAIL)") @RequestParam(required = false) String result,
            @Parameter(description = "Request ID") @RequestParam(required = false) String requestId,
            @Parameter(description = "Từ thời điểm") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Đến thời điểm") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            HttpServletResponse response
    ) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-log-v2-" + System.currentTimeMillis() + ".csv\"");

        auditLogV2Service.exportCsv(
                keyword, clientId, clientName, username, userId, role,
                dealerId, endpoint, httpMethod, httpStatus, result, requestId,
                from, to, response.getWriter()
        );
    }
}
