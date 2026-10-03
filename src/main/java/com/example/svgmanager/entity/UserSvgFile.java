package com.example.svgmanager.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * File SVG do User lưu qua /api/internal/user-files.
 * Phục vụ xem riêng của User và quản lý toàn bộ của Admin trên Web Admin.
 */
@Entity
@Table(name = "user_svg_files", indexes = {
        @Index(name = "idx_user_svg_files_user_id", columnList = "user_id"),
        @Index(name = "idx_user_svg_files_dealer_id", columnList = "dealer_id"),
        @Index(name = "idx_user_svg_files_category_id", columnList = "category_id"),
        @Index(name = "idx_user_svg_files_vehicle_node_id", columnList = "vehicle_node_id"),
        @Index(name = "idx_user_svg_files_status", columnList = "status"),
        @Index(name = "idx_user_svg_files_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class UserSvgFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "stored_file_name", nullable = false, length = 255)
    private String storedFileName;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType = "image/svg+xml";

    @Column(nullable = false, length = 100)
    private String checksum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private FileCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_node_id")
    private VehicleNode vehicleNode;

    @Column(name = "product_group", length = 100)
    private String productGroup;

    @Column(name = "product_group_name", length = 255)
    private String productGroupName;

    @Column(name = "brand_name", length = 255)
    private String brandName;

    @Column(name = "model_name", length = 255)
    private String modelName;

    @Column(name = "year_from")
    private Integer yearFrom;

    @Column(name = "year_to")
    private Integer yearTo;

    @Column(name = "generation_code", length = 100)
    private String generationCode;

    @Column(name = "film_width")
    private Double filmWidth;

    @Column(name = "film_width_unit", length = 20)
    private String filmWidthUnit = "MM";

    @Column(name = "roll_length")
    private Double rollLength;

    @Column(name = "roll_length_unit", length = 20)
    private String rollLengthUnit = "MM";

    @Column(name = "axis_x")
    private Double axisX;

    @Column(name = "axis_y")
    private Double axisY;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_id")
    private Dealer dealer;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UserSvgFile() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String fileName;
        private String originalFileName;
        private String storedFileName;
        private String filePath;
        private Long fileSize;
        private String mimeType = "image/svg+xml";
        private String checksum;
        private FileCategory category;
        private VehicleNode vehicleNode;
        private String productGroup;
        private String productGroupName;
        private String brandName;
        private String modelName;
        private Integer yearFrom;
        private Integer yearTo;
        private String generationCode;
        private Double filmWidth;
        private String filmWidthUnit = "MM";
        private Double rollLength;
        private String rollLengthUnit = "MM";
        private Double axisX;
        private Double axisY;
        private String description;
        private String status = "ACTIVE";
        private User user;
        private Dealer dealer;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder originalFileName(String originalFileName) { this.originalFileName = originalFileName; return this; }
        public Builder storedFileName(String storedFileName) { this.storedFileName = storedFileName; return this; }
        public Builder filePath(String filePath) { this.filePath = filePath; return this; }
        public Builder fileSize(Long fileSize) { this.fileSize = fileSize; return this; }
        public Builder mimeType(String mimeType) { this.mimeType = mimeType; return this; }
        public Builder checksum(String checksum) { this.checksum = checksum; return this; }
        public Builder category(FileCategory category) { this.category = category; return this; }
        public Builder vehicleNode(VehicleNode vehicleNode) { this.vehicleNode = vehicleNode; return this; }
        public Builder productGroup(String productGroup) { this.productGroup = productGroup; return this; }
        public Builder productGroupName(String productGroupName) { this.productGroupName = productGroupName; return this; }
        public Builder brandName(String brandName) { this.brandName = brandName; return this; }
        public Builder modelName(String modelName) { this.modelName = modelName; return this; }
        public Builder yearFrom(Integer yearFrom) { this.yearFrom = yearFrom; return this; }
        public Builder yearTo(Integer yearTo) { this.yearTo = yearTo; return this; }
        public Builder generationCode(String generationCode) { this.generationCode = generationCode; return this; }
        public Builder filmWidth(Double filmWidth) { this.filmWidth = filmWidth; return this; }
        public Builder filmWidthUnit(String filmWidthUnit) { this.filmWidthUnit = filmWidthUnit; return this; }
        public Builder rollLength(Double rollLength) { this.rollLength = rollLength; return this; }
        public Builder rollLengthUnit(String rollLengthUnit) { this.rollLengthUnit = rollLengthUnit; return this; }
        public Builder axisX(Double axisX) { this.axisX = axisX; return this; }
        public Builder axisY(Double axisY) { this.axisY = axisY; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder dealer(Dealer dealer) { this.dealer = dealer; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public UserSvgFile build() {
            UserSvgFile f = new UserSvgFile();
            f.setId(this.id);
            f.setFileName(this.fileName);
            f.setOriginalFileName(this.originalFileName);
            f.setStoredFileName(this.storedFileName);
            f.setFilePath(this.filePath);
            f.setFileSize(this.fileSize);
            f.setMimeType(this.mimeType);
            f.setChecksum(this.checksum);
            f.setCategory(this.category);
            f.setVehicleNode(this.vehicleNode);
            f.setProductGroup(this.productGroup);
            f.setProductGroupName(this.productGroupName);
            f.setBrandName(this.brandName);
            f.setModelName(this.modelName);
            f.setYearFrom(this.yearFrom);
            f.setYearTo(this.yearTo);
            f.setGenerationCode(this.generationCode);
            f.setFilmWidth(this.filmWidth);
            f.setFilmWidthUnit(this.filmWidthUnit);
            f.setRollLength(this.rollLength);
            f.setRollLengthUnit(this.rollLengthUnit);
            f.setAxisX(this.axisX);
            f.setAxisY(this.axisY);
            f.setDescription(this.description);
            f.setStatus(this.status);
            f.setUser(this.user);
            f.setDealer(this.dealer);
            f.setCreatedAt(this.createdAt);
            f.setUpdatedAt(this.updatedAt);
            return f;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public String getStoredFileName() { return storedFileName; }
    public void setStoredFileName(String storedFileName) { this.storedFileName = storedFileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public FileCategory getCategory() { return category; }
    public void setCategory(FileCategory category) { this.category = category; }
    public VehicleNode getVehicleNode() { return vehicleNode; }
    public void setVehicleNode(VehicleNode vehicleNode) { this.vehicleNode = vehicleNode; }
    public String getProductGroup() { return productGroup; }
    public void setProductGroup(String productGroup) { this.productGroup = productGroup; }
    public String getProductGroupName() { return productGroupName; }
    public void setProductGroupName(String productGroupName) { this.productGroupName = productGroupName; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public Integer getYearFrom() { return yearFrom; }
    public void setYearFrom(Integer yearFrom) { this.yearFrom = yearFrom; }
    public Integer getYearTo() { return yearTo; }
    public void setYearTo(Integer yearTo) { this.yearTo = yearTo; }
    public String getGenerationCode() { return generationCode; }
    public void setGenerationCode(String generationCode) { this.generationCode = generationCode; }
    public Double getFilmWidth() { return filmWidth; }
    public void setFilmWidth(Double filmWidth) { this.filmWidth = filmWidth; }
    public String getFilmWidthUnit() { return filmWidthUnit; }
    public void setFilmWidthUnit(String filmWidthUnit) { this.filmWidthUnit = filmWidthUnit; }
    public Double getRollLength() { return rollLength; }
    public void setRollLength(Double rollLength) { this.rollLength = rollLength; }
    public String getRollLengthUnit() { return rollLengthUnit; }
    public void setRollLengthUnit(String rollLengthUnit) { this.rollLengthUnit = rollLengthUnit; }
    public Double getAxisX() { return axisX; }
    public void setAxisX(Double axisX) { this.axisX = axisX; }
    public Double getAxisY() { return axisY; }
    public void setAxisY(Double axisY) { this.axisY = axisY; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Dealer getDealer() { return dealer; }
    public void setDealer(Dealer dealer) { this.dealer = dealer; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
