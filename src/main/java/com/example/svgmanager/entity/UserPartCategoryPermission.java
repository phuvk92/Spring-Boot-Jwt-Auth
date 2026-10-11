package com.example.svgmanager.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_part_category_permission", indexes = {
        @Index(name = "idx_upcp_user_id", columnList = "user_id"),
        @Index(name = "idx_upcp_part_category_id", columnList = "part_category_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_part_category_permission", columnNames = {"user_id", "part_category_id"})
})
@EntityListeners(AuditingEntityListener.class)
public class UserPartCategoryPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_category_id", nullable = false)
    private PartLibraryCategory partLibraryCategory;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    public UserPartCategoryPermission() {
    }

    public UserPartCategoryPermission(User user, PartLibraryCategory partLibraryCategory, String createdBy) {
        this.user = user;
        this.partLibraryCategory = partLibraryCategory;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public PartLibraryCategory getPartLibraryCategory() {
        return partLibraryCategory;
    }

    public void setPartLibraryCategory(PartLibraryCategory partLibraryCategory) {
        this.partLibraryCategory = partLibraryCategory;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
