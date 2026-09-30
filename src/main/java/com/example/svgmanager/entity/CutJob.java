package com.example.svgmanager.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Một lần gửi lệnh cắt xuống máy — bản ghi cho lịch sử cắt (F-38 · KX-03).
 *
 * Cố tình KHÔNG có trường hình học: ràng buộc F-38 quy định đại lý chỉ xem số liệu,
 * chặn ở tầng dữ liệu chứ không chỉ giấu ở màn hình.
 *
 * Scope là MÁY ({@link UserDevice}), không phải user: endpoint "lịch sử cắt trên máy
 * này" định danh máy qua claim {@code sid} của token.
 */
@Entity
@Table(name = "cut_jobs", indexes = {
        @Index(name = "idx_cut_jobs_device_at", columnList = "user_device_id, cut_at")
})
public class CutJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_device_id", nullable = false)
    private UserDevice userDevice;

    /** Thời điểm job được gửi xuống máy (client báo), khớp `at` trong hợp đồng. */
    @Column(name = "cut_at", nullable = false)
    private LocalDateTime cutAt;

    /** Nhãn nhóm chi tiết đã cắt — null khi nguồn không báo. */
    @Column(name = "part_label", length = 255)
    private String partLabel;

    @Column(name = "vehicle_label", length = 255)
    private String vehicleLabel;

    /** Nhãn phim nguyên văn client báo (vd "0,9 m") — chuỗi hiển thị, không tính toán. */
    @Column(name = "film_usage", length = 100)
    private String filmUsage;

    /**
     * Số đo phim dạng số (mét) để tổng hợp stats.filmUsed. NULLABLE — đại lượng đo
     * chính xác còn treo ở câu A6a với khách; thiếu nguồn thì stats trả null,
     * không bịa số.
     */
    @Column(name = "film_usage_meters", precision = 10, scale = 3)
    private BigDecimal filmUsageMeters;

    /** Thời lượng cắt — chuỗi nguyên văn (vd "2′ 18″"), giống filmUsage. */
    @Column(name = "duration", length = 50)
    private String duration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CutOutcome outcome = CutOutcome.COMPLETED;

    /** Con trỏ mỏng về bản đã lưu — F-38 "mở lại đúng bản đã cắt"; nối ở task designs. */
    @Column(name = "design_id", length = 255)
    private String designId;

    @Column(name = "design_version")
    private Integer designVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public CutJob() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserDevice getUserDevice() { return userDevice; }
    public void setUserDevice(UserDevice userDevice) { this.userDevice = userDevice; }
    public LocalDateTime getCutAt() { return cutAt; }
    public void setCutAt(LocalDateTime cutAt) { this.cutAt = cutAt; }
    public String getPartLabel() { return partLabel; }
    public void setPartLabel(String partLabel) { this.partLabel = partLabel; }
    public String getVehicleLabel() { return vehicleLabel; }
    public void setVehicleLabel(String vehicleLabel) { this.vehicleLabel = vehicleLabel; }
    public String getFilmUsage() { return filmUsage; }
    public void setFilmUsage(String filmUsage) { this.filmUsage = filmUsage; }
    public BigDecimal getFilmUsageMeters() { return filmUsageMeters; }
    public void setFilmUsageMeters(BigDecimal filmUsageMeters) { this.filmUsageMeters = filmUsageMeters; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public CutOutcome getOutcome() { return outcome; }
    public void setOutcome(CutOutcome outcome) { this.outcome = outcome; }
    public String getDesignId() { return designId; }
    public void setDesignId(String designId) { this.designId = designId; }
    public Integer getDesignVersion() { return designVersion; }
    public void setDesignVersion(Integer designVersion) { this.designVersion = designVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
