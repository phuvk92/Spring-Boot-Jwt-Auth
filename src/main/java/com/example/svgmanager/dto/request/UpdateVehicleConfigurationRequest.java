package com.example.svgmanager.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateVehicleConfigurationRequest(
        Long categoryId,
        String productGroup,
        @NotNull(message = "Hãng xe không được để trống")
        Long brandId,
        @NotNull(message = "Dòng xe không được để trống")
        Long modelId,
        @NotNull(message = "Năm sản xuất từ không được để trống")
        @Min(value = 1900, message = "Năm sản xuất từ không hợp lệ (>= 1900)")
        @Max(value = 2100, message = "Năm sản xuất từ không hợp lệ (<= 2100)")
        Integer yearFrom,
        @NotNull(message = "Năm sản xuất đến không được để trống")
        @Min(value = 1900, message = "Năm sản xuất đến không hợp lệ (>= 1900)")
        @Max(value = 2100, message = "Năm sản xuất đến không hợp lệ (<= 2100)")
        Integer yearTo,
        @NotBlank(message = "Mã khung / Mã đời không được để trống")
        String generationCode,
        String status
) {}
