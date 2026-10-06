package com.example.svgmanager.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Quản lý quyền chia sẻ file SVG giữa các User mà không sao chép file vật lý.
 */
@Entity
@Table(
        name = "user_svg_file_share",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_svg_file_share", columnNames = {"user_svg_file_id", "shared_to_user_id"})
        },
        indexes = {
                @Index(name = "idx_user_svg_file_share_to_user", columnList = "shared_to_user_id, status"),
                @Index(name = "idx_user_svg_file_share_file", columnList = "user_svg_file_id, status")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class UserSvgFileShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_svg_file_id", nullable = false)
    private UserSvgFile userSvgFile;

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

    public UserSvgFileShare() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private UserSvgFile userSvgFile;
        private User sharedToUser;
        private User sharedByUser;
        private String status = "ACTIVE";
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder userSvgFile(UserSvgFile userSvgFile) { this.userSvgFile = userSvgFile; return this; }
        public Builder sharedToUser(User sharedToUser) { this.sharedToUser = sharedToUser; return this; }
        public Builder sharedByUser(User sharedByUser) { this.sharedByUser = sharedByUser; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public UserSvgFileShare build() {
            UserSvgFileShare share = new UserSvgFileShare();
            share.setId(this.id);
            share.setUserSvgFile(this.userSvgFile);
            share.setSharedToUser(this.sharedToUser);
            share.setSharedByUser(this.sharedByUser);
            share.setStatus(this.status);
            share.setCreatedAt(this.createdAt);
            share.setUpdatedAt(this.updatedAt);
            return share;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserSvgFile getUserSvgFile() { return userSvgFile; }
    public void setUserSvgFile(UserSvgFile userSvgFile) { this.userSvgFile = userSvgFile; }
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
}
