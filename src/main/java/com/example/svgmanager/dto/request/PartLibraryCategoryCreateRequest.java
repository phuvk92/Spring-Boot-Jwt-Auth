package com.example.svgmanager.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Yêu cầu tạo mới Danh mục kho mẫu & part")
public record PartLibraryCategoryCreateRequest(
        @NotBlank(message = "Mã danh mục không được để trống")
        @Size(max = 100, message = "Mã danh mục tối đa 100 ký tự")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Mã danh mục chỉ chứa chữ cái, chữ số, dấu gạch dưới hoặc gạch ngang")
        @Schema(description = "Mã danh mục duy nhất", example = "INTERIOR")
        String code,

        @NotBlank(message = "Tên danh mục không được để trống")
        @Size(max = 255, message = "Tên danh mục tối đa 255 ký tự")
        @Schema(description = "Tên hiển thị của danh mục", example = "Nội thất")
        String name,

        @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Trạng thái chỉ có thể là ACTIVE hoặc INACTIVE")
        @Schema(description = "Trạng thái danh mục (ACTIVE hoặc INACTIVE)", example = "ACTIVE", defaultValue = "ACTIVE")
        String status
) {
}
