package com.example.svgmanager.controller.internal;

import com.example.svgmanager.dto.internal.InternalSvgDetailResponse;
import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.service.InternalSvgService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/internal/svg-files")
@Tag(name = "Internal SVG Files", description = "Endpoints truy vấn chi tiết và tải file SVG dành cho Client máy cắt")
public class InternalSvgFileController {

    private final InternalSvgService internalSvgService;

    public InternalSvgFileController(InternalSvgService internalSvgService) {
        this.internalSvgService = internalSvgService;
    }

    @GetMapping("/{svgFileId}")
    @Operation(summary = "Xem chi tiết file SVG", description = "Truy xuất metadata, cấu hình xe và quyền xem/tải của người dùng đối với file SVG", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trả về thông tin chi tiết file SVG"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token không hợp lệ", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "File SVG không tồn tại hoặc đại lý không có quyền xem", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<InternalSvgDetailResponse> getSvgDetail(
            @PathVariable Long svgFileId,
            @Parameter(description = "Thông tin thiết bị (Thiết bị)") @RequestParam(required = false) String device,
            @Parameter(description = "Địa chỉ IP client (Địa chỉ IP)") @RequestParam(required = false) String ipAddress,
            HttpServletRequest request
    ) {
        String effectiveDevice = resolveDevice(device, request);
        String effectiveIp = resolveIp(ipAddress, request);
        InternalSvgDetailResponse response = internalSvgService.getSvgDetail(svgFileId, effectiveDevice, effectiveIp);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{svgFileId}/download")
    @Operation(summary = "Tải file SVG nhị phân", description = "Tải nội dung file SVG nếu đại lý của người dùng được cấp quyền DOWNLOAD", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tải file SVG thành công (binary stream)"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token không hợp lệ", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Đại lý chỉ có quyền xem, không có quyền tải file", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "File SVG không tồn tại hoặc đại lý không có quyền xem", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Resource> downloadSvg(
            @PathVariable Long svgFileId,
            @Parameter(description = "Thông tin thiết bị (Thiết bị)") @RequestParam(required = false) String device,
            @Parameter(description = "Địa chỉ IP client (Địa chỉ IP)") @RequestParam(required = false) String ipAddress,
            HttpServletRequest request
    ) {
        String effectiveDevice = resolveDevice(device, request);
        String effectiveIp = resolveIp(ipAddress, request);
        Resource resource = internalSvgService.downloadSvg(svgFileId, effectiveDevice, effectiveIp);
        String filename = internalSvgService.getOriginalFilename(svgFileId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("image/svg+xml"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());

        return ResponseEntity.ok().headers(headers).body(resource);
    }

    private String resolveDevice(String device, HttpServletRequest request) {
        if (StringUtils.hasText(device)) {
            return device.trim();
        }
        if (request != null) {
            String headerDevice = request.getHeader("X-Device");
            if (StringUtils.hasText(headerDevice)) {
                return headerDevice.trim();
            }
        }
        return null;
    }

    private String resolveIp(String ipAddress, HttpServletRequest request) {
        if (StringUtils.hasText(ipAddress)) {
            return ipAddress.trim();
        }
        if (request != null) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(xForwardedFor)) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }
}
