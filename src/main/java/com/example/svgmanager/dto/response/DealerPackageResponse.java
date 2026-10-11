package com.example.svgmanager.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Thông tin gói tài khoản và quỹ ngày của đại lý")
public class DealerPackageResponse {

    @Schema(description = "ID đại lý", example = "1")
    private Long dealerId;

    @Schema(description = "Mã đại lý", example = "DL-0104")
    private String dealerCode;

    @Schema(description = "Tên đại lý", example = "Decal Ô Tô Sài Gòn")
    private String dealerName;

    @Schema(description = "Số lượng account tối đa được tạo trong gói", example = "10")
    private Integer maxAccounts;

    @Schema(description = "Số lượng account đã tạo hiện tại", example = "4")
    private Integer createdAccounts;

    @Schema(description = "Số lượng account còn lại có thể tạo", example = "6")
    private Integer remainingAccounts;

    @Schema(description = "Tổng quỹ ngày sử dụng được cấp", example = "3600")
    private Integer totalAllocatedDays;

    @Schema(description = "Số ngày đã phân bổ cho các account", example = "1440")
    private Integer usedAllocatedDays;

    @Schema(description = "Số ngày còn lại chưa phân bổ", example = "2160")
    private Integer remainingAllocatedDays;

    @Schema(description = "Ngày bắt đầu gói (yyyy-MM-dd)", example = "2026-01-01")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate packageStartDate;

    @Schema(description = "Ngày hết hạn của gói đại lý (yyyy-MM-dd)", example = "2026-12-31")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate packageExpiryDate;

    @Schema(description = "Trạng thái gói (ACTIVE, EXPIRED, SUSPENDED)", example = "ACTIVE")
    private String packageStatus;

    @Schema(description = "Gói đã hết hạn hay chưa", example = "false")
    private boolean packageExpired;

    @Schema(description = "Gói có đang hoạt động hợp lệ hay không", example = "true")
    private boolean packageActive;

    public DealerPackageResponse() {
    }

    public DealerPackageResponse(Long dealerId, String dealerCode, String dealerName,
                                 Integer maxAccounts, Integer createdAccounts, Integer remainingAccounts,
                                 Integer totalAllocatedDays, Integer usedAllocatedDays, Integer remainingAllocatedDays,
                                 LocalDate packageStartDate, LocalDate packageExpiryDate, String packageStatus,
                                 boolean packageExpired, boolean packageActive) {
        this.dealerId = dealerId;
        this.dealerCode = dealerCode;
        this.dealerName = dealerName;
        this.maxAccounts = maxAccounts;
        this.createdAccounts = createdAccounts;
        this.remainingAccounts = remainingAccounts;
        this.totalAllocatedDays = totalAllocatedDays;
        this.usedAllocatedDays = usedAllocatedDays;
        this.remainingAllocatedDays = remainingAllocatedDays;
        this.packageStartDate = packageStartDate;
        this.packageExpiryDate = packageExpiryDate;
        this.packageStatus = packageStatus;
        this.packageExpired = packageExpired;
        this.packageActive = packageActive;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getDealerId() {
        return dealerId;
    }

    public void setDealerId(Long dealerId) {
        this.dealerId = dealerId;
    }

    public String getDealerCode() {
        return dealerCode;
    }

    public void setDealerCode(String dealerCode) {
        this.dealerCode = dealerCode;
    }

    public String getDealerName() {
        return dealerName;
    }

    public void setDealerName(String dealerName) {
        this.dealerName = dealerName;
    }

    public Integer getMaxAccounts() {
        return maxAccounts;
    }

    public void setMaxAccounts(Integer maxAccounts) {
        this.maxAccounts = maxAccounts;
    }

    public Integer getCreatedAccounts() {
        return createdAccounts;
    }

    public void setCreatedAccounts(Integer createdAccounts) {
        this.createdAccounts = createdAccounts;
    }

    public Integer getRemainingAccounts() {
        return remainingAccounts;
    }

    public void setRemainingAccounts(Integer remainingAccounts) {
        this.remainingAccounts = remainingAccounts;
    }

    public Integer getTotalAllocatedDays() {
        return totalAllocatedDays;
    }

    public void setTotalAllocatedDays(Integer totalAllocatedDays) {
        this.totalAllocatedDays = totalAllocatedDays;
    }

    public Integer getUsedAllocatedDays() {
        return usedAllocatedDays;
    }

    public void setUsedAllocatedDays(Integer usedAllocatedDays) {
        this.usedAllocatedDays = usedAllocatedDays;
    }

    public Integer getRemainingAllocatedDays() {
        return remainingAllocatedDays;
    }

    public void setRemainingAllocatedDays(Integer remainingAllocatedDays) {
        this.remainingAllocatedDays = remainingAllocatedDays;
    }

    public LocalDate getPackageStartDate() {
        return packageStartDate;
    }

    public void setPackageStartDate(LocalDate packageStartDate) {
        this.packageStartDate = packageStartDate;
    }

    public LocalDate getPackageExpiryDate() {
        return packageExpiryDate;
    }

    public void setPackageExpiryDate(LocalDate packageExpiryDate) {
        this.packageExpiryDate = packageExpiryDate;
    }

    public String getPackageStatus() {
        return packageStatus;
    }

    public void setPackageStatus(String packageStatus) {
        this.packageStatus = packageStatus;
    }

    public boolean isPackageExpired() {
        return packageExpired;
    }

    public void setPackageExpired(boolean packageExpired) {
        this.packageExpired = packageExpired;
    }

    public boolean isPackageActive() {
        return packageActive;
    }

    public void setPackageActive(boolean packageActive) {
        this.packageActive = packageActive;
    }

    public static class Builder {
        private Long dealerId;
        private String dealerCode;
        private String dealerName;
        private Integer maxAccounts;
        private Integer createdAccounts;
        private Integer remainingAccounts;
        private Integer totalAllocatedDays;
        private Integer usedAllocatedDays;
        private Integer remainingAllocatedDays;
        private LocalDate packageStartDate;
        private LocalDate packageExpiryDate;
        private String packageStatus;
        private boolean packageExpired;
        private boolean packageActive;

        public Builder dealerId(Long dealerId) {
            this.dealerId = dealerId;
            return this;
        }

        public Builder dealerCode(String dealerCode) {
            this.dealerCode = dealerCode;
            return this;
        }

        public Builder dealerName(String dealerName) {
            this.dealerName = dealerName;
            return this;
        }

        public Builder maxAccounts(Integer maxAccounts) {
            this.maxAccounts = maxAccounts;
            return this;
        }

        public Builder createdAccounts(Integer createdAccounts) {
            this.createdAccounts = createdAccounts;
            return this;
        }

        public Builder remainingAccounts(Integer remainingAccounts) {
            this.remainingAccounts = remainingAccounts;
            return this;
        }

        public Builder totalAllocatedDays(Integer totalAllocatedDays) {
            this.totalAllocatedDays = totalAllocatedDays;
            return this;
        }

        public Builder usedAllocatedDays(Integer usedAllocatedDays) {
            this.usedAllocatedDays = usedAllocatedDays;
            return this;
        }

        public Builder remainingAllocatedDays(Integer remainingAllocatedDays) {
            this.remainingAllocatedDays = remainingAllocatedDays;
            return this;
        }

        public Builder packageStartDate(LocalDate packageStartDate) {
            this.packageStartDate = packageStartDate;
            return this;
        }

        public Builder packageExpiryDate(LocalDate packageExpiryDate) {
            this.packageExpiryDate = packageExpiryDate;
            return this;
        }

        public Builder packageStatus(String packageStatus) {
            this.packageStatus = packageStatus;
            return this;
        }

        public Builder packageExpired(boolean packageExpired) {
            this.packageExpired = packageExpired;
            return this;
        }

        public Builder packageActive(boolean packageActive) {
            this.packageActive = packageActive;
            return this;
        }

        public DealerPackageResponse build() {
            return new DealerPackageResponse(dealerId, dealerCode, dealerName, maxAccounts,
                    createdAccounts, remainingAccounts, totalAllocatedDays, usedAllocatedDays,
                    remainingAllocatedDays, packageStartDate, packageExpiryDate, packageStatus,
                    packageExpired, packageActive);
        }
    }
}
