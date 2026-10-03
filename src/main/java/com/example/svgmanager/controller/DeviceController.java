package com.example.svgmanager.controller;

import com.example.svgmanager.dto.response.DeviceStatsResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.SystemDeviceResponse;
import com.example.svgmanager.service.DeviceManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản lý thiết bị toàn hệ thống (F-57 · NGO-422 · SA-GioiHanThietBi §2.6).
 * Phục vụ màn hình "Phiên & thiết bị" trên web admin (admin.pcut.vn/sessions).
 */
@RestController
@RequestMapping("/api/devices")
@Tag(name = "Device Management", description = "Danh sách và thống kê thiết bị toàn hệ thống (F-57 · NGO-422)")
@SecurityRequirement(name = "Bearer Authentication")
public class DeviceController {

    private final DeviceManagementService deviceManagementService;

    public DeviceController(DeviceManagementService deviceManagementService) {
        this.deviceManagementService = deviceManagementService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Danh sách thiết bị toàn hệ thống (lọc, phân trang) — F-57",
            description = "ADMIN thấy tất cả; AGENT chỉ thấy thiết bị của user thuộc đại lý mình. "
                    + "Mặc định sắp lastSeenAt giảm dần. Lọc tuỳ chọn theo từ khóa q, đại lý dealerId, trạng thái status (ACTIVE|REVOKED|ALL).")
    public ResponseEntity<PageResponse<SystemDeviceResponse>> getDevices(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long dealerId,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastSeenAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(deviceManagementService.getDevices(q, dealerId, status, page, size, sortBy, sortDir));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Bốn thẻ thống kê thiết bị đầu trang — F-57",
            description = "Trả về 4 số liệu: activeNow (đang dùng trong 15p), registered (máy ACTIVE), "
                    + "usersAtLimit (user đạt tối đa số máy), staleDevices (máy không thấy > 30 ngày).")
    public ResponseEntity<DeviceStatsResponse> getStats() {
        return ResponseEntity.ok(deviceManagementService.getStats());
    }
}
