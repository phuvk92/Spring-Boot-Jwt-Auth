package com.example.svgmanager.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_configurations", indexes = {
        @Index(name = "idx_vc_category_id", columnList = "category_id"),
        @Index(name = "idx_vc_product_group", columnList = "product_group"),
        @Index(name = "idx_vc_brand_id", columnList = "brand_id"),
        @Index(name = "idx_vc_model_id", columnList = "model_id"),
        @Index(name = "idx_vc_year_from", columnList = "year_from"),
        @Index(name = "idx_vc_year_to", columnList = "year_to"),
        @Index(name = "idx_vc_status", columnList = "status"),
        @Index(name = "idx_vc_deleted", columnList = "deleted")
})
@EntityListeners(AuditingEntityListener.class)
public class VehicleConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_group", nullable = false, length = 50)
    private ProductGroup productGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private CarBrand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private CarModel model;

    @Column(name = "year_from", nullable = false)
    private Integer yearFrom;

    @Column(name = "year_to", nullable = false)
    private Integer yearTo;

    @Column(name = "generation_code", nullable = false, length = 100)
    private String generationCode;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(nullable = false)
    private boolean deleted = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public VehicleConfiguration() {
    }

    public VehicleConfiguration(Long id, Category category, ProductGroup productGroup, CarBrand brand, CarModel model,
                                Integer yearFrom, Integer yearTo, String generationCode, String status, boolean deleted,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.category = category;
        this.productGroup = productGroup;
        this.brand = brand;
        this.model = model;
        this.yearFrom = yearFrom;
        this.yearTo = yearTo;
        this.generationCode = generationCode;
        this.status = status != null ? status : "ACTIVE";
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Category category;
        private ProductGroup productGroup;
        private CarBrand brand;
        private CarModel model;
        private Integer yearFrom;
        private Integer yearTo;
        private String generationCode;
        private String status = "ACTIVE";
        private boolean deleted = false;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder category(Category category) {
            this.category = category;
            return this;
        }

        public Builder productGroup(ProductGroup productGroup) {
            this.productGroup = productGroup;
            return this;
        }

        public Builder brand(CarBrand brand) {
            this.brand = brand;
            return this;
        }

        public Builder model(CarModel model) {
            this.model = model;
            return this;
        }

        public Builder yearFrom(Integer yearFrom) {
            this.yearFrom = yearFrom;
            return this;
        }

        public Builder yearTo(Integer yearTo) {
            this.yearTo = yearTo;
            return this;
        }

        public Builder generationCode(String generationCode) {
            this.generationCode = generationCode;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder deleted(boolean deleted) {
            this.deleted = deleted;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public VehicleConfiguration build() {
            return new VehicleConfiguration(id, category, productGroup, brand, model, yearFrom, yearTo, generationCode, status, deleted, createdAt, updatedAt);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public ProductGroup getProductGroup() {
        return productGroup;
    }

    public void setProductGroup(ProductGroup productGroup) {
        this.productGroup = productGroup;
    }

    public CarBrand getBrand() {
        return brand;
    }

    public void setBrand(CarBrand brand) {
        this.brand = brand;
    }

    public CarModel getModel() {
        return model;
    }

    public void setModel(CarModel model) {
        this.model = model;
    }

    public Integer getYearFrom() {
        return yearFrom;
    }

    public void setYearFrom(Integer yearFrom) {
        this.yearFrom = yearFrom;
    }

    public Integer getYearTo() {
        return yearTo;
    }

    public void setYearTo(Integer yearTo) {
        this.yearTo = yearTo;
    }

    public String getGenerationCode() {
        return generationCode;
    }

    public void setGenerationCode(String generationCode) {
        this.generationCode = generationCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
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
