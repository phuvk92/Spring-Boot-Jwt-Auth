package com.example.svgmanager.dto.response;

import com.example.svgmanager.entity.DealerCutQuota;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Thông tin Quota lượt Cut của đại lý")
public class DealerCutQuotaResponse {

    private Long id;
    private Long dealerId;
    private String dealerName;
    private String dealerCode;
    private Integer allocatedCuts;
    private Integer consumedCuts;
    private Integer remainingCuts;
    private Integer successCuts;
    private Integer failedCuts;
    private Integer refundedCuts;
    private Integer adjustedCuts;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public DealerCutQuotaResponse() {
    }

    public static DealerCutQuotaResponse fromEntity(DealerCutQuota quota) {
        if (quota == null) return null;
        DealerCutQuotaResponse res = new DealerCutQuotaResponse();
        res.setId(quota.getId());
        if (quota.getDealer() != null) {
            res.setDealerId(quota.getDealer().getId());
            res.setDealerName(quota.getDealer().getName());
            res.setDealerCode(quota.getDealer().getCode());
        }
        res.setAllocatedCuts(quota.getAllocatedCuts());
        res.setConsumedCuts(quota.getConsumedCuts());
        res.setRemainingCuts(quota.getRemainingCuts());
        res.setSuccessCuts(quota.getSuccessCuts());
        res.setFailedCuts(quota.getFailedCuts());
        res.setRefundedCuts(quota.getRefundedCuts());
        res.setAdjustedCuts(quota.getAdjustedCuts());
        res.setUpdatedAt(quota.getUpdatedAt());
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDealerId() { return dealerId; }
    public void setDealerId(Long dealerId) { this.dealerId = dealerId; }
    public String getDealerName() { return dealerName; }
    public void setDealerName(String dealerName) { this.dealerName = dealerName; }
    public String getDealerCode() { return dealerCode; }
    public void setDealerCode(String dealerCode) { this.dealerCode = dealerCode; }
    public Integer getAllocatedCuts() { return allocatedCuts; }
    public void setAllocatedCuts(Integer allocatedCuts) { this.allocatedCuts = allocatedCuts; }
    public Integer getConsumedCuts() { return consumedCuts; }
    public void setConsumedCuts(Integer consumedCuts) { this.consumedCuts = consumedCuts; }
    public Integer getRemainingCuts() { return remainingCuts; }
    public void setRemainingCuts(Integer remainingCuts) { this.remainingCuts = remainingCuts; }
    public Integer getSuccessCuts() { return successCuts; }
    public void setSuccessCuts(Integer successCuts) { this.successCuts = successCuts; }
    public Integer getFailedCuts() { return failedCuts; }
    public void setFailedCuts(Integer failedCuts) { this.failedCuts = failedCuts; }
    public Integer getRefundedCuts() { return refundedCuts; }
    public void setRefundedCuts(Integer refundedCuts) { this.refundedCuts = refundedCuts; }
    public Integer getAdjustedCuts() { return adjustedCuts; }
    public void setAdjustedCuts(Integer adjustedCuts) { this.adjustedCuts = adjustedCuts; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
