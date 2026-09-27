package com.example.svgmanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCarModelRequest(
        @NotNull(message = "Hãng xe không được để trống")
        Long brandId,
        @NotBlank(message = "Mã dòng xe không được để trống")
        String code,
        @NotBlank(message = "Tên dòng xe không được để trống")
        String name,
        Integer displayOrder,
        String status
) {}
