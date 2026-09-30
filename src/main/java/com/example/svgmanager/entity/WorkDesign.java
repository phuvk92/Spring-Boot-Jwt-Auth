package com.example.svgmanager.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Một bản làm việc đã lưu của thợ — F-37 · KX-02, schema SavedDesign của openapi v0.3.0.
 *
 * Bản sao RIÊNG của thợ (chốt D1: mở mẫu = tạo bản sao có id riêng, DS-84b) — không
 * phải mẫu trong kho (svg_files), nên lọc theo {@link #owner} và không join kho.
 *
 * Chỉ metadata: hình học bản làm việc lưu ở đâu là việc của task lưu/tải sau,
 * endpoint đọc metadata không cần biết.
 */
@Entity
@Table(name = "work_designs", indexes = {
        @Index(name = "idx_work_designs_owner_updated", columnList = "owner_user_id, updated_at")
})
public class WorkDesign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Id chuỗi trong hợp đồng ("wd-2401") — cut_jobs.design_id trỏ tới cùng khoá này. */
    @Column(name = "design_key", nullable = false, unique = true, length = 255)
    private String designKey;

    /** Chủ sở hữu — bản làm việc là dữ liệu riêng của thợ, scope lọc bắt buộc. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "vehicle_label", length = 255)
    private String vehicleLabel;

    @Column(name = "category", length = 100)
    private String category;

    /** Con trỏ về mẫu gốc trong kho (DS-84b) — null khi nhập từ file ngoài (F-33). */
    @Column(name = "source_template_id", length = 255)
    private String sourceTemplateId;

    @Column(name = "source_template_name", length = 255)
    private String sourceTemplateName;

    /** F-58 ⚠ D8 — mẫu gốc đổi sau lúc sao; client hiện chỉ hiện nhãn. */
    @Column(name = "source_template_changed", nullable = false)
    private boolean sourceTemplateChanged;

    /** DS-08c — một bản vẽ là cả tập part, không phải một part. */
    @Column(name = "part_count", nullable = false)
    private int partCount;

    /** Chuỗi hiển thị nguyên văn kèm đơn vị — chờ A6a chốt đại lượng đo. */
    @Column(name = "film_usage", length = 100)
    private String filmUsage;

    /** Khổ cắt đã xếp lúc lưu — chuỗi hiển thị nguyên văn (DS-156), vd "1500 × 1500". */
    @Column(name = "cut_area", length = 100)
    private String cutArea;

    @Column(name = "has_been_cut", nullable = false)
    private boolean hasBeenCut;

    @Column(name = "last_saved_by_device_name", length = 255)
    private String lastSavedByDeviceName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "workDesign", fetch = FetchType.LAZY)
    private List<WorkDesignVersion> versions;

    public Long getId() { return id; }
    public String getDesignKey() { return designKey; }
    public void setDesignKey(String designKey) { this.designKey = designKey; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVehicleLabel() { return vehicleLabel; }
    public void setVehicleLabel(String vehicleLabel) { this.vehicleLabel = vehicleLabel; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSourceTemplateId() { return sourceTemplateId; }
    public void setSourceTemplateId(String sourceTemplateId) { this.sourceTemplateId = sourceTemplateId; }
    public String getSourceTemplateName() { return sourceTemplateName; }
    public void setSourceTemplateName(String sourceTemplateName) { this.sourceTemplateName = sourceTemplateName; }
    public boolean isSourceTemplateChanged() { return sourceTemplateChanged; }
    public void setSourceTemplateChanged(boolean v) { this.sourceTemplateChanged = v; }
    public int getPartCount() { return partCount; }
    public void setPartCount(int partCount) { this.partCount = partCount; }
    public String getFilmUsage() { return filmUsage; }
    public void setFilmUsage(String filmUsage) { this.filmUsage = filmUsage; }
    public String getCutArea() { return cutArea; }
    public void setCutArea(String cutArea) { this.cutArea = cutArea; }
    public boolean isHasBeenCut() { return hasBeenCut; }
    public void setHasBeenCut(boolean hasBeenCut) { this.hasBeenCut = hasBeenCut; }
    public String getLastSavedByDeviceName() { return lastSavedByDeviceName; }
    public void setLastSavedByDeviceName(String v) { this.lastSavedByDeviceName = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<WorkDesignVersion> getVersions() { return versions; }
}
