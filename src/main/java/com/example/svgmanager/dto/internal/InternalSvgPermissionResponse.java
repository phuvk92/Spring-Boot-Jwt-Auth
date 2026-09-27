package com.example.svgmanager.dto.internal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Access permissions for current authenticated user on SVG file")
public record InternalSvgPermissionResponse(
        @Schema(description = "Có quyền xem chi tiết SVG", example = "true")
        boolean canView,

        @Schema(description = "Có quyền tải file SVG", example = "true")
        boolean canDownload
) {}
