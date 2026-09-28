package com.example.svgmanager.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Một part bên trong file thiết kế (KX-32 — {@code Part} trong openapi v0.3.0).
 * {@code zone} là danh mục MỞ do đội nội dung nhập, chỉ để hiển thị — không phải cấp
 * lọc như {@code category} nên cố tình không FK sang bảng categories.
 */
@Entity
@Table(name = "svg_file_parts", uniqueConstraints = {
        @UniqueConstraint(name = "uq_svg_file_part_key", columnNames = {"svg_file_id", "part_key"})
}, indexes = {
        @Index(name = "idx_svg_file_parts_file_id", columnList = "svg_file_id")
})
@EntityListeners(AuditingEntityListener.class)
public class SvgFilePart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "svg_file_id", nullable = false)
    private SvgFile svgFile;

    /** Id chuỗi trong hợp đồng (vd 'capo', 'đèn-trái') — duy nhất trong phạm vi một file. */
    @Column(name = "part_key", nullable = false, length = 100)
    private String partKey;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 100)
    private String zone;

    /** Chuỗi nguyên văn kèm đơn vị ('1,42 m') — server giữ nguyên, không cộng, không đổi đơn vị. */
    @Column(name = "film_usage", length = 100)
    private String filmUsage;

    @Column(columnDefinition = "TEXT")
    private String note;

    /** Thứ tự đội nội dung dựng — thứ tự trả về trong /files/{id}/parts. */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public SvgFilePart() {
    }

    public SvgFilePart(SvgFile svgFile, String partKey, String name, String zone,
                       String filmUsage, String note, Integer displayOrder) {
        this.svgFile = svgFile;
        this.partKey = partKey;
        this.name = name;
        this.zone = zone;
        this.filmUsage = filmUsage;
        this.note = note;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
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

    public String getPartKey() {
        return partKey;
    }

    public void setPartKey(String partKey) {
        this.partKey = partKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getFilmUsage() {
        return filmUsage;
    }

    public void setFilmUsage(String filmUsage) {
        this.filmUsage = filmUsage;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
