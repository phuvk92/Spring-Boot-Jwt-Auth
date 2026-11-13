package com.example.svgmanager.dto.response;

import com.example.svgmanager.entity.CutQuotaLedger;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Bản ghi sổ cái biến động quota lượt cut")
public class CutQuotaLedgerResponse {

    private Long id;
    private Long dealerId;
    private String dealerName;
    private Long userId;
    private String username;
    private String transactionType; // ALLOCATE, CONSUME, REFUND, ADJUST
    private Integer delta;
    private Integer balanceBefore;
    private Integer balanceAfter;
    private Long cutJobId;
    private String idempotencyKey;
    private String requestId;
    private String status;
    private String ipAddress;
    private String notes;
    private String createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public CutQuotaLedgerResponse() {
    }

    public static CutQuotaLedgerResponse fromEntity(CutQuotaLedger l) {
        if (l == null) return null;
        CutQuotaLedgerResponse res = new CutQuotaLedgerResponse();
        res.setId(l.getId());
        res.setDealerId(l.getDealer() != null ? l.getDealer().getId() : null);
        res.setDealerName(l.getDealerName());
        res.setUserId(l.getUser() != null ? l.getUser().getId() : null);
        res.setUsername(l.getUsername());
        res.setTransactionType(l.getTransactionType());
        res.setDelta(l.getDelta());
        res.setBalanceBefore(l.getBalanceBefore());
        res.setBalanceAfter(l.getBalanceAfter());
        res.setCutJobId(l.getCutJob() != null ? l.getCutJob().getId() : null);
        res.setIdempotencyKey(l.getIdempotencyKey());
        res.setRequestId(l.getRequestId());
        res.setStatus(l.getStatus());
        res.setIpAddress(l.getIpAddress());
        res.setNotes(l.getNotes());
        res.setCreatedBy(l.getCreatedBy());
        res.setCreatedAt(l.getCreatedAt());
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDealerId() { return dealerId; }
    public void setDealerId(Long dealerId) { this.dealerId = dealerId; }
    public String getDealerName() { return dealerName; }
    public void setDealerName(String dealerName) { this.dealerName = dealerName; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public Integer getDelta() { return delta; }
    public void setDelta(Integer delta) { this.delta = delta; }
    public Integer getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(Integer balanceBefore) { this.balanceBefore = balanceBefore; }
    public Integer getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Integer balanceAfter) { this.balanceAfter = balanceAfter; }
    public Long getCutJobId() { return cutJobId; }
    public void setCutJobId(Long cutJobId) { this.cutJobId = cutJobId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
