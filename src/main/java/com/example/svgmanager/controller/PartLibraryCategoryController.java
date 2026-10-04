package com.example.svgmanager.controller;

import com.example.svgmanager.dto.request.PartLibraryCategoryCreateRequest;
import com.example.svgmanager.dto.request.PartLibraryCategoryUpdateRequest;
import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.PartLibraryCategoryResponse;
import com.example.svgmanager.service.PartLibraryCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/part-library-categories")
@Tag(name = "Part Library Categories", description = "Quản lý Danh mục kho mẫu & part (hoàn toàn độc lập với Danh mục xe)")
@SecurityRequirement(name = "bearerAuth")
public class PartLibraryCategoryController {

    private final PartLibraryCategoryService categoryService;

    public PartLibraryCategoryController(PartLibraryCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Lấy danh sách Danh mục kho mẫu & part có phân trang",
            description = "Cho phép lọc theo trạng thái và tìm kiếm theo mã hoặc tên danh mục")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<PartLibraryCategoryResponse>> getCategories(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(defaultValue = "false") boolean all
    ) {
        int effectiveSize = all ? 1000 : size;
        return ResponseEntity.ok(categoryService.getCategories(status, search, page, effectiveSize, sortBy, sortDirection));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Lấy danh sách Danh mục kho mẫu & part đang ACTIVE",
            description = "Dùng cho dropdown lựa chọn khi upload hoặc lọc part file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PartLibraryCategoryResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<PartLibraryCategoryResponse>> getActiveCategories() {
        return ResponseEntity.ok(categoryService.getActiveCategories());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Lấy chi tiết Danh mục kho mẫu & part theo ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy chi tiết thành công",
                    content = @Content(schema = @Schema(implementation = PartLibraryCategoryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PartLibraryCategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo mới Danh mục kho mẫu & part",
            description = "Chỉ Quản trị viên (ADMIN) mới có quyền tạo mới danh mục")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tạo thành công",
                    content = @Content(schema = @Schema(implementation = PartLibraryCategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc mã đã tồn tại",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PartLibraryCategoryResponse> createCategory(@Valid @RequestBody PartLibraryCategoryCreateRequest request) {
        PartLibraryCategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật Danh mục kho mẫu & part theo ID",
            description = "Chỉ Quản trị viên (ADMIN) mới có quyền cập nhật danh mục")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
                    content = @Content(schema = @Schema(implementation = PartLibraryCategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc mã đã tồn tại",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PartLibraryCategoryResponse> updateCategory(@PathVariable Long id,
                                                                      @Valid @RequestBody PartLibraryCategoryUpdateRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa Danh mục kho mẫu & part",
            description = "Chỉ xóa được khi chưa có part file nào sử dụng. Nếu đã có file tham chiếu, cần chuyển sang INACTIVE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Xóa thành công"),
            @ApiResponse(responseCode = "400", description = "Danh mục đang có part file sử dụng",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
