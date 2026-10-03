package com.example.svgmanager.controller;

import com.example.svgmanager.dto.request.DesignSaveRequest;
import com.example.svgmanager.dto.response.DesignVersionResponse;
import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.dto.response.SavedDesignResponse;
import com.example.svgmanager.service.SavedDesignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bản làm việc đã lưu — openapi v0.3.0 (F-37 · KX-02).
 * Mảng rỗng = chưa lưu bản nào, KHÁC với lỗi: hợp đồng không trả null body mập mờ,
 * lỗi đi qua ErrorResponse với code rõ ràng.
 */
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Saved Designs", description = "Bản làm việc đã lưu của thợ — chỉ metadata, không hình học")
public class SavedDesignController {

    private final SavedDesignService savedDesignService;

    public SavedDesignController(SavedDesignService savedDesignService) {
        this.savedDesignService = savedDesignService;
    }

    @GetMapping("/api/v1/designs")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Bản làm việc đã lưu của chính user này (F-37 · KX-02)",
            description = "Lọc theo chủ sở hữu (D1 — bản sao riêng, DS-84b), không trả lẫn mẫu trong kho. "
                    + "Rỗng = chưa lưu bản nào — khác với lỗi server (ErrorResponse).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Danh sách bản làm việc (có thể rỗng)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SavedDesignResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Không có phiên còn hiệu lực",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<SavedDesignResponse>> getSavedDesigns() {
        return ResponseEntity.ok(savedDesignService.getSavedDesigns());
    }

    @GetMapping("/api/v1/designs/{id}/versions")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Các lần lưu của một bản làm việc (F-37)",
            description = "Mới nhất trước, chỉ metadata. Bản không tồn tại (hoặc không phải của user) → 404, "
                    + "không trả mảng rỗng — 'chưa có phiên bản' và 'không có bản này' là hai câu khác nhau.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Danh sách phiên bản, mới nhất trước",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DesignVersionResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Không có phiên còn hiệu lực",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không có bản làm việc này (DESIGN_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<DesignVersionResponse>> getVersions(@PathVariable("id") String designKey) {
        return ResponseEntity.ok(savedDesignService.getVersions(designKey));
    }

    @PostMapping("/api/v1/designs")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Tạo bản làm việc trên cloud — lần Lưu đầu (F-36 · DS-05 · DS-148)",
            description = "Tạo bản làm việc mới và phiên bản 1. Sinh design_key ở server. Trả về 201 Created.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Bản làm việc vừa tạo",
                    content = @Content(schema = @Schema(implementation = SavedDesignResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload không hợp lệ (DESIGN_INVALID)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Không có phiên còn hiệu lực",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "Payload vượt quá 20 MB (DESIGN_TOO_LARGE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SavedDesignResponse> createDesign(@Valid @RequestBody DesignSaveRequest request) {
        SavedDesignResponse response = savedDesignService.createDesign(request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/v1/designs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Cập nhật bản làm việc — các lần Lưu sau (F-36 · DS-05 · DS-151)",
            description = "Thêm một phiên bản mới trong lịch sử của bản đó, cập nhật siêu dữ liệu. Không đổi sourceTemplateId.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bản làm việc sau khi lưu (versionCount đã tăng 1)",
                    content = @Content(schema = @Schema(implementation = SavedDesignResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload không hợp lệ (DESIGN_INVALID)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Không có phiên còn hiệu lực",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không có bản làm việc này (DESIGN_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "Payload vượt quá 20 MB (DESIGN_TOO_LARGE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SavedDesignResponse> updateDesign(
            @PathVariable("id") String designKey,
            @Valid @RequestBody DesignSaveRequest request) {
        return ResponseEntity.ok(savedDesignService.updateDesign(designKey, request));
    }

    @GetMapping("/api/v1/designs/{id}/content")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Nội dung bản làm việc để mở lại (F-36)",
            description = "Lấy nội dung payload của phiên bản chỉ định hoặc bản hiện hành. Chỉ chủ sở hữu xem được.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Nội dung bản làm việc",
                    content = @Content(schema = @Schema(implementation = com.example.svgmanager.dto.response.DesignContentResponse.class))),
            @ApiResponse(responseCode = "401", description = "Không có phiên còn hiệu lực",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không có bản làm việc hoặc phiên bản này (DESIGN_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<com.example.svgmanager.dto.response.DesignContentResponse> getDesignContent(
            @PathVariable("id") String designKey,
            @org.springframework.web.bind.annotation.RequestParam(value = "version", required = false) Integer version) {
        return ResponseEntity.ok(savedDesignService.getDesignContent(designKey, version));
    }
}
