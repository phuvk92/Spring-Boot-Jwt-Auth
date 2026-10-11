package com.example.svgmanager.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "dealer_package_history", indexes = {
        @Index(name = "idx_dealer_pkg_hist_dealer", columnList = "dealer_id"),
        @Index(name = "idx_dealer_pkg_hist_user", columnList = "user_id"),
        @Index(name = "idx_dealer_pkg_hist_created", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class DealerPackageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_id", nullable = false)
    private Dealer dealer;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "previous_max_accounts")
    private Integer previousMaxAccounts;

    @Column(name = "new_max_accounts")
    private Integer newMaxAccounts;

    @Column(name = "previous_total_days")
    private Integer previousTotalDays;

    @Column(name = "new_total_days")
    private Integer newTotalDays;

    @Column(name = "used_days")
    private Integer usedDays;

    @Column(name = "change_days")
    private Integer changeDays;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DealerPackageHistory() {
    }

    public DealerPackageHistory(Long id, Dealer dealer, String action, Integer previousMaxAccounts,
                                Integer newMaxAccounts, Integer previousTotalDays, Integer newTotalDays,
                                Integer usedDays, Integer changeDays, User user, String notes,
                                String createdBy, LocalDateTime createdAt) {
        this.id = id;
        this.dealer = dealer;
        this.action = action;
        this.previousMaxAccounts = previousMaxAccounts;
        this.newMaxAccounts = newMaxAccounts;
        this.previousTotalDays = previousTotalDays;
        this.newTotalDays = newTotalDays;
        this.usedDays = usedDays;
        this.changeDays = changeDays;
        this.user = user;
        this.notes = notes;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getPreviousMaxAccounts() {
        return previousMaxAccounts;
    }

    public void setPreviousMaxAccounts(Integer previousMaxAccounts) {
        this.previousMaxAccounts = previousMaxAccounts;
    }

    public Integer getNewMaxAccounts() {
        return newMaxAccounts;
    }

    public void setNewMaxAccounts(Integer newMaxAccounts) {
        this.newMaxAccounts = newMaxAccounts;
    }

    public Integer getPreviousTotalDays() {
        return previousTotalDays;
    }

    public void setPreviousTotalDays(Integer previousTotalDays) {
        this.previousTotalDays = previousTotalDays;
    }

    public Integer getNewTotalDays() {
        return newTotalDays;
    }

    public void setNewTotalDays(Integer newTotalDays) {
        this.newTotalDays = newTotalDays;
    }

    public Integer getUsedDays() {
        return usedDays;
    }

    public void setUsedDays(Integer usedDays) {
        this.usedDays = usedDays;
    }

    public Integer getChangeDays() {
        return changeDays;
    }

    public void setChangeDays(Integer changeDays) {
        this.changeDays = changeDays;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static class Builder {
        private Long id;
        private Dealer dealer;
        private String action;
        private Integer previousMaxAccounts;
        private Integer newMaxAccounts;
        private Integer previousTotalDays;
        private Integer newTotalDays;
        private Integer usedDays;
        private Integer changeDays;
        private User user;
        private String notes;
        private String createdBy;
        private LocalDateTime createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder dealer(Dealer dealer) {
            this.dealer = dealer;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder previousMaxAccounts(Integer previousMaxAccounts) {
            this.previousMaxAccounts = previousMaxAccounts;
            return this;
        }

        public Builder newMaxAccounts(Integer newMaxAccounts) {
            this.newMaxAccounts = newMaxAccounts;
            return this;
        }

        public Builder previousTotalDays(Integer previousTotalDays) {
            this.previousTotalDays = previousTotalDays;
            return this;
        }

        public Builder newTotalDays(Integer newTotalDays) {
            this.newTotalDays = newTotalDays;
            return this;
        }

        public Builder usedDays(Integer usedDays) {
            this.usedDays = usedDays;
            return this;
        }

        public Builder changeDays(Integer changeDays) {
            this.changeDays = changeDays;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public DealerPackageHistory build() {
            return new DealerPackageHistory(id, dealer, action, previousMaxAccounts, newMaxAccounts,
                    previousTotalDays, newTotalDays, usedDays, changeDays, user, notes, createdBy, createdAt);
        }
    }
}
