package com.example.svgmanager.entity.v2;

import com.example.svgmanager.entity.User;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "client_svg_shares", indexes = {
        @Index(name = "idx_client_svg_shares_share_id", columnList = "share_id", unique = true),
        @Index(name = "idx_client_svg_shares_owner_user", columnList = "owner_user_id"),
        @Index(name = "idx_client_svg_shares_owner_client", columnList = "owner_client_id"),
        @Index(name = "idx_client_svg_shares_file_id", columnList = "user_svg_file_v2_id"),
        @Index(name = "idx_client_svg_shares_status", columnList = "status"),
        @Index(name = "idx_client_svg_shares_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class ClientSvgShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "share_id", nullable = false, unique = true, length = 64)
    private String shareId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_svg_file_v2_id", nullable = false)
    private UserSvgFileV2 userSvgFile;

    @Column(name = "owner_client_id", length = 100)
    private String ownerClientId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, REVOKED, EXPIRED

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by")
    private User revokedBy;

    @Column(name = "revoked_by_username", length = 100)
    private String revokedByUsername;

    @Column(name = "revoke_reason", columnDefinition = "TEXT")
    private String revokeReason;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "verify_count", nullable = false)
    private int verifyCount = 0;

    @Column(name = "preview_count", nullable = false)
    private int previewCount = 0;

    @Column(name = "download_count", nullable = false)
    private int downloadCount = 0;

    @Column(name = "failed_verify_attempts", nullable = false)
    private int failedVerifyAttempts = 0;

    @Column(name = "token_version", nullable = false)
    private int tokenVersion = 1;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ClientSvgShare() {
    }

    public boolean isRevoked() {
        return "REVOKED".equalsIgnoreCase(status) || revokedAt != null;
    }

    public boolean isExpired() {
        return "EXPIRED".equalsIgnoreCase(status) ||
                (expiresAt != null && expiresAt.isBefore(LocalDateTime.now()));
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status) && !isRevoked() && !isExpired();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public UserSvgFileV2 getUserSvgFile() {
        return userSvgFile;
    }

    public void setUserSvgFile(UserSvgFileV2 userSvgFile) {
        this.userSvgFile = userSvgFile;
    }

    public String getOwnerClientId() {
        return ownerClientId;
    }

    public void setOwnerClientId(String ownerClientId) {
        this.ownerClientId = ownerClientId;
    }

    public User getOwnerUser() {
        return ownerUser;
    }

    public void setOwnerUser(User ownerUser) {
        this.ownerUser = ownerUser;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public User getRevokedBy() {
        return revokedBy;
    }

    public void setRevokedBy(User revokedBy) {
        this.revokedBy = revokedBy;
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

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
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

    public int getFailedVerifyAttempts() {
        return failedVerifyAttempts;
    }

    public void setFailedVerifyAttempts(int failedVerifyAttempts) {
        this.failedVerifyAttempts = failedVerifyAttempts;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    public void setTokenVersion(int tokenVersion) {
        this.tokenVersion = tokenVersion;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ClientSvgShare target = new ClientSvgShare();

        public Builder shareId(String shareId) {
            target.setShareId(shareId);
            return this;
        }

        public Builder userSvgFile(UserSvgFileV2 userSvgFile) {
            target.setUserSvgFile(userSvgFile);
            return this;
        }

        public Builder ownerClientId(String ownerClientId) {
            target.setOwnerClientId(ownerClientId);
            return this;
        }

        public Builder ownerUser(User ownerUser) {
            target.setOwnerUser(ownerUser);
            return this;
        }

        public Builder passwordHash(String passwordHash) {
            target.setPasswordHash(passwordHash);
            return this;
        }

        public Builder status(String status) {
            target.setStatus(status);
            return this;
        }

        public Builder note(String note) {
            target.setNote(note);
            return this;
        }

        public Builder expiresAt(LocalDateTime expiresAt) {
            target.setExpiresAt(expiresAt);
            return this;
        }

        public Builder lastAccessedAt(LocalDateTime lastAccessedAt) {
            target.setLastAccessedAt(lastAccessedAt);
            return this;
        }

        public Builder verifyCount(int verifyCount) {
            target.setVerifyCount(verifyCount);
            return this;
        }

        public Builder previewCount(int previewCount) {
            target.setPreviewCount(previewCount);
            return this;
        }

        public Builder downloadCount(int downloadCount) {
            target.setDownloadCount(downloadCount);
            return this;
        }

        public Builder failedVerifyAttempts(int failedVerifyAttempts) {
            target.setFailedVerifyAttempts(failedVerifyAttempts);
            return this;
        }

        public Builder tokenVersion(int tokenVersion) {
            target.setTokenVersion(tokenVersion);
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            target.setCreatedAt(createdAt);
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            target.setUpdatedAt(updatedAt);
            return this;
        }

        public ClientSvgShare build() {
            return target;
        }
    }
}
