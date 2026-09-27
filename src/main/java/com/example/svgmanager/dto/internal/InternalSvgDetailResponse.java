package com.example.svgmanager.dto.internal;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Detailed information of SVG file for cutting machine client")
public record InternalSvgDetailResponse(
        @Schema(description = "ID của file SVG", example = "501")
        Long id,

        @Schema(description = "Tên file gốc", example = "BMW_X5_G05_SIDE_SKIRT.svg")
        String fileName,

        @Schema(description = "Dung lượng file tính bằng bytes", example = "1258291")
        Long fileSize,

        @Schema(description = "MIME Type", example = "image/svg+xml")
        String mimeType,

        @Schema(description = "Mã băm SHA-256 kiểm tra tính toàn vẹn", example = "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        String checksum,

        @Schema(description = "Trạng thái file", example = "ACTIVE")
        String status,

        @Schema(description = "Danh sách cấu hình xe áp dụng cho file SVG này")
        List<InternalVehicleConfigurationResponse> vehicleConfigurations,

        @Schema(description = "Quyền truy cập của người dùng hiện tại")
        InternalSvgPermissionResponse permission,

        @Schema(description = "Thời gian cập nhật gần nhất", example = "2026-09-27T16:00:00+07:00")
        String updatedAt
) {}
