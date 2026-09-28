package com.example.svgmanager.controller.internal;

import com.example.svgmanager.dto.response.UserDeviceResponse;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.UserDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/internal/devices")
@Tag(name = "Internal Devices", description = "Máy đã đăng ký của người dùng hiện tại (F-57) — cho màn Thông tin cá nhân của app cắt")
public class InternalDeviceController {

    private final UserDeviceService userDeviceService;
    private final CurrentUserService currentUserService;

    public InternalDeviceController(UserDeviceService userDeviceService, CurrentUserService currentUserService) {
        this.userDeviceService = userDeviceService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @Operation(summary = "Máy đã đăng ký của tôi",
            description = "Đánh dấu máy đang gọi bằng current=true. Thợ không tự gỡ máy được (Q2 chốt 28/09) — liên hệ quản trị đại lý.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<UserDeviceResponse>> myDevices() {
        String sessionId = currentUserService.getCurrentJwt().map(jwt -> jwt.getClaimAsString("sid")).orElse(null);
        return ResponseEntity.ok(userDeviceService.listDevices(currentUserService.getCurrentUser(), sessionId));
    }
}
