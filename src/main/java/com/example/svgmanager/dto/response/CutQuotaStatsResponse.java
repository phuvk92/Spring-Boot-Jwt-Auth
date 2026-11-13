package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Thống kê tổng hợp Quota lượt Cut")
public class CutQuotaStatsResponse {

    private long totalAllocatedCuts;
    private long totalConsumedCuts;
    private long totalRemainingCuts;
    private long totalSuccessCuts;
    private long totalFailedCuts;
    private long totalRefundedCuts;
    private long totalAdjustedCuts;
    private long totalDealersWithQuota;

    public CutQuotaStatsResponse() {
    }

    public CutQuotaStatsResponse(long totalAllocatedCuts, long totalConsumedCuts, long totalRemainingCuts,
                                 long totalSuccessCuts, long totalFailedCuts, long totalRefundedCuts,
                                 long totalAdjustedCuts, long totalDealersWithQuota) {
        this.totalAllocatedCuts = totalAllocatedCuts;
        this.totalConsumedCuts = totalConsumedCuts;
        this.totalRemainingCuts = totalRemainingCuts;
        this.totalSuccessCuts = totalSuccessCuts;
        this.totalFailedCuts = totalFailedCuts;
        this.totalRefundedCuts = totalRefundedCuts;
        this.totalAdjustedCuts = totalAdjustedCuts;
        this.totalDealersWithQuota = totalDealersWithQuota;
    }

    public long getTotalAllocatedCuts() { return totalAllocatedCuts; }
    public void setTotalAllocatedCuts(long totalAllocatedCuts) { this.totalAllocatedCuts = totalAllocatedCuts; }
    public long getTotalConsumedCuts() { return totalConsumedCuts; }
    public void setTotalConsumedCuts(long totalConsumedCuts) { this.totalConsumedCuts = totalConsumedCuts; }
    public long getTotalRemainingCuts() { return totalRemainingCuts; }
    public void setTotalRemainingCuts(long totalRemainingCuts) { this.totalRemainingCuts = totalRemainingCuts; }
    public long getTotalSuccessCuts() { return totalSuccessCuts; }
    public void setTotalSuccessCuts(long totalSuccessCuts) { this.totalSuccessCuts = totalSuccessCuts; }
    public long getTotalFailedCuts() { return totalFailedCuts; }
    public void setTotalFailedCuts(long totalFailedCuts) { this.totalFailedCuts = totalFailedCuts; }
    public long getTotalRefundedCuts() { return totalRefundedCuts; }
    public void setTotalRefundedCuts(long totalRefundedCuts) { this.totalRefundedCuts = totalRefundedCuts; }
    public long getTotalAdjustedCuts() { return totalAdjustedCuts; }
    public void setTotalAdjustedCuts(long totalAdjustedCuts) { this.totalAdjustedCuts = totalAdjustedCuts; }
    public long getTotalDealersWithQuota() { return totalDealersWithQuota; }
    public void setTotalDealersWithQuota(long totalDealersWithQuota) { this.totalDealersWithQuota = totalDealersWithQuota; }
}
