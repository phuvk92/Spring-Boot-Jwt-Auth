package com.example.svgmanager.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DealerResponse {

    @Schema(description = "ID đại lý", example = "1")
    private Long id;

    @Schema(description = "Mã đại lý", example = "DL-0104")
    private String code;

    @Schema(description = "Tên đại lý", example = "Decal Ô Tô Sài Gòn")
    private String name;

    @Schema(description = "Khu vực / Tỉnh thành", example = "TP.HCM")
    private String region;

    @Schema(description = "Địa chỉ chi tiết", example = "124 Cộng Hoà, P.12, Q.Tân Bình, TP.HCM")
    private String address;

    @Schema(description = "Số điện thoại liên hệ", example = "0903123456")
    private String phone;

    @Schema(description = "Email đại lý", example = "saigon@decaloto.vn")
    private String email;

    @Schema(description = "Người đại diện / quản lý", example = "Nguyễn Văn Thắng")
    private String contactPerson;

    @Schema(description = "Gói cước (Cơ bản, Chuyên nghiệp, Chuỗi, Enterprise)", example = "Chuỗi")
    private String plan;

    @Schema(description = "Hạn dùng", example = "12/2026")
    private String dueDate;

    @Schema(description = "Trạng thái (ACTIVE, EXPIRING, LOCKED)", example = "ACTIVE")
    private String status;

    @Schema(description = "Số lượng user / thợ thuộc đại lý", example = "14")
    private long usersCount;

    @Schema(description = "Ghi chú nội bộ")
    private String notes;

    // --- Thông tin gói account của đại lý ---
    @Schema(description = "Số lượng account tối đa", example = "10")
    private Integer maxAccounts;

    @Schema(description = "Số lượng account đã tạo", example = "4")
    private Integer createdAccounts;

    @Schema(description = "Số lượng account còn lại", example = "6")
    private Integer remainingAccounts;

    @Schema(description = "Tổng quỹ ngày sử dụng", example = "3600")
    private Integer totalAllocatedDays;

    @Schema(description = "Số ngày đã phân bổ", example = "1440")
    private Integer usedAllocatedDays;

    @Schema(description = "Số ngày còn lại", example = "2160")
    private Integer remainingAllocatedDays;

    @Schema(description = "Ngày bắt đầu gói (yyyy-MM-dd)", example = "2026-01-01")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate packageStartDate;

    @Schema(description = "Ngày hết hạn gói đại lý (yyyy-MM-dd)", example = "2026-12-31")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate packageExpiryDate;

    @Schema(description = "Trạng thái gói (ACTIVE, EXPIRED, SUSPENDED)", example = "ACTIVE")
    private String packageStatus;

    @Schema(description = "Gói đã hết hạn hay chưa", example = "false")
    private boolean packageExpired;

    @Schema(description = "Thời gian tạo")
    private LocalDateTime createdAt;

    @Schema(description = "Thời gian cập nhật")
    private LocalDateTime updatedAt;

    public DealerResponse() {
    }

    public DealerResponse(Long id, String code, String name, String region, String address,
                          String phone, String email, String contactPerson, String plan,
                          String dueDate, String status, long usersCount, String notes,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.region = region;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.contactPerson = contactPerson;
        this.plan = plan;
        this.dueDate = dueDate;
        this.status = status;
        this.usersCount = usersCount;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getUsersCount() {
        return usersCount;
    }

    public void setUsersCount(long usersCount) {
        this.usersCount = usersCount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
