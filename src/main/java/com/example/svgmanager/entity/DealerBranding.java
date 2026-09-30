package com.example.svgmanager.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Cấu hình thương hiệu pha 2 của một đại lý (BR-10 — GET /api/v1/branding).
 * Một đại lý tối đa một dòng. Cột đều nullable: NULL = "đại lý chưa đặt" → tầng
 * service rơi về thương hiệu sản phẩm Pcut (BR-32), công tắc bảo mật nghiêng về an toàn.
 */
@Entity
@Table(name = "dealer_branding")
@EntityListeners(AuditingEntityListener.class)
public class DealerBranding {

    @Id
    @Column(name = "dealer_id")
    private Long dealerId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "dealer_id", nullable = false)
    private Dealer dealer;

    /** Tên ngắn hiện trên app, khác tên pháp nhân đầy đủ (BR-23). */
    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(length = 255)
    private String slogan;

    @Column(length = 50)
    private String hotline;

    /** Một trong 5 màu cố định của thiết kế (BR-40) — CHECK trong migration V15. */
    @Column(name = "primary_color", length = 7)
    private String primaryColor;

    @Column(name = "secondary_color", length = 7)
    private String secondaryColor;

    /** {@code light | dark | system} — CHECK trong migration V15. */
    @Column(length = 10)
    private String theme;

    @Column(name = "allow_worker_theme_toggle")
    private Boolean allowWorkerThemeToggle;

    @Column(name = "show_dealer_name_next_to_logo")
    private Boolean showDealerNameNextToLogo;

    /** Công tắc bảo mật RB-06 — client hiểu "thiếu" là true, nên giữ NULL được an toàn. */
    @Column(name = "protect_screen_capture")
    private Boolean protectScreenCapture;

    /** PNG nền trong suốt, trần 1MB (BR-12) — CHECK trong migration V15. */
    @Column(name = "logo_png")
    private byte[] logoPng;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public DealerBranding() {
    }

    public Long getDealerId() { return dealerId; }
    public Dealer getDealer() { return dealer; }
    public void setDealer(Dealer dealer) { this.dealer = dealer; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getSlogan() { return slogan; }
    public void setSlogan(String slogan) { this.slogan = slogan; }
    public String getHotline() { return hotline; }
    public void setHotline(String hotline) { this.hotline = hotline; }
    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
    public String getSecondaryColor() { return secondaryColor; }
    public void setSecondaryColor(String secondaryColor) { this.secondaryColor = secondaryColor; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public Boolean getAllowWorkerThemeToggle() { return allowWorkerThemeToggle; }
    public void setAllowWorkerThemeToggle(Boolean allowWorkerThemeToggle) { this.allowWorkerThemeToggle = allowWorkerThemeToggle; }
    public Boolean getShowDealerNameNextToLogo() { return showDealerNameNextToLogo; }
    public void setShowDealerNameNextToLogo(Boolean showDealerNameNextToLogo) { this.showDealerNameNextToLogo = showDealerNameNextToLogo; }
    public Boolean getProtectScreenCapture() { return protectScreenCapture; }
    public void setProtectScreenCapture(Boolean protectScreenCapture) { this.protectScreenCapture = protectScreenCapture; }
    public byte[] getLogoPng() { return logoPng; }
    public void setLogoPng(byte[] logoPng) { this.logoPng = logoPng; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
