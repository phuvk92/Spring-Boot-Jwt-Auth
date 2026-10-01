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
        @UniqueConstraint(name = "uq_svg_file_part_layout_key", columnNames = {"svg_file_id", "layout", "part_key"})
}, indexes = {
        @Index(name = "idx_svg_file_parts_file_id", columnList = "svg_file_id"),
        @Index(name = "idx_svg_file_parts_layout", columnList = "svg_file_id, layout")
})
@EntityListeners(AuditingEntityListener.class)
public class SvgFilePart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "svg_file_id", nullable = false)
    private SvgFile svgFile;

    /** Bố cục part: NESTED (đã xếp) hoặc RAW (chưa xếp) — SA-DanhMucXe-v2 §8. */
    @Column(name = "layout", nullable = false, length = 8)
    private String layout = "NESTED";

    /** Id chuỗi trong hợp đồng (vd 'capo', 'đèn-trái') — duy nhất trong phạm vi (file, layout). */
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

    // ---- Hình học hiển thị (F-56 — PartOutline) ---------------------------------
    // pathData trong hệ toạ độ của chính part, gốc ở góc trên-trái hộp bao; kích
    // thước/vị trí luôn mm (DS-86). Lệnh cắt KHÔNG sinh từ chuỗi này (RB-07).

    /** Chuỗi đường dẫn SVG — hình học hiển thị, không phải lệnh cắt. */
    @Column(name = "path_data", columnDefinition = "TEXT")
    private String pathData;

    /** Hộp bao, mm. */
    @Column(name = "width_mm")
    private Double widthMm;

    /** Hộp bao, mm. */
    @Column(name = "height_mm")
    private Double heightMm;

    /** Vị trí hộp bao trên vùng cắt, mm, gốc trên-trái vùng cắt. */
    @Column(name = "x_mm")
    private Double xMm;

    /** Vị trí hộp bao trên vùng cắt, mm, gốc trên-trái vùng cắt. */
    @Column(name = "y_mm")
    private Double yMm;

    /** Cho dòng meta của part (DS-57). */
    @Column(name = "node_count")
    private Integer nodeCount;

    @Column(name = "hole_count")
    private Integer holeCount;

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

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
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

    public String getPathData() {
        return pathData;
    }

    public void setPathData(String pathData) {
        this.pathData = pathData;
    }

    public Double getWidthMm() {
        return widthMm;
    }

    public void setWidthMm(Double widthMm) {
        this.widthMm = widthMm;
    }

    public Double getHeightMm() {
        return heightMm;
    }

    public void setHeightMm(Double heightMm) {
        this.heightMm = heightMm;
    }

    public Double getXMm() {
        return xMm;
    }

    public void setXMm(Double xMm) {
        this.xMm = xMm;
    }

    public Double getYMm() {
        return yMm;
    }

    public void setYMm(Double yMm) {
        this.yMm = yMm;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public Integer getHoleCount() {
        return holeCount;
    }

    public void setHoleCount(Integer holeCount) {
        this.holeCount = holeCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
