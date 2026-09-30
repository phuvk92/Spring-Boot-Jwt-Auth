package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Thân trả của GET /api/v1/cuts — schema CutHistory trong openapi v0.3.0.
 * Rỗng (chưa cắt lần nào / token không gắn máy) là trạng thái thường: stats về 0/null,
 * jobs là mảng rỗng — không phải lỗi, không 404.
 */
@Schema(description = "Lịch sử cắt trên máy này — chỉ số liệu (F-38 · KX-03)")
public class CutHistoryResponse {

    private CutStatsResponse stats;

    private List<CutJobResponse> jobs;

    public CutHistoryResponse() {
    }

    public CutHistoryResponse(CutStatsResponse stats, List<CutJobResponse> jobs) {
        this.stats = stats;
        this.jobs = jobs;
    }

    public CutStatsResponse getStats() { return stats; }
    public void setStats(CutStatsResponse stats) { this.stats = stats; }
    public List<CutJobResponse> getJobs() { return jobs; }
    public void setJobs(List<CutJobResponse> jobs) { this.jobs = jobs; }
}
