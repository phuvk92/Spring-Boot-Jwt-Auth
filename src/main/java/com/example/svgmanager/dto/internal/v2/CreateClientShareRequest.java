package com.example.svgmanager.dto.internal.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "Request Client A tạo lượt chia sẻ file SVG cho Client B")
public class CreateClientShareRequest {

    @NotNull(message = "fileId không được để trống")
    @Schema(description = "ID bản lưu file SVG V2 của người dùng", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fileId;

    @NotBlank(message = "Mật khẩu chia sẻ là bắt buộc")
    @Size(min = 4, max = 50, message = "Mật khẩu chia sẻ phải từ 4 đến 50 ký tự")
    @Schema(description = "Mật khẩu do Client A tự đặt để bảo vệ file chia sẻ", example = "Secret123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "Thời điểm hết hạn chia sẻ (tuỳ chọn; null nghĩa là không giới hạn thời gian)", example = "2026-12-31T23:59:59")
    private LocalDateTime expiresAt;

    @Schema(description = "Ghi chú cho lượt chia sẻ", example = "Chia sẻ cho cơ sở 2 cắt decal")
    private String note;

    public CreateClientShareRequest() {
    }

    public CreateClientShareRequest(Long fileId, String password, LocalDateTime expiresAt, String note) {
        this.fileId = fileId;
        this.password = password;
        this.expiresAt = expiresAt;
        this.note = note;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
