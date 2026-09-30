package com.example.svgmanager.controller;

import com.example.svgmanager.dto.response.BrandingResponse;
import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.service.BrandingService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/v1/branding — thương hiệu pha 2 của đại lý mà user thuộc về (RB-08, BR-10).
 * Hợp đồng: Pcut-Client/contracts/openapi.yaml. Luôn 200 khi có phiên hợp lệ — lỗi
 * thương hiệu không được chặn đăng nhập (BR-13); phiên hỏng thì DeviceSessionFilter
 * đã trả 401 SESSION_REVOKED trước khi tới đây.
 */
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Branding", description = "Thương hiệu đại lý cho app cắt — hợp đồng /api/v1/branding")
public class BrandingController {

    private final BrandingService brandingService;

    public BrandingController(BrandingService brandingService) {
        this.brandingService = brandingService;
    }

    @GetMapping("/api/v1/branding")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Thương hiệu của đại lý mà user thuộc về (RB-08 pha 2)",
            description = "Trả bản mặc định Pcut (logoPng = null) khi user độc lập hoặc đại lý chưa cấu hình — không bao giờ 404/500 vì thiếu cấu hình (BR-30, BR-32).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Setting thương hiệu",
                    content = @Content(schema = @Schema(implementation = BrandingResponse.class))),
            @ApiResponse(responseCode = "401", description = "Không có phiên còn hiệu lực (SESSION_REVOKED)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BrandingResponse> getBranding() {
        return ResponseEntity.ok(brandingService.getBrandingForCurrentUser());
    }
}
