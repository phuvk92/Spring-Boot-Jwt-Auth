package com.example.svgmanager.controller.internal;

import com.example.svgmanager.dto.internal.InternalChangePasswordRequest;
import com.example.svgmanager.dto.internal.InternalChangePasswordResponse;
import com.example.svgmanager.dto.internal.InternalLoginRequest;
import com.example.svgmanager.dto.internal.InternalLoginResponse;
import com.example.svgmanager.dto.internal.InternalLogoutRequest;
import com.example.svgmanager.dto.internal.InternalLogoutResponse;
import com.example.svgmanager.dto.internal.InternalRefreshTokenRequest;
import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.service.InternalAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/auth")
@Tag(name = "Internal Authentication", description = "Endpoints authentication, token refresh, logout và đổi mật khẩu dành riêng cho ứng dụng Client máy cắt")
public class InternalAuthController {

    private final InternalAuthService internalAuthService;

    public InternalAuthController(InternalAuthService internalAuthService) {
        this.internalAuthService = internalAuthService;
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập Client máy cắt", description = "Xác thực tài khoản người dùng qua Keycloak kết hợp thông tin đại lý từ database")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công, trả về access token, refresh token và thông tin user"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Sai tài khoản, mật khẩu hoặc tài khoản bị vô hiệu hóa", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<InternalLoginResponse> login(@Valid @RequestBody InternalLoginRequest request) {
        InternalLoginResponse response = internalAuthService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = {"/refresh-token", "/refresh"})
    @Operation(summary = "Làm mới JWT access token cho máy cắt", description = "Đổi refresh token lấy access token mới từ Keycloak và cập nhật thông tin user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Làm mới token thành công, trả về access token và refresh token mới"),
            @ApiResponse(responseCode = "400", description = "Refresh token trống hoặc không hợp lệ", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token hết hạn hoặc tài khoản bị vô hiệu hóa", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<InternalLoginResponse> refreshToken(@Valid @RequestBody InternalRefreshTokenRequest request) {
        InternalLoginResponse response = internalAuthService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất tài khoản máy cắt", description = "Thu hồi Refresh Token và chấm dứt phiên đăng nhập trên Keycloak")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đăng xuất thành công"),
            @ApiResponse(responseCode = "400", description = "Không có thông tin phiên làm việc hoặc refresh token", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<InternalLogoutResponse> logout(@RequestBody(required = false) InternalLogoutRequest request) {
        InternalLogoutResponse response = internalAuthService.logout(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Đổi mật khẩu người dùng hiện tại", description = "Xác thực mật khẩu cũ và cập nhật mật khẩu mới đồng bộ với Keycloak", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đổi mật khẩu thành công"),
            @ApiResponse(responseCode = "400", description = "Mật khẩu hiện tại sai hoặc mật khẩu mới không hợp lệ", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token không hợp lệ", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<InternalChangePasswordResponse> changePassword(@Valid @RequestBody InternalChangePasswordRequest request) {
        InternalChangePasswordResponse response = internalAuthService.changePassword(request);
        return ResponseEntity.ok(response);
    }
}
