package com.example.svgmanager.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "svg_file_dealer_permissions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_svg_dealer_permission", columnNames = {"svg_file_id", "dealer_id"})
}, indexes = {
        @Index(name = "idx_svg_dp_svg_file_id", columnList = "svg_file_id"),
        @Index(name = "idx_svg_dp_dealer_id", columnList = "dealer_id"),
        @Index(name = "idx_svg_dp_view_download", columnList = "dealer_id, can_view, can_download")
})
@EntityListeners(AuditingEntityListener.class)
public class SvgFileDealerPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "svg_file_id", nullable = false)
    private SvgFile svgFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_id", nullable = false)
    private Dealer dealer;

    @Column(name = "can_view", nullable = false)
    private boolean canView = true;

    @Column(name = "can_download", nullable = false)
    private boolean canDownload = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SvgFileDealerPermission() {
    }

    public SvgFileDealerPermission(Long id, SvgFile svgFile, Dealer dealer, boolean canView, boolean canDownload, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.svgFile = svgFile;
        this.dealer = dealer;
        this.canView = canView;
        this.canDownload = canDownload;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public SvgFileDealerPermission(SvgFile svgFile, Dealer dealer, boolean canView, boolean canDownload) {
        this.svgFile = svgFile;
        this.dealer = dealer;
        this.canView = canView;
        this.canDownload = canDownload;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SvgFile getSvgFile() {
        return svgFile;
    }

    public void setSvgFile(SvgFile svgFile) {
        this.svgFile = svgFile;
    }

    public Dealer getDealer() {
        return dealer;
    }

    public void setDealer(Dealer dealer) {
        this.dealer = dealer;
    }

    public boolean isCanView() {
        return canView;
    }

    public void setCanView(boolean canView) {
        this.canView = canView;
    }

    public boolean isCanDownload() {
        return canDownload;
    }

    public void setCanDownload(boolean canDownload) {
        this.canDownload = canDownload;
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
}
