package com.example.svgmanager.dto.response;

import java.time.LocalDateTime;

public record SvgFileDealerPermissionResponse(
        Long id,
        Long dealerId,
        String dealerCode,
        String dealerName,
        boolean canView,
        boolean canDownload,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
