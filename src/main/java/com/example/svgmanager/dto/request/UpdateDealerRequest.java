package com.example.svgmanager.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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

    @Schema(description = "Gói cước (Cơ bản, Chuyên nghiệp, Chuỗi, Enterprise)", example = "Chuỗi")
    @Size(max = 50, message = "Gói cước tối đa 50 ký tự")
    private String plan = "Cơ bản";

    @Schema(description = "Hạn dùng / Ngày hết hạn", example = "12/2026")
    @Size(max = 50, message = "Hạn dùng tối đa 50 ký tự")
    private String dueDate;

    @Schema(description = "Trạng thái (ACTIVE, EXPIRING, LOCKED)", example = "ACTIVE")
    @Size(max = 50, message = "Trạng thái tối đa 50 ký tự")
    private String status = "ACTIVE";

    @Schema(description = "Ghi chú nội bộ")
    private String notes;

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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
