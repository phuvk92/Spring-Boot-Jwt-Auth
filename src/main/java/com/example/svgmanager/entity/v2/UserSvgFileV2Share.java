package com.example.svgmanager.entity.v2;

import com.example.svgmanager.entity.User;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Quản lý quyền chia sẻ file SVG V2 giữa các User mà không sao chép file vật lý.
 * Độc lập hoàn toàn với UserSvgFileShare V1.
 */
@Entity
@Table(
        name = "user_svg_file_v2_share",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_svg_file_v2_share", columnNames = {"user_svg_file_v2_id", "shared_to_user_id"})
        },
        indexes = {
                @Index(name = "idx_user_svg_file_v2_share_to_user", columnList = "shared_to_user_id, status"),
                @Index(name = "idx_user_svg_file_v2_share_file", columnList = "user_svg_file_v2_id, status")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class UserSvgFileV2Share {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_svg_file_v2_id", nullable = false)
    private UserSvgFileV2 userSvgFile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shared_to_user_id", nullable = false)
    private User sharedToUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shared_by_user_id", nullable = false)
    private User sharedByUser;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UserSvgFileV2Share() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserSvgFileV2 getUserSvgFile() { return userSvgFile; }
    public void setUserSvgFile(UserSvgFileV2 userSvgFile) { this.userSvgFile = userSvgFile; }
    public User getSharedToUser() { return sharedToUser; }
    public void setSharedToUser(User sharedToUser) { this.sharedToUser = sharedToUser; }
    public User getSharedByUser() { return sharedByUser; }
    public void setSharedByUser(User sharedByUser) { this.sharedByUser = sharedByUser; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class Builder {
        private final UserSvgFileV2Share target = new UserSvgFileV2Share();

        public Builder userSvgFile(UserSvgFileV2 userSvgFile) { target.setUserSvgFile(userSvgFile); return this; }
        public Builder sharedToUser(User sharedToUser) { target.setSharedToUser(sharedToUser); return this; }
        public Builder sharedByUser(User sharedByUser) { target.setSharedByUser(sharedByUser); return this; }
        public Builder status(String status) { target.setStatus(status); return this; }

        public UserSvgFileV2Share build() {
            return target;
        }
    }
}
