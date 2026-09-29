package com.example.svgmanager.controller;

import com.example.svgmanager.dto.request.CreateVehicleConfigurationRequest;
import com.example.svgmanager.dto.request.UpdateVehicleConfigurationRequest;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.VehicleConfigurationResponse;
import com.example.svgmanager.service.VehicleConfigurationService;
import com.example.svgmanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vehicle-configurations")
@Tag(name = "Vehicle Configurations", description = "Quản lý cấu hình xe (ADMIN)")
@PreAuthorize("hasRole('ADMIN')")
public class VehicleConfigurationController {

    private final VehicleConfigurationService configurationService;

    public VehicleConfigurationController(VehicleConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách cấu hình xe (phân trang, lọc theo nhóm, hãng, dòng, năm, trạng thái, tìm kiếm)")
    public ResponseEntity<PageResponse<VehicleConfigurationResponse>> getConfigurations(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String productGroup,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long modelId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        PageResponse<VehicleConfigurationResponse> response = configurationService.getConfigurations(
                categoryId, productGroup, brandId, modelId, year, status, search, page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id:[0-9]+}")
    @Operation(summary = "Chi tiết một cấu hình xe theo ID")
    public ResponseEntity<VehicleConfigurationResponse> getConfigurationById(@PathVariable Long id) {
        VehicleConfigurationResponse response = configurationService.getConfigurationById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Tạo mới một cấu hình xe")
    public ResponseEntity<VehicleConfigurationResponse> createConfiguration(
            @Valid @RequestBody CreateVehicleConfigurationRequest request
    ) {
        String actor = SecurityUtils.getCurrentUsername();
        String role = SecurityUtils.isAdmin() ? "ADMIN" : "USER";
        VehicleConfigurationResponse response = configurationService.createConfiguration(request, actor, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id:[0-9]+}")
    @Operation(summary = "Cập nhật thông tin cấu hình xe")
    public ResponseEntity<VehicleConfigurationResponse> updateConfiguration(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleConfigurationRequest request
    ) {
        String actor = SecurityUtils.getCurrentUsername();
        String role = SecurityUtils.isAdmin() ? "ADMIN" : "USER";
        VehicleConfigurationResponse response = configurationService.updateConfiguration(id, request, actor, role);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id:[0-9]+}")
    @Operation(summary = "Xóa (soft delete) cấu hình xe")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable Long id) {
        String actor = SecurityUtils.getCurrentUsername();
        String role = SecurityUtils.isAdmin() ? "ADMIN" : "USER";
        configurationService.deleteConfiguration(id, actor, role);
        return ResponseEntity.noContent().build();
    }
}
