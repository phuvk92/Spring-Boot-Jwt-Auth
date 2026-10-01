package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Một dòng của màn "Kho mẫu & part file" — SA-DanhMucXe-v2 §3.2.
 */
@Schema(description = "File thiết kế trong kho part file (admin)")
public record AdminFileResponse(

        Long id,

        @Schema(example = "audi-q6-2024--a3f9c1b2")
        String fileKey,

        @Schema(example = "Audi Q6 2024 — ngoại thất")
        String name,

        @Schema(example = "Audi Q6 2024.svg")
        String originalFilename,

        @Schema(example = "Ngoại thất")
        String category,

        @Schema(description = "Năm áp dụng; null = mọi năm (Q3)", example = "2024")
        Integer year,

        @Schema(description = "Các mẫu xe file gắn vào (Q5 — một file nhiều mẫu)")
        List<VehicleRef> vehicles,

        @Schema(example = "SYSTEM")
        String source,

        @Schema(example = "170")
        int partCount,

        LocalDateTime updatedAt,

        @Schema(description = "Đường dẫn ảnh xem trước, null khi chưa gắn thumbnail")
        String thumbnailUrl,

        @Schema(description = "Có bản đã xếp (vào vùng cắt) hay không", example = "true")
        boolean hasNested,

        @Schema(description = "Có bản chưa xếp (vào khu chưa cắt) hay không", example = "true")
        boolean hasRaw
) {
    /** Một mẫu xe file gắn vào, kèm đường dẫn đầy đủ trong cây ("Toyota › Camry › Camry 2.5Q"). */
    public record VehicleRef(Long nodeId, String path) {
    }
}
