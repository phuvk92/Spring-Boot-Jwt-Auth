package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Một node trong cây xe 4 cấp — children chứa cả nhánh con")
public record VehicleNodeResponse(

        @Schema(example = "3")
        Long id,

        @Schema(example = "MODEL")
        String level,

        @Schema(example = "Camry 2.5Q")
        String name,

        @Schema(description = "Số con trực tiếp", example = "2")
        int childCount,

        List<VehicleNodeResponse> children
) {
}
