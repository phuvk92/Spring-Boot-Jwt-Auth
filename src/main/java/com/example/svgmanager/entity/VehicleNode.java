package com.example.svgmanager.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Một nút trong cây xe 4 cấp (V14). BRAND là gốc (parent_id NULL), cấp con = cấp cha + 1 —
 * hai ràng buộc đó kiểm ở service, DB chỉ giữ CHECK level + UNIQUE (cha, tên).
 */
@Entity
@Table(name = "vehicle_nodes", indexes = {
        @Index(name = "idx_vehicle_nodes_parent_id", columnList = "parent_id"),
        @Index(name = "idx_vehicle_nodes_level", columnList = "level")
})
@EntityListeners(AuditingEntityListener.class)
public class VehicleNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private VehicleNode parent;

    @OneToMany(mappedBy = "parent")
    @OrderBy("displayOrder ASC, id ASC")
    private List<VehicleNode> children = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VehicleNodeLevel level;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public VehicleNode() {
    }

    public VehicleNode(VehicleNode parent, VehicleNodeLevel level, String name, Integer displayOrder) {
        this.parent = parent;
        this.level = level;
        this.name = name;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public VehicleNode getParent() {
        return parent;
    }

    public void setParent(VehicleNode parent) {
        this.parent = parent;
    }

    public List<VehicleNode> getChildren() {
        return children;
    }

    public void setChildren(List<VehicleNode> children) {
        this.children = children;
    }

    public VehicleNodeLevel getLevel() {
        return level;
    }

    public void setLevel(VehicleNodeLevel level) {
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
