package com.example.svgmanager.controller.admin.v2;

import com.example.svgmanager.dto.internal.v2.FileSharesV2Response;
import com.example.svgmanager.dto.internal.v2.UserSvgFileV2Response;
import com.example.svgmanager.dto.internal.v2.UserSvgFileV2ShareRequest;
import com.example.svgmanager.dto.internal.v2.UserSvgFileV2ShareResponse;
import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.internal.v2.InternalUserFileV2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/v2/user-files")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin User Saved Files V2", description = "Quản lý toàn bộ bản file SVG V2 có mã hoá AES-256-GCM của người dùng (dành riêng cho ADMIN)")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserFilesV2Controller {

    private final InternalUserFileV2Service userFileV2Service;
    private final CurrentUserService currentUserService;

    public AdminUserFilesV2Controller(
            InternalUserFileV2Service userFileV2Service,
            CurrentUserService currentUserService
    ) {
        this.userFileV2Service = userFileV2Service;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @Operation(summary = "Xem danh sách toàn bộ bản SVG đã lưu V2 có mã hoá của người dùng (ADMIN)",
            description = "ADMIN được quyền xem toàn bộ bản lưu V2 của mọi User, hỗ trợ phân trang, tìm kiếm và lọc.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Danh sách bản lưu SVG V2 phân trang"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền ADMIN", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<UserSvgFileV2Response>> listUserFiles(
            @Parameter(description = "Từ khoá tìm kiếm theo tên file, mô tả, mẫu xe...") @RequestParam(required = false) String keyword,
            @Parameter(description = "Lọc theo danh mục") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Lọc theo cấu hình xe (alias)") @RequestParam(required = false) Long vehicleConfigurationId,
            @Parameter(description = "Lọc theo node xe (vehicleNodeId)") @RequestParam(required = false) Long vehicleNodeId,
            @Parameter(description = "Lọc theo hãng xe (brandId)") @RequestParam(required = false) Long brandId,
            @Parameter(description = "Lọc theo dòng xe (modelId)") @RequestParam(required = false) Long modelId,
            @Parameter(description = "Lọc theo đại lý") @RequestParam(required = false) Long dealerId,
            @Parameter(description = "Lọc theo người dùng tạo") @RequestParam(required = false) Long userId,
            @Parameter(description = "Lọc từ ngày tạo (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @Parameter(description = "Lọc đến ngày tạo (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @Parameter(description = "Trạng thái (ACTIVE, DELETED). Mặc định lọc trừ DELETED") @RequestParam(required = false) String status,
            @Parameter(description = "Số trang (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước trang") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sắp xếp, ví dụ: createdAt,desc") @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Long effectiveNodeId = vehicleNodeId != null ? vehicleNodeId : vehicleConfigurationId;
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sortObj);

        PageResponse<UserSvgFileV2Response> response = userFileV2Service.listAdminFiles(
                keyword, categoryId, effectiveNodeId, brandId, modelId, dealerId, userId,
                createdFrom, createdTo, status, pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một bản SVG V2 đã lưu (ADMIN)",
            description = "Trả về thông tin chi tiết đầy đủ của bản lưu SVG V2 (không bao gồm DEK/IV hoặc đường dẫn nội bộ).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thông tin chi tiết bản lưu V2"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền ADMIN", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bản lưu V2", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserSvgFileV2Response> getFileDetail(@PathVariable Long id) {
        return ResponseEntity.ok(userFileV2Service.getAdminFile(id));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Tải file SVG V2 đã giải mã của người dùng (ADMIN)",
            description = "ADMIN tải trực tiếp file SVG sau khi giải mã AES-256-GCM từ V2 storage.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tải file SVG V2 thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền ADMIN", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy file", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        Resource resource = userFileV2Service.downloadAdminFile(id);
        String filename = userFileV2Service.getOriginalFilename(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("image/svg+xml;charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("Cache-Control", "private, no-store");

        return ResponseEntity.ok().headers(headers).body(resource);
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "Xem trước nội dung file SVG V2 đã giải mã (ADMIN, inline stream)",
            description = "Stream nội dung SVG an toàn cho component xem trước trực tiếp trên giao diện Admin với CSP và nosniff.")
    public ResponseEntity<byte[]> previewFile(@PathVariable Long id) {
        byte[] bytes = userFileV2Service.getAdminFileBytes(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("image/svg+xml;charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.inline().build());
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("Content-Security-Policy", "default-src 'none'; img-src 'self' data:; style-src 'unsafe-inline'; sandbox");
        headers.add("Cache-Control", "private, no-store");

        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @PostMapping("/{id}/shares")
    @Operation(summary = "Chia sẻ file SVG V2 cho người dùng (ADMIN)",
            description = "ADMIN chia sẻ quyền xem/tải file SVG V2 của bất kỳ user nào cho một USER khác.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Chia sẻ file V2 thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc người nhận không hợp lệ", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền ADMIN", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy file hoặc người nhận", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserSvgFileV2ShareResponse> shareFile(
            @PathVariable Long id,
            @Valid @RequestBody UserSvgFileV2ShareRequest request
    ) {
        User currentUser = currentUserService.getCurrentUser();
        UserSvgFileV2ShareResponse response = userFileV2Service.shareFile(currentUser, id, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/shares")
    @Operation(summary = "Xem danh sách người dùng được chia sẻ file SVG V2 (ADMIN)",
            description = "ADMIN xem danh sách người dùng đang được chia sẻ file V2.")
    public ResponseEntity<FileSharesV2Response> getFileShares(@PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUser();
        return ResponseEntity.ok(userFileV2Service.getShares(currentUser, id));
    }

    @DeleteMapping("/{id}/shares/{targetUserId}")
    @Operation(summary = "Thu hồi quyền chia sẻ file SVG V2 (ADMIN)",
            description = "ADMIN thu hồi quyền chia sẻ file V2 từ một USER.")
    public ResponseEntity<Void> revokeFileShare(
            @PathVariable Long id,
            @PathVariable Long targetUserId
    ) {
        User currentUser = currentUserService.getCurrentUser();
        userFileV2Service.revokeShare(currentUser, id, targetUserId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá mềm một bản lưu SVG V2 (ADMIN)",
            description = "ADMIN xoá mềm bản lưu V2. Đặt status = DELETED. Trả về 204.")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        userFileV2Service.deleteAdminFile(id);
        return ResponseEntity.noContent().build();
    }

    private Sort parseSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sort.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}
