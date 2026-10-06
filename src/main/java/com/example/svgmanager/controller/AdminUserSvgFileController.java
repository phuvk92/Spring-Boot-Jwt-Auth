package com.example.svgmanager.controller;

import com.example.svgmanager.dto.request.ShareFileRequest;
import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.dto.response.FileSharesResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.UserSavedFileResponse;
import com.example.svgmanager.dto.response.UserSvgFileShareResponse;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.UserSvgFileService;
import com.example.svgmanager.service.UserSvgFileShareService;
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
@RequestMapping("/api/admin/user-files")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin User Saved Files", description = "Quản lý toàn bộ bản file SVG do người dùng lưu (dành riêng cho ADMIN)")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserSvgFileController {

    private final UserSvgFileService userSvgFileService;
    private final UserSvgFileShareService userSvgFileShareService;
    private final CurrentUserService currentUserService;

    public AdminUserSvgFileController(UserSvgFileService userSvgFileService,
                                     UserSvgFileShareService userSvgFileShareService,
                                     CurrentUserService currentUserService) {
        this.userSvgFileService = userSvgFileService;
        this.userSvgFileShareService = userSvgFileShareService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @Operation(summary = "Xem danh sách toàn bộ bản SVG đã lưu của người dùng",
            description = "ADMIN được quyền xem toàn bộ bản lưu của mọi User, hỗ trợ phân trang, tìm kiếm và lọc.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Danh sách bản lưu SVG phân trang"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền ADMIN", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<UserSavedFileResponse>> listUserFiles(
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
            @Parameter(description = "Sắp xếp, ví dụ: createdAt,desc hoặc fileName,asc") @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Long effectiveNodeId = vehicleNodeId != null ? vehicleNodeId : vehicleConfigurationId;
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sortObj);

        PageResponse<UserSavedFileResponse> response = userSvgFileService.listAdminFiles(
                keyword, categoryId, effectiveNodeId, brandId, modelId, dealerId, userId,
                createdFrom, createdTo, status, pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một bản SVG đã lưu",
            description = "Trả về thông tin chi tiết đầy đủ của bản lưu SVG (không bao gồm đường dẫn lưu trữ vật lý nội bộ).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thông tin chi tiết bản lưu"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền ADMIN", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bản lưu", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserSavedFileResponse> getFileDetail(@PathVariable Long id) {
        return ResponseEntity.ok(userSvgFileService.getAdminFile(id));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Tải file SVG đã lưu của người dùng",
            description = "ADMIN tải trực tiếp file SVG nhị phân.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tải file SVG thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền ADMIN", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy file", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        Resource resource = userSvgFileService.downloadAdminFile(id);
        String filename = userSvgFileService.getOriginalFilename(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("image/svg+xml"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());

        return ResponseEntity.ok().headers(headers).body(resource);
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "Xem trước nội dung file SVG (inline stream)",
            description = "Stream nội dung SVG an toàn cho component xem trước trực tiếp trên giao diện Admin.")
    public ResponseEntity<byte[]> previewFile(@PathVariable Long id) {
        byte[] bytes = userSvgFileService.getAdminFileBytes(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("image/svg+xml;charset=UTF-8"));
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");

        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @PostMapping("/{id}/shares")
    @Operation(summary = "Chia sẻ file SVG cho người dùng (ADMIN)",
            description = "ADMIN chia sẻ quyền xem/tải file SVG của bất kỳ user nào cho một USER khác.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Chia sẻ file thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc người nhận không hợp lệ", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền ADMIN", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy file hoặc người nhận", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserSvgFileShareResponse> shareFile(
            @PathVariable Long id,
            @Valid @RequestBody ShareFileRequest request
    ) {
        User currentUser = currentUserService.getCurrentUser();
        UserSvgFileShareResponse response = userSvgFileShareService.shareFile(currentUser, id, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/shares")
    @Operation(summary = "Xem danh sách người dùng được chia sẻ file SVG (ADMIN)",
            description = "ADMIN xem danh sách người dùng đang được chia sẻ file.")
    public ResponseEntity<FileSharesResponse> getFileShares(@PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUser();
        return ResponseEntity.ok(userSvgFileShareService.getShares(currentUser, id));
    }

    @DeleteMapping("/{id}/shares/{targetUserId}")
    @Operation(summary = "Thu hồi quyền chia sẻ file SVG (ADMIN)",
            description = "ADMIN thu hồi quyền chia sẻ file từ một USER.")
    public ResponseEntity<Void> revokeFileShare(
            @PathVariable Long id,
            @PathVariable Long targetUserId
    ) {
        User currentUser = currentUserService.getCurrentUser();
        userSvgFileShareService.revokeShare(currentUser, id, targetUserId);
        return ResponseEntity.noContent().build();
    }

    private Sort parseSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sort.split(",");
        String prop = parts[0].trim();
        Sort.Direction dir = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(dir, prop);
    }
}
