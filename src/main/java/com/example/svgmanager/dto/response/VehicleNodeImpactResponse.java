package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Số liệu cho hộp xác nhận trước khi xoá nhánh xe")
public record VehicleNodeImpactResponse(

        @Schema(description = "Số node con sẽ bị xoá theo (không tính chính node)", example = "4")
        long nodes,

        @Schema(description = "Số file sẽ mất liên kết mẫu xe (file vẫn còn trong kho — Q4)", example = "12")
        long files
) {
}
