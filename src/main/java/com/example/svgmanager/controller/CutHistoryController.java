package com.example.svgmanager.controller;

import com.example.svgmanager.dto.response.CutHistoryResponse;
import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.service.CutHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/v1/cuts — lịch sử cắt trên máy này (F-38 · KX-03), hợp đồng openapi v0.3.0.
 * Chỉ số liệu: F-38 cấm hình học ở đây, chặn bằng shape DTO — không có trường nào để lộ.
 */
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cut History", description = "Lịch sử cắt theo thiết bị — chỉ số liệu, không hình học (F-38)")
public class CutHistoryController {

    private final CutHistoryService cutHistoryService;

    public CutHistoryController(CutHistoryService cutHistoryService) {
        this.cutHistoryService = cutHistoryService;
    }

    @GetMapping("/api/v1/cuts")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Lịch sử cắt trên máy này (F-38 · KX-03)",
            description = "Scope theo máy trong token (claim sid). Trường nào thiếu dữ liệu nguồn thì null, không ném.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bốn số thống kê và danh sách job",
                    content = @Content(schema = @Schema(implementation = CutHistoryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Không có phiên còn hiệu lực",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CutHistoryResponse> getCutHistory() {
        return ResponseEntity.ok(cutHistoryService.getHistoryForCurrentDevice());
    }
}
