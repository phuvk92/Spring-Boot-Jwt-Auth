package com.example.svgmanager.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Liên kết N:N file ↔ node xe (Q5 — một file gắn nhiều mẫu, kể cả khác hãng).
 * Node gắn phải là MODEL hoặc SUBTYPE (kiểm ở service khi gán). Cascade hai phía ở DB:
 * xoá file hay xoá node đều chỉ mất dòng liên kết (Q4).
 */
@Entity
@Table(name = "svg_file_vehicle_nodes")
@IdClass(SvgFileVehicleNodeId.class)
@EntityListeners(AuditingEntityListener.class)
public class SvgFileVehicleNode {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "svg_file_id", nullable = false)
    private SvgFile svgFile;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_node_id", nullable = false)
    private VehicleNode vehicleNode;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public SvgFileVehicleNode() {
    }

    public SvgFileVehicleNode(SvgFile svgFile, VehicleNode vehicleNode) {
        this.svgFile = svgFile;
        this.vehicleNode = vehicleNode;
    }

    public SvgFile getSvgFile() {
        return svgFile;
    }

    public void setSvgFile(SvgFile svgFile) {
        this.svgFile = svgFile;
    }

    public VehicleNode getVehicleNode() {
        return vehicleNode;
    }

    public void setVehicleNode(VehicleNode vehicleNode) {
        this.vehicleNode = vehicleNode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
