package com.example.svgmanager.dto.response;

import java.time.LocalDateTime;

public record CarBrandResponse(
        Long id,
        String code,
        String name,
        String status,
        Integer displayOrder,
        int modelsCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
