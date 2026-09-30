package com.example.svgmanager.controller;

import com.example.svgmanager.dto.response.AdminFileResponse;
import com.example.svgmanager.dto.response.AdminFileStatsResponse;
import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.service.AdminFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Kho part file — SA-DanhMucXe-v2 §3.2 (màn "Kho mẫu & part file" trên web admin).
 * Chỉ ADMIN. Upload chỉ nhận .svg; vehicleNodeIds ≥1, mỗi node MODEL/SUBTYPE.
 */
@RestController
@RequestMapping("/api/admin/files")
@Tag(name = "Admin Part Files", description = "Kho mẫu & part file — Data Center v2")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminFileController {

    private final AdminFileService adminFileService;

    public AdminFileController(AdminFileService adminFileService) {
        this.adminFileService = adminFileService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách file trong kho, lọc + phân trang",
            description = "Lọc theo q · categoryId · year · brandId/seriesId/modelId (nhánh cây). File không ghi năm hiện với mọi năm (Q3).")
    public ResponseEntity<PageResponse<AdminFileResponse>> getFiles(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long seriesId,
            @RequestParam(required = false) Long modelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminFileService.getFiles(q, categoryId, year,
                brandId, seriesId, modelId, page, size));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bốn thẻ thống kê kho file",
            description = "unlinked = file ACTIVE không có liên kết mẫu xe (sinh ra khi xoá node — Q4).")
    public ResponseEntity<AdminFileStatsResponse> getStats() {
        return ResponseEntity.ok(adminFileService.getStats());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload file thiết kế mới",
            description = "Chỉ .svg (kiểm cả đuôi lẫn nội dung — khác → 400 UNSUPPORTED_FORMAT). "
                    + "File không khai đơn vị → 400 SVG_UNITS_MISSING. Server tách part + hình học (SA §4).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Đã tạo file + part",
                    content = @Content(schema = @Schema(implementation = AdminFileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Định dạng sai / thiếu đơn vị / node không hợp lệ",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AdminFileResponse> createFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam("vehicleNodeIds") List<Long> vehicleNodeIds,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminFileService.createFile(file, name, categoryId, year, vehicleNodeIds, thumbnail));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sửa file trong kho",
            description = "Như POST nhưng mọi trường tuỳ chọn; file mới (nếu có) được tách lại part, "
                    + "thay toàn bộ svg_file_parts. year= (rỗng) xoá năm — file hiện mọi năm.")
    public ResponseEntity<AdminFileResponse> updateFile(
            @PathVariable Long id,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "vehicleNodeIds", required = false) List<Long> vehicleNodeIds,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            HttpServletRequest request) {
        // Tham số year HIỆN DIỆN (kể cả rỗng) nghĩa là client muốn đặt lại năm — để
        // phân biệt "không gửi" (giữ nguyên) với "gửi rỗng" (xoá năm → mọi năm).
        boolean yearPresent = request.getParameterMap().containsKey("year");
        return ResponseEntity.ok(adminFileService.updateFile(id, file, name, categoryId,
                year, yearPresent, vehicleNodeIds, thumbnail));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xoá mềm file (status = DELETED)")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        adminFileService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }
}
