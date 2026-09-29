package com.example.svgmanager.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Đổi tên node — không đổi cha/cấp (đúng design)")
public record UpdateVehicleNodeRequest(

        @Schema(description = "Tên mới", example = "Camry 2.5Q 2024", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Tên node không được để trống")
        @Size(max = 255, message = "Tên node tối đa 255 ký tự")
        String name
) {
}
