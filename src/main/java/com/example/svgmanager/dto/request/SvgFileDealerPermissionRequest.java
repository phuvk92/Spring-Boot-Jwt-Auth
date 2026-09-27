package com.example.svgmanager.dto.request;

import jakarta.validation.constraints.NotNull;

public record SvgFileDealerPermissionRequest(
        @NotNull(message = "dealerId is required")
        Long dealerId,
        boolean canView,
        boolean canDownload
) {
}
