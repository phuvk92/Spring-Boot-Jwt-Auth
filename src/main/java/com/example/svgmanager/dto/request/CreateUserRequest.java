package com.example.svgmanager.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.List;

import com.example.svgmanager.entity.Role;
import com.example.svgmanager.util.PasswordUtils;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request object for creating a new user by Admin or Agent")
public class CreateUserRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3 đến 50 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Chỉ được sử dụng chữ cái tiếng Anh, số, dấu gạch dưới (_) và dấu gạch ngang (-)")
    @Schema(example = "agent_001", description = "Tên đăng nhập (chỉ gồm chữ cái tiếng Anh, chữ số, dấu _ và -)")
    private String username;

    @NotBlank(message = "Địa chỉ email/gmail là bắt buộc")
    @Email(message = "Địa chỉ email/gmail không hợp lệ")
    @Size(max = 255, message = "Email/gmail không được vượt quá 255 ký tự")
    @Schema(example = "agent01@gmail.com", description = "Địa chỉ email/gmail bắt buộc")
    private String email;

    @Schema(example = "Nguyen Van A", description = "Full name of the user")
    @Size(max = 255, message = "Full name cannot exceed 255 characters")
    private String fullName;

    @Schema(example = "+84901234567", description = "Contact phone number")
    @Size(max = 50, message = "Phone number cannot exceed 50 characters")
    private String phone;

    @NotBlank(message = "Password cannot be blank")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
            message = "Password must be at least 8 characters long, contain at least one uppercase letter, one lowercase letter, one number, and one special character"
    )
    @Schema(example = "Password123!", description = "Initial password")
    private String password;

    @NotNull(message = "Role is required")
    @Schema(example = "AGENT", description = "User role (ADMIN, AGENT, USER)")
    private Role role;

    @Deprecated
    @Schema(example = "2", description = "Không dùng nữa — Đại lý được quản lý qua dealerId", deprecated = true)
    private Long agentId;

    @Schema(example = "1", description = "ID của đại lý / chi nhánh trực thuộc")
    private Long dealerId;

    @Schema(example = "360", description = "Số ngày phân bổ từ quỹ đại lý (quy ước 30 ngày/tháng, 360 ngày/năm)")
    private Integer allocatedDays;

    @Schema(example = "2026-12-31", description = "Ngày hết hạn tài khoản (yyyy-MM-dd). Không bắt buộc. Riêng ADMIN không áp dụng điều kiện hết hạn.")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;

    @Schema(example = "true", description = "Account status")
    private Boolean enabled = true;

    @Schema(description = "Danh sách ID danh mục kho mẫu & part được cấp quyền", example = "[1, 2]")
    private List<Long> partCategoryIds;

    public CreateUserRequest() {
    }

    public CreateUserRequest(String username, String email, String fullName, String phone, String password, Role role, Long agentId, Long dealerId, Boolean enabled) {
        this.username = com.example.svgmanager.util.UsernameUtils.trimUsername(username);
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.password = PasswordUtils.trimPassword(password);
        this.role = role;
        this.agentId = agentId;
        this.dealerId = dealerId;
        this.enabled = enabled != null ? enabled : true;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String username;
        private String email;
        private String fullName;
        private String phone;
        private String password;
        private Role role;
        private Long agentId;
        private Long dealerId;
        private Boolean enabled = true;
        private Integer allocatedDays;
        private LocalDate expirationDate;
        private List<Long> partCategoryIds;

        public Builder username(String username) {
            this.username = com.example.svgmanager.util.UsernameUtils.trimUsername(username);
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder password(String password) {
            this.password = PasswordUtils.trimPassword(password);
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder agentId(Long agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder dealerId(Long dealerId) {
            this.dealerId = dealerId;
            return this;
        }

        public Builder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder allocatedDays(Integer allocatedDays) {
            this.allocatedDays = allocatedDays;
            return this;
        }

        public Builder expirationDate(LocalDate expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public Builder partCategoryIds(List<Long> partCategoryIds) {
            this.partCategoryIds = partCategoryIds;
            return this;
        }

        public CreateUserRequest build() {
            CreateUserRequest req = new CreateUserRequest(username, email, fullName, phone, password, role, agentId, dealerId, enabled);
            req.setAllocatedDays(this.allocatedDays);
            req.setExpirationDate(this.expirationDate);
            req.setPartCategoryIds(this.partCategoryIds);
            return req;
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = com.example.svgmanager.util.UsernameUtils.trimUsername(username);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getPassword() {
        return PasswordUtils.trimPassword(password);
    }

    public void setPassword(String password) {
        this.password = PasswordUtils.trimPassword(password);
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public Long getDealerId() {
        return dealerId;
    }

    public void setDealerId(Long dealerId) {
        this.dealerId = dealerId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getAllocatedDays() {
        return allocatedDays;
    }

    public void setAllocatedDays(Integer allocatedDays) {
        this.allocatedDays = allocatedDays;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public List<Long> getPartCategoryIds() {
        return partCategoryIds;
    }

    public void setPartCategoryIds(List<Long> partCategoryIds) {
        this.partCategoryIds = partCategoryIds;
    }
}
