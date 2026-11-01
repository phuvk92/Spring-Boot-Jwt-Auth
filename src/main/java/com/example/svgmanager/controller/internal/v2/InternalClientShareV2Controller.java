package com.example.svgmanager.controller.internal.v2;

import com.example.svgmanager.dto.internal.v2.ClientShareResponse;
import com.example.svgmanager.dto.internal.v2.CreateClientShareRequest;
import com.example.svgmanager.dto.internal.v2.RevokeClientShareRequest;
import com.example.svgmanager.dto.internal.v2.VerifyClientShareRequest;
import com.example.svgmanager.dto.internal.v2.VerifyClientShareResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.UnauthorizedException;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.internal.v2.ClientSvgShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
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
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/internal/v2/shares")
@Tag(name = "Client File Sharing V2", description = "API quản lý chia sẻ và thu hồi (Unshare) file SVG giữa các Client bằng Share ID và Password")
public class InternalClientShareV2Controller {

    private final ClientSvgShareService clientSvgShareService;
    private final CurrentUserService currentUserService;

    public InternalClientShareV2Controller(
            ClientSvgShareService clientSvgShareService,
            CurrentUserService currentUserService
    ) {
        this.clientSvgShareService = clientSvgShareService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Client A tạo lượt chia sẻ file SVG cho Client B bằng Share ID và mật khẩu",
            description = "Tạo một mã chia sẻ mới (Share ID theo định dạng ddMMyyyy+6 ký tự random) và mật khẩu do Client A tự đặt. Mật khẩu không trả về trong response và không lưu plaintext.")
    public ResponseEntity<ClientShareResponse> createShare(
            @Valid @RequestBody CreateClientShareRequest request,
            HttpServletRequest httpRequest
    ) {
        User currentUser = currentUserService.getCurrentUser();
        ClientShareResponse response = clientSvgShareService.createShare(currentUser, request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Xem danh sách các lượt chia sẻ do Client A tạo",
            description = "Trả về danh sách lượt chia sẻ phân trang của Client A (ADMIN có thể xem toàn bộ hệ thống). Hỗ trợ lọc theo trạng thái, fileId, khoảng thời gian (from, to) hoặc từ khoá.")
    public ResponseEntity<PageResponse<ClientShareResponse>> listShares(
            @Parameter(description = "Trạng thái (ACTIVE, REVOKED, EXPIRED)") @RequestParam(required = false) String status,
            @Parameter(description = "Lọc theo ID file SVG V2") @RequestParam(required = false) Long fileId,
            @Parameter(description = "Từ khoá tìm kiếm (tên file, shareId, ghi chú)") @RequestParam(required = false) String keyword,
            @Parameter(description = "Từ ngày tạo (ISO Date Time: yyyy-MM-ddTHH:mm:ss)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Đến ngày tạo (ISO Date Time: yyyy-MM-ddTHH:mm:ss)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest
    ) {
        User currentUser = currentUserService.getCurrentUser();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<ClientShareResponse> response = clientSvgShareService.listShares(currentUser, status, fileId, keyword, from, to, pageable, httpRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shareId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Xem thông tin chi tiết một lượt chia sẻ",
            description = "Chỉ người tạo lượt chia sẻ (Owner) hoặc ADMIN mới có quyền xem thông tin chi tiết.")
    public ResponseEntity<ClientShareResponse> getShareDetail(
            @PathVariable String shareId,
            HttpServletRequest httpRequest
    ) {
        User currentUser = currentUserService.getCurrentUser();
        ClientShareResponse response = clientSvgShareService.getShare(currentUser, shareId, httpRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{shareId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Thu hồi lượt chia sẻ (Unshare / Revoke)",
            description = "Chuyển trạng thái lượt chia sẻ sang REVOKED, vô hiệu hoá ngay lập tức Share ID và mọi token đã cấp trước đó. File gốc không bị xoá. Thao tác an toàn khi gọi lặp lại.")
    public ResponseEntity<ClientShareResponse> revokeShare(
            @PathVariable String shareId,
            @RequestBody(required = false) RevokeClientShareRequest request,
            @RequestParam(value = "reason", required = false) String queryReason,
            HttpServletRequest httpRequest
    ) {
        User currentUser = currentUserService.getCurrentUser();
        String reason = (request != null && request.getReason() != null && !request.getReason().isBlank())
                ? request.getReason()
                : queryReason;

        ClientShareResponse response = clientSvgShareService.revokeShare(currentUser, shareId, reason, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{shareId}/verify")
    @Operation(summary = "Client B xác thực mật khẩu truy cập lượt chia sẻ",
            description = "Client B gửi mật khẩu để nhận Share Access Token ngắn hạn dùng để preview và download. Nếu lượt chia sẻ đã bị thu hồi hoặc hết hạn, API sẽ từ chối.")
    public ResponseEntity<VerifyClientShareResponse> verifyShare(
            @PathVariable String shareId,
            @Valid @RequestBody VerifyClientShareRequest request,
            HttpServletRequest httpRequest
    ) {
        VerifyClientShareResponse response = clientSvgShareService.verifyShare(shareId, request.getPassword(), httpRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shareId}/preview")
    @Operation(summary = "Xem trước file SVG đã chia sẻ (Inline SVG stream)",
            description = "Xem file SVG trực tuyến bằng Share Token. Hệ thống kiểm tra trực tiếp trạng thái trong DB để đảm bảo chặn truy cập ngay khi share bị thu hồi.")
    public ResponseEntity<byte[]> previewSharedFile(
            @PathVariable String shareId,
            @RequestHeader(value = "X-Share-Token", required = false) String headerToken,
            @RequestParam(value = "token", required = false) String queryToken,
            @RequestParam(value = "shareToken", required = false) String queryShareToken,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            HttpServletRequest httpRequest
    ) {
        String token = resolveShareToken(headerToken, queryToken, queryShareToken, authHeader);
        if (token == null) {
            throw new UnauthorizedException("Thiếu mã xác thực chia sẻ (Header X-Share-Token hoặc tham số ?token=...)");
        }

        byte[] svgBytes = clientSvgShareService.previewShare(shareId, token, httpRequest);
        String filename = clientSvgShareService.getShareFilename(shareId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("image/svg+xml;charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.inline().filename(filename, StandardCharsets.UTF_8).build());
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("Cache-Control", "private, no-store, no-cache, must-revalidate");

        return ResponseEntity.ok().headers(headers).body(svgBytes);
    }

    @GetMapping("/{shareId}/download")
    @Operation(summary = "Tải file SVG đã chia sẻ về máy",
            description = "Tải file SVG đã giải mã on-the-fly bằng Share Token. Tăng biến đếm download_count. Chặn tải ngay nếu lượt chia sẻ bị thu hồi.")
    public ResponseEntity<Resource> downloadSharedFile(
            @PathVariable String shareId,
            @RequestHeader(value = "X-Share-Token", required = false) String headerToken,
            @RequestParam(value = "token", required = false) String queryToken,
            @RequestParam(value = "shareToken", required = false) String queryShareToken,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            HttpServletRequest httpRequest
    ) {
        String token = resolveShareToken(headerToken, queryToken, queryShareToken, authHeader);
        if (token == null) {
            throw new UnauthorizedException("Thiếu mã xác thực chia sẻ (Header X-Share-Token hoặc tham số ?token=...)");
        }

        byte[] svgBytes = clientSvgShareService.downloadShare(shareId, token, httpRequest);
        String filename = clientSvgShareService.getShareFilename(shareId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("image/svg+xml;charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("Cache-Control", "private, no-store, no-cache, must-revalidate");

        return ResponseEntity.ok().headers(headers).body(new ByteArrayResource(svgBytes));
    }

    private String resolveShareToken(String headerToken, String queryToken, String queryShareToken, String authHeader) {
        if (headerToken != null && !headerToken.isBlank()) {
            return headerToken.trim();
        }
        if (queryToken != null && !queryToken.isBlank()) {
            return queryToken.trim();
        }
        if (queryShareToken != null && !queryShareToken.isBlank()) {
            return queryShareToken.trim();
        }
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authHeader.substring(7).trim();
        }
        return null;
    }
}
