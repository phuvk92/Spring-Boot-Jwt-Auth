package com.example.svgmanager.controller;

import com.example.svgmanager.dto.response.CatalogOptionDto;
import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

import java.util.List;

/**
 * Danh mục cho app thợ — Data Center v2 (SA-DanhMucXe-v2 §3.3, hợp đồng v0.6).
 */
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Catalog", description = "Cây xe 4 cấp + danh mục file cho bộ lọc của app thợ")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/api/v1/file-categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Danh mục file (Ngoại thất · Nội thất · Window film · Đèn & kính)",
            description = "Danh mục do Admin quản, client không hard-code. value là id của file_categories.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Danh mục đang hiệu lực, theo display_order",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CatalogOptionDto.class)))),
            @ApiResponse(responseCode = "401", description = "Không có phiên còn hiệu lực",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<CatalogOptionDto>> getFileCategories() {
        return ResponseEntity.ok(catalogService.getFileCategories());
    }

    @GetMapping("/api/v1/catalog/{level}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Giá trị hợp lệ của một cấp cây xe, lọc theo id cha",
            description = "series cần brandId · model cần seriesId · subtype cần modelId — thiếu trả 400. "
                    + "year là cấp đặc biệt: trả các năm có file khớp categoryId/modelId/subtypeId đã chọn.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Danh sách rỗng nghĩa là cấp trên hợp lệ nhưng dưới không có gì",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CatalogOptionDto.class)))),
            @ApiResponse(responseCode = "400", description = "Thiếu tham số id của cấp cha",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Không có phiên còn hiệu lực",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<CatalogOptionDto>> getCatalog(
            @PathVariable String level,
            @Parameter(description = "Bắt buộc khi level=series") @RequestParam(required = false) String brandId,
            @Parameter(description = "Bắt buộc khi level=model") @RequestParam(required = false) String seriesId,
            @Parameter(description = "Bắt buộc khi level=subtype; lọc khi level=year") @RequestParam(required = false) String modelId,
            @Parameter(description = "Lọc khi level=year") @RequestParam(required = false) String subtypeId,
            @Parameter(description = "Lọc khi level=year") @RequestParam(required = false) String categoryId) {
        return ResponseEntity.ok(
                catalogService.getCatalog(level, brandId, seriesId, modelId, subtypeId, categoryId));
    }
}
