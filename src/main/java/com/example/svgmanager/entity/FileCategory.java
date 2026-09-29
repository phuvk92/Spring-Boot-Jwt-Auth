package com.example.svgmanager.entity;

import jakarta.persistence.*;

/**
 * Danh mục file (Ngoại thất · Nội thất · Window film · Đèn & kính) — thay enum cứng
 * của client, seed ở V14.
 */
@Entity
@Table(name = "file_categories")
public class FileCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(nullable = false)
    private Boolean active = true;

    public FileCategory() {
    }

    public FileCategory(String name, Integer displayOrder) {
        this.name = name;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
