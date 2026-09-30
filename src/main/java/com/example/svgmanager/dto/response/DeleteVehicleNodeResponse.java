package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Kết quả xoá nhánh xe — không chặn kể cả khi còn file (Q4)")
public record DeleteVehicleNodeResponse(

        @Schema(description = "Số node đã xoá (tính cả node gốc của nhánh)", example = "5")
        long deletedNodes,

        @Schema(description = "Số file đã mất liên kết mẫu xe", example = "12")
        long unlinkedFiles
) {
}
