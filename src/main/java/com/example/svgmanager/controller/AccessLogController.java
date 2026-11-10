package com.example.svgmanager.controller;

import com.example.svgmanager.dto.request.AccessEventRequest;
import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.service.ClientAccessLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nhật ký truy cập của app cắt — F-54 · CL-34 (contract 0.4.0). App đẩy từng sự kiện ngay khi
 * nó xảy ra, không hàng đợi; mở một file trong kho thiết kế là {@code template_download} và
 * thành một dòng {@code OPEN_FILE} ở trang Audit.
 */
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Access Log", description = "Nhật ký truy cập app cắt — mở file trong kho, đầu phiên (F-54)")
public class AccessLogController {

    private final ClientAccessLogService clientAccessLogService;

    public AccessLogController(ClientAccessLogService clientAccessLogService) {
        this.clientAccessLogService = clientAccessLogService;
    }

    @PostMapping("/api/v1/access-log")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Ghi một sự kiện truy cập của app cắt (F-54 · CL-34)",
            description = "template_download (mở một file trong kho thiết kế) → audit OPEN_FILE kèm tên file, máy, IP. "
                    + "session_start và loại lạ ghi nguyên văn. Log một chiều: 202, không nội dung.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Đã ghi nhận"),
            @ApiResponse(responseCode = "400", description = "Thiếu kind",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Không có phiên còn hiệu lực",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> record(@Valid @RequestBody AccessEventRequest event, HttpServletRequest request) {
        clientAccessLogService.record(event, clientIp(request));
        return ResponseEntity.accepted().build();
    }

    /** Như AuditTrailInterceptor: IP đầu tiên của X-Forwarded-For (sau nginx), không có thì remote addr. */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            String first = forwarded.split(",")[0].trim();
            if (first.length() <= 50 && first.matches("[a-fA-F0-9:.]+")) return first;
        }
        return request.getRemoteAddr();
    }
}
