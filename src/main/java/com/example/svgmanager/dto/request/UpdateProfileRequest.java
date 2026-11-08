package com.example.svgmanager.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request object for updating own profile")
public class UpdateProfileRequest {

    @Size(max = 255, message = "Tên hiển thị tối đa 255 ký tự")
    @Schema(example = "Super Admin", description = "Tên hiển thị mới")
    private String fullName;

    @Size(max = 50, message = "Số điện thoại tối đa 50 ký tự")
    @Schema(example = "+84901234567", description = "Số điện thoại liên hệ")
    private String phone;

    @Schema(description = "Ảnh đại diện (Data URI hoặc URL)")
    private String avatar;

    public UpdateProfileRequest() {
    }

    public UpdateProfileRequest(String fullName, String phone, String avatar) {
        this.fullName = fullName;
        this.phone = phone;
        this.avatar = avatar;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
