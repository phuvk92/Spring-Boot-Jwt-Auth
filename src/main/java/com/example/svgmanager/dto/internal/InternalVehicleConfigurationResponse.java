package com.example.svgmanager.dto.internal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Vehicle configuration details mapped to SVG file")
public record InternalVehicleConfigurationResponse(
        @Schema(description = "ID cấu hình xe", example = "10001")
        Long id,

        @Schema(description = "Mã nhóm sản phẩm", example = "PPF_EXTERIOR")
        String productGroup,

        @Schema(description = "Tên nhóm sản phẩm hiển thị", example = "PPF Exterior")
        String productGroupName,

        @Schema(description = "Tên hãng xe", example = "BMW")
        String brandName,

        @Schema(description = "Tên dòng xe", example = "X5")
        String modelName,

        @Schema(description = "Năm sản xuất từ", example = "2019")
        Integer yearFrom,

        @Schema(description = "Năm sản xuất đến", example = "2023")
        Integer yearTo,

        @Schema(description = "Mã khung / Mã đời", example = "G05")
        String generationCode
) {}
