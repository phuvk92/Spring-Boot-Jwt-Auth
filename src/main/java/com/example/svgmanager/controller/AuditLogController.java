package com.example.svgmanager.controller;

import com.example.svgmanager.dto.response.AuditLogResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.entity.AuditLog;
import com.example.svgmanager.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit Logs", description = "Quản lý và tra cứu Audit Trail & Activity Log của hệ thống (Dành riêng cho ADMIN)")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách Audit Trail & Activity Log có phân trang và lọc",
            description = "Cho phép ADMIN xem toàn bộ lịch sử hoạt động, gọi API của User và Đại lý.")
    public ResponseEntity<PageResponse<AuditLogResponse>> getAuditLogs(
            @Parameter(description = "Từ khoá tìm kiếm chung") @RequestParam(required = false) String keyword,
            @Parameter(description = "Tên entity/tài nguyên") @RequestParam(required = false) String entity,
            @Parameter(description = "Tài khoản thực hiện (actor)") @RequestParam(required = false) String actor,
            @Parameter(description = "Vai trò (USER, AGENT, ADMIN)") @RequestParam(required = false) String actorRole,
            @Parameter(description = "Hành động (action)") @RequestParam(required = false) String action,
            @Parameter(description = "Từ thời điểm") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Đến thời điểm") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Sort sort = Sort.by(
                "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC,
                sortBy != null && !sortBy.isBlank() ? sortBy : "timestamp"
        );
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);

        Page<AuditLog> logs = auditLogService.getLogs(keyword, entity, actor, actorRole, action, from, to, pageable);
        Page<AuditLogResponse> responsePage = logs.map(AuditLogResponse::fromEntity);

        return ResponseEntity.ok(PageResponse.of(responsePage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một bản ghi Audit Log theo ID")
    public ResponseEntity<AuditLogResponse> getAuditLogById(@PathVariable Long id) {
        return auditLogService.getLogById(id)
                .map(AuditLogResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
