package com.example.svgmanager.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Tạo nút xe mới — parentId null thì là hãng (BRAND)")
public record CreateVehicleNodeRequest(

        @Schema(description = "Id node cha; null → hãng mới", example = "2")
        Long parentId,

        @Schema(description = "Tên hiển thị của node", example = "Camry 3.5V", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Tên node không được để trống")
        @Size(max = 255, message = "Tên node tối đa 255 ký tự")
        String name
) {
}
