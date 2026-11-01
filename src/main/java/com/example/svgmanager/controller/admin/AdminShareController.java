package com.example.svgmanager.controller.admin;

import com.example.svgmanager.dto.request.AdminRevokeShareRequest;
import com.example.svgmanager.dto.response.AdminShareActivityResponse;
import com.example.svgmanager.dto.response.AdminShareItemResponse;
import com.example.svgmanager.dto.response.AdminShareStatsResponse;
import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.AdminShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/shares")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Shares", description = "Quản lý tập trung toàn bộ hoạt động chia sẻ file SVG (dành riêng cho ADMIN)")
@PreAuthorize("hasRole('ADMIN')")
public class AdminShareController {

    private final AdminShareService adminShareService;
    private final CurrentUserService currentUserService;

    public AdminShareController(AdminShareService adminShareService, CurrentUserService currentUserService) {
        this.adminShareService = adminShareService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @Operation(summary = "Xem danh sách toàn bộ lượt chia sẻ file SVG (ADMIN)",
            description = "ADMIN tra cứu toàn bộ lượt chia sẻ (Admin Share và Client Share), hỗ trợ lọc theo loại, trạng thái, đại lý, khoảng thời gian và phân trang.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Danh sách lượt chia sẻ phân trang"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền ADMIN", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<AdminShareItemResponse>> listShares(
            @Parameter(description = "Loại chia sẻ: ALL, ADMIN_TO_CLIENT, CLIENT_TO_CLIENT") @RequestParam(required = false) String shareType,
            @Parameter(description = "Trạng thái: ALL, ACTIVE, REVOKED, EXPIRED") @RequestParam(required = false) String status,
            @Parameter(description = "Từ khoá tìm kiếm theo Share ID, tên file, username, client ID") @RequestParam(required = false) String keyword,
            @Parameter(description = "Lọc theo ID đại lý") @RequestParam(required = false) Long dealerId,
            @Parameter(description = "Lọc theo ID file SVG") @RequestParam(required = false) Long fileId,
            @Parameter(description = "Lọc từ ngày tạo (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Lọc đến ngày tạo (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "Số trang (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước trang") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sắp xếp, ví dụ: createdAt,desc hoặc previewCount,desc") @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sortObj);

        PageResponse<AdminShareItemResponse> response = adminShareService.listShares(
                shareType, status, keyword, dealerId, fileId, from, to, pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Xem thống kê tổng quan các lượt chia sẻ (ADMIN)",
            description = "Trả về số liệu thống kê tổng hợp số lượt share, trạng thái, lượt xem, tải xuống, active clients.")
    public ResponseEntity<AdminShareStatsResponse> getStatistics() {
        return ResponseEntity.ok(adminShareService.getStatistics());
    }

    @GetMapping("/activity-history")
    @Operation(summary = "Xem lịch sử hoạt động chia sẻ file SVG toàn hệ thống (ADMIN)",
            description = "Tra cứu toàn bộ log sự kiện liên quan tới chia sẻ file: Tạo share, Verify, Preview, Download, Revoke.")
    public ResponseEntity<PageResponse<AdminShareActivityResponse>> listActivityHistory(
            @Parameter(description = "Lọc theo Share ID") @RequestParam(required = false) String shareId,
            @Parameter(description = "Lọc theo File ID") @RequestParam(required = false) Long fileId,
            @Parameter(description = "Lọc theo hành động: ALL, CREATE, VERIFY, PREVIEW, DOWNLOAD, REVOKE, EXPIRE") @RequestParam(required = false) String action,
            @Parameter(description = "Lọc theo kết quả: ALL, SUCCESS, FAILED") @RequestParam(required = false) String result,
            @Parameter(description = "Từ khoá tìm kiếm") @RequestParam(required = false) String keyword,
            @Parameter(description = "Lọc từ thời điểm (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Lọc đến thời điểm (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "Số trang (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước trang") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sắp xếp, ví dụ: timestamp,desc") @RequestParam(defaultValue = "timestamp,desc") String sort
    ) {
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sortObj);

        PageResponse<AdminShareActivityResponse> response = adminShareService.listActivityHistory(
                shareId, fileId, action, result, keyword, from, to, pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shareId}")
    @Operation(summary = "Xem chi tiết một lượt chia sẻ file SVG (ADMIN)",
            description = "ADMIN xem chi tiết thông số, trạng thái và thống kê truy cập của lượt chia sẻ.")
    public ResponseEntity<AdminShareItemResponse> getShareDetail(@PathVariable String shareId) {
        return ResponseEntity.ok(adminShareService.getShareDetail(shareId));
    }

    @GetMapping("/{shareId}/history")
    @Operation(summary = "Xem lịch sử hoạt động của một lượt chia sẻ cụ thể (ADMIN)",
            description = "Trả về danh sách timeline các lần xác thực, preview, download, thu hồi của Share ID này.")
    public ResponseEntity<List<AdminShareActivityResponse>> getShareHistory(@PathVariable String shareId) {
        return ResponseEntity.ok(adminShareService.getShareHistory(shareId));
    }

    @PostMapping("/{shareId}/revoke")
    @Operation(summary = "ADMIN thu hồi lượt chia sẻ file SVG",
            description = "ADMIN có quyền thu hồi bất kỳ lượt chia sẻ nào (cả Admin Share và Client Share). Vô hiệu hoá tức thì quyền truy cập và token đã cấp.")
    public ResponseEntity<AdminShareItemResponse> revokeShare(
            @PathVariable String shareId,
            @RequestBody(required = false) AdminRevokeShareRequest request,
            HttpServletRequest httpRequest
    ) {
        User currentUser = currentUserService.getCurrentUser();
        AdminShareItemResponse response = adminShareService.revokeShare(currentUser, shareId, request, httpRequest);
        return ResponseEntity.ok(response);
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
