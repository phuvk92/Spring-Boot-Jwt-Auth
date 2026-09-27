package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
