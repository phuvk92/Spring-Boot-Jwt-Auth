package com.example.svgmanager.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Thông tin Danh mục kho mẫu & part")
public record PartLibraryCategoryResponse(
        @Schema(description = "ID danh mục", example = "1")
        Long id,

        @Schema(description = "Mã danh mục", example = "INTERIOR")
        String code,

        @Schema(description = "Tên danh mục", example = "Nội thất")
        String name,

        @Schema(description = "Trạng thái", example = "ACTIVE")
        String status,

        @Schema(description = "Thời gian tạo")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,

        @Schema(description = "Thời gian cập nhật")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt,

        @Schema(description = "Người tạo", example = "admin")
        String createdBy,

        @Schema(description = "Người cập nhật", example = "admin")
        String updatedBy,

        @Schema(description = "Số lượng part file đang gắn vào danh mục này", example = "15")
        long usageCount
) {
    public PartLibraryCategoryResponse(Long id, String code, String name, String status,
                                       LocalDateTime createdAt, LocalDateTime updatedAt,
                                       String createdBy, String updatedBy) {
        this(id, code, name, status, createdAt, updatedAt, createdBy, updatedBy, 0L);
    }
}
