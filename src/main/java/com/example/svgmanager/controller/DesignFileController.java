package com.example.svgmanager.controller;

import com.example.svgmanager.dto.response.DesignFileDto;
import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.service.DesignFileService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * Kho file thiết kế cho app cắt — KX-32 · F-56. Đầu danh sách file (GET /api/v1/files)
 * đi theo mô hình cây xe mới ở NGO-325/326 (SA-DanhMucXe-v2 §3.3).
 */
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Design Files", description = "Kho file thiết kế — part và hình học theo fileKey")
public class DesignFileController {

    private final DesignFileService designFileService;

    public DesignFileController(DesignFileService designFileService) {
        this.designFileService = designFileService;
    }

    @GetMapping("/api/v1/files")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "File thiết kế khớp bộ lọc — mọi tham số tuỳ chọn, phân trang (Data Center v2)",
            description = "Lọc theo q · categoryId · year · brandId/seriesId/modelId/subtypeId. "
                    + "Không truyền gì → mọi file ACTIVE. "
                    + "Cấp xe: brandId/seriesId/modelId khớp file gắn node hoặc bất kỳ node con nào; "
                    + "subtypeId khớp subtype hoặc model cha. "
                    + "year: model_year = year HOẶC NULL (Q3). "
                    + "Chỉ file ACTIVE, không lọc quyền đại lý (Q6). Sắp xếp updatedAt giảm dần.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Danh sách phân trang các file thiết kế",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Không có phiên còn hiệu lực",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<DesignFileDto>> getFiles(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long seriesId,
            @RequestParam(required = false) Long modelId,
            @RequestParam(required = false) Long subtypeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(designFileService.getFiles(q, categoryId, year,
                brandId, seriesId, modelId, subtypeId, page, size));
    }

    @GetMapping(value = "/api/v1/files/{id}/svg", produces = "image/svg+xml")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Nội dung SVG của part file — app tự tách part (board 08/10)",
            description = "layout = nested | raw; bỏ trống → bản đã xếp nếu có. SVG đã qua bộ khử độc lúc upload. "
                    + "Trả inline, KHÔNG Content-Disposition (RB-01: app giữ trong RAM, không ghi đĩa).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Nội dung SVG (UTF-8)",
                    content = @Content(mediaType = "image/svg+xml", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "layout không hợp lệ",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Không có phiên còn hiệu lực",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không có file / không có bản được hỏi (FILE_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<String> getFileSvg(@PathVariable("id") String fileKey,
                                             @RequestParam(required = false) String layout) {
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.valueOf("image/svg+xml;charset=UTF-8"))
                .header("Cache-Control", "no-store")
                .body(designFileService.getFileSvg(fileKey, layout));
    }
}
