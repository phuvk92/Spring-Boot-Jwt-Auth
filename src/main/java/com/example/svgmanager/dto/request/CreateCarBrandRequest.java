package com.example.svgmanager.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCarBrandRequest(
        @NotBlank(message = "Mã hãng xe không được để trống")
        String code,
        @NotBlank(message = "Tên hãng xe không được để trống")
        String name,
        Integer displayOrder,
        String status
) {}
