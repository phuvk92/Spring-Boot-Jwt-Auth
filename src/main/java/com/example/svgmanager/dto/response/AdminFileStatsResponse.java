package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Bốn thẻ thống kê trên màn Kho part file — SA-DanhMucXe-v2 §3.2.
 * {@code unlinked} = file ACTIVE không có liên kết mẫu xe nào (sinh ra tự nhiên
 * khi xoá node xe — Q4).
 */
@Schema(description = "Thẻ thống kê kho part file")
public record AdminFileStatsResponse(
        long total,
        long modelsWithFiles,
        long fromDealers,
        long unlinked
) {
}
