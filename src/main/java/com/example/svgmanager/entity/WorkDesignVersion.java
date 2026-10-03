package com.example.svgmanager.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Một lần lưu của cùng một bản làm việc — F-37, schema DesignVersion của openapi v0.3.0.
 *
 * `isCurrent` là flag do task lưu/khôi phục duy trì — KHÔNG suy ra từ max(number):
 * khôi phục bản cũ đổi phiên bản hiện hành mà không xoá bản mới hơn.
 */
@Entity
@Table(name = "work_design_versions", indexes = {
        @Index(name = "idx_work_design_versions_design", columnList = "work_design_id, number")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_work_design_version_number", columnNames = {"work_design_id", "number"})
})
public class WorkDesignVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_design_id", nullable = false)
    private WorkDesign workDesign;

    /** Số thứ tự tăng dần — thợ nói "bản 12" thay vì nói giờ. */
    @Column(name = "number", nullable = false)
    private int number;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    @Column(name = "saved_by_device_name", length = 255)
    private String savedByDeviceName;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    /** Nội dung đã mã hoá / dữ liệu layout (CL-39, F-36). Server không giải mã. */
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    /** Dung lượng payload tính bằng byte (F-36). */
    @Column(name = "payload_size")
    private Integer payloadSize;

    public Long getId() { return id; }
    public WorkDesign getWorkDesign() { return workDesign; }
    public void setWorkDesign(WorkDesign workDesign) { this.workDesign = workDesign; }
    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }
    public LocalDateTime getSavedAt() { return savedAt; }
    public void setSavedAt(LocalDateTime savedAt) { this.savedAt = savedAt; }
    public String getSavedByDeviceName() { return savedByDeviceName; }
    public void setSavedByDeviceName(String v) { this.savedByDeviceName = v; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Integer getPayloadSize() { return payloadSize; }
    public void setPayloadSize(Integer payloadSize) { this.payloadSize = payloadSize; }
}
