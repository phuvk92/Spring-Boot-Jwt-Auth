package com.example.svgmanager.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_categories_parent_id", columnList = "parent_id"),
        @Index(name = "idx_categories_level", columnList = "level"),
        @Index(name = "idx_categories_value", columnList = "value")
})
@EntityListeners(AuditingEntityListener.class)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "\"value\"", nullable = false, length = 100)
    private String value;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(nullable = false, length = 50)
    private String level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = false)
    @OrderBy("displayOrder ASC, id ASC")
    private List<Category> children = new ArrayList<>();

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Category() {
    }

    public Category(String value, String label, String level, Category parent, Integer displayOrder) {
        this.value = value;
        this.label = label;
        this.level = level;
        this.parent = parent;
        this.children = new ArrayList<>();
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }

    public Category(Long id, String value, String label, String level, Category parent, List<Category> children, Integer displayOrder, LocalDateTime createdAt) {
        this.id = id;
        this.value = value;
        this.label = label;
        this.level = level;
        this.parent = parent;
        this.children = children != null ? children : new ArrayList<>();
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String value;
        private String label;
        private String level;
        private Category parent;
        private List<Category> children = new ArrayList<>();
        private Integer displayOrder = 0;
        private LocalDateTime createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder level(String level) {
            this.level = level;
            return this;
        }

        public Builder parent(Category parent) {
            this.parent = parent;
            return this;
        }

        public Builder children(List<Category> children) {
            this.children = children;
            return this;
        }

        public Builder displayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Category build() {
            return new Category(id, value, label, level, parent, children, displayOrder, createdAt);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Category getParent() {
        return parent;
    }

    public void setParent(Category parent) {
        this.parent = parent;
    }

    public List<Category> getChildren() {
        return children;
    }

    public void setChildren(List<Category> children) {
        this.children = children;
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
