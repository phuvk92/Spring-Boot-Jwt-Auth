package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Khuôn của GET /api/v1/branding — khớp schema {@code Branding} trong
 * Pcut-Client/contracts/openapi.yaml (BR-10). {@code null} ở trường tuỳ chọn nghĩa là
 * đại lý chưa đặt → client dùng mặc định (BR-30, BR-32).
 */
@Schema(description = "Thương hiệu pha 2 của đại lý mà user thuộc về")
public record BrandingResponse(
        @Schema(description = "Tên hiển thị trên app", example = "SG Decal")
        String displayName,

        @Schema(description = "Khẩu hiệu dưới logo", nullable = true)
        String slogan,

        @Schema(description = "Hotline hỗ trợ hiện trong app", nullable = true)
        String hotline,

        @Schema(description = "Một trong 5 màu dựng sẵn", example = "#2563C9",
                allowableValues = {"#2563C9", "#7C3AED", "#2E7D5B", "#C2452D", "#35342F"})
        String primaryColor,

        @Schema(description = "Màu phụ (nút và nhấn mạnh)", nullable = true)
        String secondaryColor,

        @Schema(description = "Theme cho thợ", allowableValues = {"light", "dark", "system"})
        String theme,

        @Schema(description = "Cho thợ đổi giao diện tối/sáng")
        Boolean allowWorkerThemeToggle,

        @Schema(description = "Hiện tên xưởng cạnh logo")
        Boolean showDealerNameNextToLogo,

        @Schema(description = "Chống chụp/quay màn hình cửa sổ có bản vẽ (RB-06) — nguồn có thẩm quyền sau đăng nhập")
        Boolean protectScreenCapture,

        @Schema(description = "Logo PNG nền trong suốt, base64, trần 1MB (BR-12)", nullable = true)
        String logoPng
) {
}
