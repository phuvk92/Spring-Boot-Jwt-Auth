package com.example.svgmanager.dto.response;

import java.time.LocalDateTime;

public class AdminShareItemResponse {

    private String uid;
    private Long originalId;
    private String shareId;
    private String shareType; // ADMIN_TO_CLIENT hoặc CLIENT_TO_CLIENT
    private Long fileId;
    private String fileName;
    private String categoryName;
    private UserSummaryDto sharedBy;
    private UserSummaryDto recipient;
    private String ownerClientId;
    private String status; // ACTIVE, REVOKED, EXPIRED
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private UserSummaryDto revokedBy;
    private String revokeReason;
    private int verifyCount;
    private int failedVerifyCount;
    private int previewCount;
    private int downloadCount;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String version;

    public AdminShareItemResponse() {
    }

    public static class UserSummaryDto {
        private Long id;
        private String username;
        private String fullName;
        private String role;
        private Long dealerId;
        private String dealerName;

        public UserSummaryDto() {
        }

        public UserSummaryDto(Long id, String username, String fullName, String role, Long dealerId, String dealerName) {
            this.id = id;
            this.username = username;
            this.fullName = fullName;
            this.role = role;
            this.dealerId = dealerId;
            this.dealerName = dealerName;
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

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public Long getDealerId() {
            return dealerId;
        }

        public void setDealerId(Long dealerId) {
            this.dealerId = dealerId;
        }

        public String getDealerName() {
            return dealerName;
        }

        public void setDealerName(String dealerName) {
            this.dealerName = dealerName;
        }
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Long getOriginalId() {
        return originalId;
    }

    public void setOriginalId(Long originalId) {
        this.originalId = originalId;
    }

    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public String getShareType() {
        return shareType;
    }

    public void setShareType(String shareType) {
        this.shareType = shareType;
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

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public UserSummaryDto getSharedBy() {
        return sharedBy;
    }

    public void setSharedBy(UserSummaryDto sharedBy) {
        this.sharedBy = sharedBy;
    }

    public UserSummaryDto getRecipient() {
        return recipient;
    }

    public void setRecipient(UserSummaryDto recipient) {
        this.recipient = recipient;
    }

    public String getOwnerClientId() {
        return ownerClientId;
    }

    public void setOwnerClientId(String ownerClientId) {
        this.ownerClientId = ownerClientId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public UserSummaryDto getRevokedBy() {
        return revokedBy;
    }

    public void setRevokedBy(UserSummaryDto revokedBy) {
        this.revokedBy = revokedBy;
    }

    public String getRevokeReason() {
        return revokeReason;
    }

    public void setRevokeReason(String revokeReason) {
        this.revokeReason = revokeReason;
    }

    public int getVerifyCount() {
        return verifyCount;
    }

    public void setVerifyCount(int verifyCount) {
        this.verifyCount = verifyCount;
    }

    public int getFailedVerifyCount() {
        return failedVerifyCount;
    }

    public void setFailedVerifyCount(int failedVerifyCount) {
        this.failedVerifyCount = failedVerifyCount;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
