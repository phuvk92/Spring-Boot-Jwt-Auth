package com.example.svgmanager.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "cut_quota_ledger", indexes = {
        @Index(name = "idx_cut_quota_ledger_dealer", columnList = "dealer_id"),
        @Index(name = "idx_cut_quota_ledger_user", columnList = "user_id"),
        @Index(name = "idx_cut_quota_ledger_created", columnList = "created_at DESC"),
        @Index(name = "idx_cut_quota_ledger_job", columnList = "cut_job_id"),
        @Index(name = "idx_cut_quota_ledger_type", columnList = "transaction_type")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_ledger_dealer_idempotency", columnNames = {"dealer_id", "idempotency_key"})
})
@EntityListeners(AuditingEntityListener.class)
public class CutQuotaLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_id", nullable = false)
    private Dealer dealer;

    @Column(name = "dealer_name", nullable = false, length = 255)
    private String dealerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "transaction_type", nullable = false, length = 50)
    private String transactionType; // ALLOCATE, CONSUME, REFUND, ADJUST

    @Column(nullable = false)
    private Integer delta;

    @Column(name = "balance_before", nullable = false)
    private Integer balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cut_job_id")
    private CutJob cutJob;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(nullable = false, length = 50)
    private String status = "SUCCESS"; // SUCCESS, FAILED, REFUNDED

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public CutQuotaLedger() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Dealer dealer;
        private String dealerName;
        private User user;
        private String username;
        private String transactionType;
        private Integer delta;
        private Integer balanceBefore;
        private Integer balanceAfter;
        private CutJob cutJob;
        private String idempotencyKey;
        private String requestId;
        private String status = "SUCCESS";
        private String ipAddress;
        private String notes;
        private String createdBy;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder id(Long id) { this.id = id; return this; }
        public Builder dealer(Dealer dealer) { this.dealer = dealer; return this; }
        public Builder dealerName(String dealerName) { this.dealerName = dealerName; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder transactionType(String transactionType) { this.transactionType = transactionType; return this; }
        public Builder delta(Integer delta) { this.delta = delta; return this; }
        public Builder balanceBefore(Integer balanceBefore) { this.balanceBefore = balanceBefore; return this; }
        public Builder balanceAfter(Integer balanceAfter) { this.balanceAfter = balanceAfter; return this; }
        public Builder cutJob(CutJob cutJob) { this.cutJob = cutJob; return this; }
        public Builder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public Builder requestId(String requestId) { this.requestId = requestId; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public CutQuotaLedger build() {
            CutQuotaLedger l = new CutQuotaLedger();
            l.id = this.id;
            l.dealer = this.dealer;
            l.dealerName = this.dealerName;
            l.user = this.user;
            l.username = this.username;
            l.transactionType = this.transactionType;
            l.delta = this.delta;
            l.balanceBefore = this.balanceBefore;
            l.balanceAfter = this.balanceAfter;
            l.cutJob = this.cutJob;
            l.idempotencyKey = this.idempotencyKey;
            l.requestId = this.requestId;
            l.status = this.status;
            l.ipAddress = this.ipAddress;
            l.notes = this.notes;
            l.createdBy = this.createdBy;
            l.createdAt = this.createdAt != null ? this.createdAt : LocalDateTime.now();
            return l;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Dealer getDealer() { return dealer; }
    public void setDealer(Dealer dealer) { this.dealer = dealer; }
    public String getDealerName() { return dealerName; }
    public void setDealerName(String dealerName) { this.dealerName = dealerName; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
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
    public CutJob getCutJob() { return cutJob; }
    public void setCutJob(CutJob cutJob) { this.cutJob = cutJob; }
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
