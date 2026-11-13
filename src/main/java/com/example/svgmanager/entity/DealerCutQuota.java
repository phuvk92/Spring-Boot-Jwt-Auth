package com.example.svgmanager.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "dealer_cut_quotas", indexes = {
        @Index(name = "idx_dealer_cut_quotas_dealer", columnList = "dealer_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
public class DealerCutQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_id", nullable = false, unique = true)
    private Dealer dealer;

    @Column(name = "allocated_cuts", nullable = false)
    private Integer allocatedCuts = 1000;

    @Column(name = "consumed_cuts", nullable = false)
    private Integer consumedCuts = 0;

    @Column(name = "remaining_cuts", nullable = false)
    private Integer remainingCuts = 1000;

    @Column(name = "success_cuts", nullable = false)
    private Integer successCuts = 0;

    @Column(name = "failed_cuts", nullable = false)
    private Integer failedCuts = 0;

    @Column(name = "refunded_cuts", nullable = false)
    private Integer refundedCuts = 0;

    @Column(name = "adjusted_cuts", nullable = false)
    private Integer adjustedCuts = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public DealerCutQuota() {
    }

    public DealerCutQuota(Dealer dealer, Integer initialCuts) {
        this.dealer = dealer;
        this.allocatedCuts = initialCuts != null ? initialCuts : 1000;
        this.remainingCuts = this.allocatedCuts;
        this.consumedCuts = 0;
        this.successCuts = 0;
        this.failedCuts = 0;
        this.refundedCuts = 0;
        this.adjustedCuts = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Dealer getDealer() {
        return dealer;
    }

    public void setDealer(Dealer dealer) {
        this.dealer = dealer;
    }

    public Integer getAllocatedCuts() {
        return allocatedCuts;
    }

    public void setAllocatedCuts(Integer allocatedCuts) {
        this.allocatedCuts = allocatedCuts;
    }

    public Integer getConsumedCuts() {
        return consumedCuts;
    }

    public void setConsumedCuts(Integer consumedCuts) {
        this.consumedCuts = consumedCuts;
    }

    public Integer getRemainingCuts() {
        return remainingCuts;
    }

    public void setRemainingCuts(Integer remainingCuts) {
        this.remainingCuts = remainingCuts;
    }

    public Integer getSuccessCuts() {
        return successCuts;
    }

    public void setSuccessCuts(Integer successCuts) {
        this.successCuts = successCuts;
    }

    public Integer getFailedCuts() {
        return failedCuts;
    }

    public void setFailedCuts(Integer failedCuts) {
        this.failedCuts = failedCuts;
    }

    public Integer getRefundedCuts() {
        return refundedCuts;
    }

    public void setRefundedCuts(Integer refundedCuts) {
        this.refundedCuts = refundedCuts;
    }

    public Integer getAdjustedCuts() {
        return adjustedCuts;
    }

    public void setAdjustedCuts(Integer adjustedCuts) {
        this.adjustedCuts = adjustedCuts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
