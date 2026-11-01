package com.example.svgmanager.entity.v2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import java.time.LocalDateTime;

/**
 * Read-only JPA entity hợp nhất toàn bộ dữ liệu chia sẻ của Admin và Client.
 * Sử dụng @Subselect để tương thích hoàn toàn cả PostgreSQL (production) và H2 in-memory (integration test).
 */
@Entity
@Immutable
@Subselect("""
    SELECT
        CONCAT('C2C_', c.id) AS uid,
        c.id AS original_id,
        c.share_id AS share_id,
        'CLIENT_TO_CLIENT' AS share_type,
        c.user_svg_file_v2_id AS file_id,
        f.file_name AS file_name,
        cat.name AS category_name,
        c.owner_user_id AS shared_by_user_id,
        u_owner.username AS shared_by_username,
        u_owner.full_name AS shared_by_full_name,
        u_owner.role AS shared_by_role,
        d_owner.id AS shared_by_dealer_id,
        d_owner.name AS shared_by_dealer_name,
        CAST(NULL AS BIGINT) AS shared_to_user_id,
        CAST(NULL AS VARCHAR(100)) AS shared_to_username,
        CAST(NULL AS VARCHAR(255)) AS shared_to_full_name,
        CAST(NULL AS VARCHAR(50)) AS shared_to_role,
        CAST(NULL AS BIGINT) AS shared_to_dealer_id,
        CAST(NULL AS VARCHAR(255)) AS shared_to_dealer_name,
        c.owner_client_id AS owner_client_id,
        CASE
            WHEN c.status = 'REVOKED' OR c.revoked_at IS NOT NULL THEN 'REVOKED'
            WHEN c.expires_at IS NOT NULL AND c.expires_at < CURRENT_TIMESTAMP THEN 'EXPIRED'
            ELSE c.status
        END AS status,
        c.expires_at AS expires_at,
        c.revoked_at AS revoked_at,
        c.revoked_by AS revoked_by_id,
        c.revoked_by_username AS revoked_by_username,
        c.revoke_reason AS revoke_reason,
        c.verify_count AS verify_count,
        c.failed_verify_attempts AS failed_verify_count,
        c.preview_count AS preview_count,
        c.download_count AS download_count,
        c.last_accessed_at AS last_accessed_at,
        c.created_at AS created_at,
        c.updated_at AS updated_at,
        'V2' AS version
    FROM client_svg_shares c
    LEFT JOIN user_svg_file_v2 f ON c.user_svg_file_v2_id = f.id
    LEFT JOIN file_categories cat ON f.category_id = cat.id
    LEFT JOIN users u_owner ON c.owner_user_id = u_owner.id
    LEFT JOIN dealers d_owner ON u_owner.dealer_id = d_owner.id

    UNION ALL

    SELECT
        CONCAT('DIR_V2_', s.id) AS uid,
        s.id AS original_id,
        CONCAT('DIR-V2-', s.id) AS share_id,
        'ADMIN_TO_CLIENT' AS share_type,
        s.user_svg_file_v2_id AS file_id,
        f.file_name AS file_name,
        cat.name AS category_name,
        s.shared_by_user_id AS shared_by_user_id,
        u_by.username AS shared_by_username,
        u_by.full_name AS shared_by_full_name,
        u_by.role AS shared_by_role,
        d_by.id AS shared_by_dealer_id,
        d_by.name AS shared_by_dealer_name,
        s.shared_to_user_id AS shared_to_user_id,
        u_to.username AS shared_to_username,
        u_to.full_name AS shared_to_full_name,
        u_to.role AS shared_to_role,
        d_to.id AS shared_to_dealer_id,
        d_to.name AS shared_to_dealer_name,
        CAST(NULL AS VARCHAR(100)) AS owner_client_id,
        CASE
            WHEN s.status = 'REVOKED' OR s.revoked_at IS NOT NULL THEN 'REVOKED'
            ELSE s.status
        END AS status,
        CAST(NULL AS TIMESTAMP) AS expires_at,
        s.revoked_at AS revoked_at,
        s.revoked_by_id AS revoked_by_id,
        u_rev.username AS revoked_by_username,
        s.revoke_reason AS revoke_reason,
        0 AS verify_count,
        0 AS failed_verify_count,
        0 AS preview_count,
        0 AS download_count,
        s.updated_at AS last_accessed_at,
        s.created_at AS created_at,
        s.updated_at AS updated_at,
        'V2' AS version
    FROM user_svg_file_v2_share s
    LEFT JOIN user_svg_file_v2 f ON s.user_svg_file_v2_id = f.id
    LEFT JOIN file_categories cat ON f.category_id = cat.id
    LEFT JOIN users u_by ON s.shared_by_user_id = u_by.id
    LEFT JOIN dealers d_by ON u_by.dealer_id = d_by.id
    LEFT JOIN users u_to ON s.shared_to_user_id = u_to.id
    LEFT JOIN dealers d_to ON u_to.dealer_id = d_to.id
    LEFT JOIN users u_rev ON s.revoked_by_id = u_rev.id

    UNION ALL

    SELECT
        CONCAT('DIR_V1_', s.id) AS uid,
        s.id AS original_id,
        CONCAT('DIR-V1-', s.id) AS share_id,
        'ADMIN_TO_CLIENT' AS share_type,
        s.user_svg_file_id AS file_id,
        f.file_name AS file_name,
        cat.name AS category_name,
        s.shared_by_user_id AS shared_by_user_id,
        u_by.username AS shared_by_username,
        u_by.full_name AS shared_by_full_name,
        u_by.role AS shared_by_role,
        d_by.id AS shared_by_dealer_id,
        d_by.name AS shared_by_dealer_name,
        s.shared_to_user_id AS shared_to_user_id,
        u_to.username AS shared_to_username,
        u_to.full_name AS shared_to_full_name,
        u_to.role AS shared_to_role,
        d_to.id AS shared_to_dealer_id,
        d_to.name AS shared_to_dealer_name,
        CAST(NULL AS VARCHAR(100)) AS owner_client_id,
        CASE
            WHEN s.status = 'REVOKED' OR s.revoked_at IS NOT NULL THEN 'REVOKED'
            ELSE s.status
        END AS status,
        CAST(NULL AS TIMESTAMP) AS expires_at,
        s.revoked_at AS revoked_at,
        s.revoked_by_id AS revoked_by_id,
        u_rev.username AS revoked_by_username,
        s.revoke_reason AS revoke_reason,
        0 AS verify_count,
        0 AS failed_verify_count,
        0 AS preview_count,
        0 AS download_count,
        s.updated_at AS last_accessed_at,
        s.created_at AS created_at,
        s.updated_at AS updated_at,
        'V1' AS version
    FROM user_svg_file_share s
    LEFT JOIN user_svg_files f ON s.user_svg_file_id = f.id
    LEFT JOIN file_categories cat ON f.category_id = cat.id
    LEFT JOIN users u_by ON s.shared_by_user_id = u_by.id
    LEFT JOIN dealers d_by ON u_by.dealer_id = d_by.id
    LEFT JOIN users u_to ON s.shared_to_user_id = u_to.id
    LEFT JOIN dealers d_to ON u_to.dealer_id = d_to.id
    LEFT JOIN users u_rev ON s.revoked_by_id = u_rev.id
    """)
@Synchronize({"client_svg_shares", "user_svg_file_v2_share", "user_svg_file_share"})
public class UnifiedShare {

    @Id
    @Column(name = "uid", length = 100)
    private String uid;

    @Column(name = "original_id")
    private Long originalId;

    @Column(name = "share_id", length = 64)
    private String shareId;

    @Column(name = "share_type", length = 30)
    private String shareType; // ADMIN_TO_CLIENT hoặc CLIENT_TO_CLIENT

    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "category_name", length = 255)
    private String categoryName;

    @Column(name = "shared_by_user_id")
    private Long sharedByUserId;

    @Column(name = "shared_by_username", length = 100)
    private String sharedByUsername;

    @Column(name = "shared_by_full_name", length = 255)
    private String sharedByFullName;

    @Column(name = "shared_by_role", length = 50)
    private String sharedByRole;

    @Column(name = "shared_by_dealer_id")
    private Long sharedByDealerId;

    @Column(name = "shared_by_dealer_name", length = 255)
    private String sharedByDealerName;

    @Column(name = "shared_to_user_id")
    private Long sharedToUserId;

    @Column(name = "shared_to_username", length = 100)
    private String sharedToUsername;

    @Column(name = "shared_to_full_name", length = 255)
    private String sharedToFullName;

    @Column(name = "shared_to_role", length = 50)
    private String sharedToRole;

    @Column(name = "shared_to_dealer_id")
    private Long sharedToDealerId;

    @Column(name = "shared_to_dealer_name", length = 255)
    private String sharedToDealerName;

    @Column(name = "owner_client_id", length = 100)
    private String ownerClientId;

    @Column(name = "status", length = 20)
    private String status; // ACTIVE, REVOKED, EXPIRED

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by_id")
    private Long revokedById;

    @Column(name = "revoked_by_username", length = 100)
    private String revokedByUsername;

    @Column(name = "revoke_reason", columnDefinition = "TEXT")
    private String revokeReason;

    @Column(name = "verify_count")
    private int verifyCount;

    @Column(name = "failed_verify_count")
    private int failedVerifyCount;

    @Column(name = "preview_count")
    private int previewCount;

    @Column(name = "download_count")
    private int downloadCount;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "version", length = 10)
    private String version;

    public UnifiedShare() {
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

    public Long getSharedByUserId() {
        return sharedByUserId;
    }

    public void setSharedByUserId(Long sharedByUserId) {
        this.sharedByUserId = sharedByUserId;
    }

    public String getSharedByUsername() {
        return sharedByUsername;
    }

    public void setSharedByUsername(String sharedByUsername) {
        this.sharedByUsername = sharedByUsername;
    }

    public String getSharedByFullName() {
        return sharedByFullName;
    }

    public void setSharedByFullName(String sharedByFullName) {
        this.sharedByFullName = sharedByFullName;
    }

    public String getSharedByRole() {
        return sharedByRole;
    }

    public void setSharedByRole(String sharedByRole) {
        this.sharedByRole = sharedByRole;
    }

    public Long getSharedByDealerId() {
        return sharedByDealerId;
    }

    public void setSharedByDealerId(Long sharedByDealerId) {
        this.sharedByDealerId = sharedByDealerId;
    }

    public String getSharedByDealerName() {
        return sharedByDealerName;
    }

    public void setSharedByDealerName(String sharedByDealerName) {
        this.sharedByDealerName = sharedByDealerName;
    }

    public Long getSharedToUserId() {
        return sharedToUserId;
    }

    public void setSharedToUserId(Long sharedToUserId) {
        this.sharedToUserId = sharedToUserId;
    }

    public String getSharedToUsername() {
        return sharedToUsername;
    }

    public void setSharedToUsername(String sharedToUsername) {
        this.sharedToUsername = sharedToUsername;
    }

    public String getSharedToFullName() {
        return sharedToFullName;
    }

    public void setSharedToFullName(String sharedToFullName) {
        this.sharedToFullName = sharedToFullName;
    }

    public String getSharedToRole() {
        return sharedToRole;
    }

    public void setSharedToRole(String sharedToRole) {
        this.sharedToRole = sharedToRole;
    }

    public Long getSharedToDealerId() {
        return sharedToDealerId;
    }

    public void setSharedToDealerId(Long sharedToDealerId) {
        this.sharedToDealerId = sharedToDealerId;
    }

    public String getSharedToDealerName() {
        return sharedToDealerName;
    }

    public void setSharedToDealerName(String sharedToDealerName) {
        this.sharedToDealerName = sharedToDealerName;
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

    public Long getRevokedById() {
        return revokedById;
    }

    public void setRevokedById(Long revokedById) {
        this.revokedById = revokedById;
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
