package com.example.svgmanager.dto.internal.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Thông tin lượt chia sẻ file SVG giữa các Client")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientShareResponse {

    @Schema(description = "Mã định danh duy nhất của lượt chia sẻ (ddMMyyyy + 6 ký tự random)", example = "01112026Ab1234")
    private String shareId;

    @Schema(description = "ID bản lưu file SVG V2", example = "101")
    private Long fileId;

    @Schema(description = "Tên hiển thị file", example = "toyota_camry_door.svg")
    private String fileName;

    @Schema(description = "Tên file gốc", example = "toyota_camry_door.svg")
    private String originalFileName;

    @Schema(description = "Kích thước file (bytes)", example = "20480")
    private Long fileSize;

    @Schema(description = "Trạng thái lượt chia sẻ: ACTIVE, REVOKED, EXPIRED", example = "ACTIVE")
    private String status;

    @Schema(description = "Ghi chú chia sẻ", example = "Chia sẻ cho cơ sở 2")
    private String note;

    @Schema(description = "Thời điểm hết hạn chia sẻ, nullable")
    private LocalDateTime expiresAt;

    @Schema(description = "Số lượt xác thực thành công", example = "3")
    private int verifyCount;

    @Schema(description = "Số lượt Preview", example = "5")
    private int previewCount;

    @Schema(description = "Số lượt Download", example = "2")
    private int downloadCount;

    @Schema(description = "Thời điểm truy cập gần nhất")
    private LocalDateTime lastAccessedAt;

    @Schema(description = "Thời điểm tạo")
    private LocalDateTime createdAt;

    @Schema(description = "Thời điểm thu hồi")
    private LocalDateTime revokedAt;

    @Schema(description = "Tên người thu hồi")
    private String revokedByUsername;

    @Schema(description = "Lý do thu hồi")
    private String revokeReason;

    @Schema(description = "Thông tin người tạo lượt chia sẻ")
    private CreatedByDto createdBy;

    public ClientShareResponse() {
    }

    public static class CreatedByDto {
        private Long id;
        private String username;
        private String fullName;

        public CreatedByDto() {
        }

        public CreatedByDto(Long id, String username, String fullName) {
            this.id = id;
            this.username = username;
            this.fullName = fullName;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }
    }

    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getVerifyCount() {
        return verifyCount;
    }

    public void setVerifyCount(int verifyCount) {
        this.verifyCount = verifyCount;
    }

    public int getPreviewCount() {
        return previewCount;
    }

    public void setPreviewCount(int previewCount) {
        this.previewCount = previewCount;
    }

    public int getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(int downloadCount) {
        this.downloadCount = downloadCount;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRevokedByUsername() {
        return revokedByUsername;
    }

    public void setRevokedByUsername(String revokedByUsername) {
        this.revokedByUsername = revokedByUsername;
    }

    public String getRevokeReason() {
        return revokeReason;
    }

    public void setRevokeReason(String revokeReason) {
        this.revokeReason = revokeReason;
    }

    public CreatedByDto getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(CreatedByDto createdBy) {
        this.createdBy = createdBy;
    }
}
