package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Khổ cắt khai theo part file (epic NGO-399). {@code null} trên file nghĩa là
 * chưa khai — client dùng mặc định 15000 × 700 (board 02/10).
 */
@Schema(description = "Khổ cắt của file, mm; null khi file chưa khai (dữ liệu cũ)")
public record CutAreaDto(

        @Schema(description = "Chiều dọc cuộn, trục X — mm (100–50000)", example = "15000")
        Integer lengthMm,

        @Schema(description = "Khổ phim, trục Y — mm (100–2000)", example = "700")
        Integer widthMm
) {
    /** null khi file chưa khai khổ — giữ trên dây là null chứ không bơm mặc định. */
    public static CutAreaDto of(Integer lengthMm, Integer widthMm) {
        if (lengthMm == null || widthMm == null) {
            return null;
        }
        return new CutAreaDto(lengthMm, widthMm);
    }
}
