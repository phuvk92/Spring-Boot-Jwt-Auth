package com.example.svgmanager.dto.response;

import java.time.LocalDateTime;

public record CarModelResponse(
        Long id,
        Long brandId,
        String brandName,
        String code,
        String name,
        String status,
        Integer displayOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
