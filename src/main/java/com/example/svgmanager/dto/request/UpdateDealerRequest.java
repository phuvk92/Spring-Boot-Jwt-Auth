package com.example.svgmanager.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UpdateDealerRequest {

    @Schema(description = "Mã đại lý", example = "DL-0104")
    @NotBlank(message = "Mã đại lý không được để trống")
    @Size(max = 50, message = "Mã đại lý tối đa 50 ký tự")
    private String code;

    @Schema(description = "Tên đại lý", example = "Decal Ô Tô Sài Gòn")
    @NotBlank(message = "Tên đại lý không được để trống")
    @Size(max = 255, message = "Tên đại lý tối đa 255 ký tự")
    private String name;

    @Schema(description = "Khu vực / Tỉnh thành", example = "TP.HCM")
    @Size(max = 100, message = "Khu vực tối đa 100 ký tự")
    private String region;

    @Schema(description = "Địa chỉ chi tiết", example = "124 Cộng Hoà, P.12, Q.Tân Bình, TP.HCM")
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    @Schema(description = "Số điện thoại liên hệ", example = "0903123456")
    @Size(max = 50, message = "Số điện thoại tối đa 50 ký tự")
    private String phone;

    @Schema(description = "Email đại lý", example = "saigon@decaloto.vn")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @Schema(description = "Người đại diện / quản lý", example = "Nguyễn Văn Thắng")
    @Size(max = 100, message = "Người đại diện tối đa 100 ký tự")
    private String contactPerson;

    @Schema(description = "Trạng thái (ACTIVE, EXPIRING, LOCKED)", example = "ACTIVE")
    @Size(max = 50, message = "Trạng thái tối đa 50 ký tự")
    private String status = "ACTIVE";

    @Schema(description = "Ghi chú nội bộ")
    private String notes;

    // --- Cấu hình gói account đại lý ---
    @Schema(description = "Số lượng account tối đa", example = "10")
    @Min(value = 1, message = "Số lượng account tối đa phải từ 1 trở lên")
    private Integer maxAccounts;

    @Schema(description = "Tổng quỹ ngày sử dụng", example = "3600")
    @Min(value = 1, message = "Tổng quỹ ngày sử dụng phải lớn hơn 0")
    private Integer totalAllocatedDays;

    @Schema(description = "Ngày bắt đầu gói (yyyy-MM-dd)", example = "2026-01-01")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate packageStartDate;

    @Schema(description = "Ngày hết hạn gói đại lý (yyyy-MM-dd)", example = "2026-12-31")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate packageExpiryDate;

    @Schema(description = "Trạng thái gói (ACTIVE, EXPIRED, SUSPENDED)", example = "ACTIVE")
    private String packageStatus;

    public UpdateDealerRequest() {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Integer getTotalAllocatedDays() {
        return totalAllocatedDays;
    }

    public void setTotalAllocatedDays(Integer totalAllocatedDays) {
        this.totalAllocatedDays = totalAllocatedDays;
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
}
