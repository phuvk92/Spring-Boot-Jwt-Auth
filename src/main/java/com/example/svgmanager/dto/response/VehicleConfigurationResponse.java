package com.example.svgmanager.dto.response;

import java.time.LocalDateTime;

public record VehicleConfigurationResponse(
        Long id,
        CategoryRef category,
        String productGroup,
        String productGroupName,
        BrandRef brand,
        ModelRef model,
        Integer yearFrom,
        Integer yearTo,
        String generationCode,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record CategoryRef(Long id, String name) {}
    public record BrandRef(Long id, String code, String name) {}
    public record ModelRef(Long id, String code, String name) {}
}
