package com.example.svgmanager.entity.v2;

import com.example.svgmanager.entity.Dealer;
import com.example.svgmanager.entity.FileCategory;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.VehicleNode;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * File SVG do User lưu qua /api/internal/v2/user-files với mã hoá AES-256-GCM.
 * Độc lập hoàn toàn với UserSvgFile V1.
 */
@Entity
@Table(name = "user_svg_file_v2", indexes = {
        @Index(name = "idx_user_svg_file_v2_user_id", columnList = "user_id"),
        @Index(name = "idx_user_svg_file_v2_dealer_id", columnList = "dealer_id"),
        @Index(name = "idx_user_svg_file_v2_category_id", columnList = "category_id"),
        @Index(name = "idx_user_svg_file_v2_vehicle_node_id", columnList = "vehicle_node_id"),
        @Index(name = "idx_user_svg_file_v2_status", columnList = "status"),
        @Index(name = "idx_user_svg_file_v2_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class UserSvgFileV2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "storage_key", nullable = false, length = 1000)
    private String storageKey;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType = "image/svg+xml";

    @Column(name = "plaintext_checksum", nullable = false, length = 100)
    private String plaintextChecksum;

    @Column(name = "encrypted_checksum", nullable = false, length = 100)
    private String encryptedChecksum;

    // --- Encryption metadata (AES-256-GCM) ---
    @Column(name = "encryption_algorithm", nullable = false, length = 50)
    private String encryptionAlgorithm = "AES-256-GCM";

    @Column(name = "encryption_key_version", nullable = false, length = 50)
    private String encryptionKeyVersion = "v1";

    @Column(name = "encrypted_dek", nullable = false)
    private byte[] encryptedDek;

    @Column(name = "encryption_iv", nullable = false)
    private byte[] encryptionIv;

    // --- Business metadata ---
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

    @Column(name = "source_file_key", length = 255)
    private String sourceFileKey;

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

    public UserSvgFileV2() {
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getPlaintextChecksum() { return plaintextChecksum; }
    public void setPlaintextChecksum(String plaintextChecksum) { this.plaintextChecksum = plaintextChecksum; }
    public String getEncryptedChecksum() { return encryptedChecksum; }
    public void setEncryptedChecksum(String encryptedChecksum) { this.encryptedChecksum = encryptedChecksum; }
    public String getEncryptionAlgorithm() { return encryptionAlgorithm; }
    public void setEncryptionAlgorithm(String encryptionAlgorithm) { this.encryptionAlgorithm = encryptionAlgorithm; }
    public String getEncryptionKeyVersion() { return encryptionKeyVersion; }
    public void setEncryptionKeyVersion(String encryptionKeyVersion) { this.encryptionKeyVersion = encryptionKeyVersion; }
    public byte[] getEncryptedDek() { return encryptedDek; }
    public void setEncryptedDek(byte[] encryptedDek) { this.encryptedDek = encryptedDek; }
    public byte[] getEncryptionIv() { return encryptionIv; }
    public void setEncryptionIv(byte[] encryptionIv) { this.encryptionIv = encryptionIv; }
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
    public String getSourceFileKey() { return sourceFileKey; }
    public void setSourceFileKey(String sourceFileKey) { this.sourceFileKey = sourceFileKey; }
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

    public static class Builder {
        private final UserSvgFileV2 target = new UserSvgFileV2();

        public Builder fileName(String fileName) { target.setFileName(fileName); return this; }
        public Builder originalFileName(String originalFileName) { target.setOriginalFileName(originalFileName); return this; }
        public Builder storageKey(String storageKey) { target.setStorageKey(storageKey); return this; }
        public Builder filePath(String filePath) { target.setFilePath(filePath); return this; }
        public Builder fileSize(Long fileSize) { target.setFileSize(fileSize); return this; }
        public Builder mimeType(String mimeType) { target.setMimeType(mimeType); return this; }
        public Builder plaintextChecksum(String plaintextChecksum) { target.setPlaintextChecksum(plaintextChecksum); return this; }
        public Builder encryptedChecksum(String encryptedChecksum) { target.setEncryptedChecksum(encryptedChecksum); return this; }
        public Builder encryptionAlgorithm(String encryptionAlgorithm) { target.setEncryptionAlgorithm(encryptionAlgorithm); return this; }
        public Builder encryptionKeyVersion(String encryptionKeyVersion) { target.setEncryptionKeyVersion(encryptionKeyVersion); return this; }
        public Builder encryptedDek(byte[] encryptedDek) { target.setEncryptedDek(encryptedDek); return this; }
        public Builder encryptionIv(byte[] encryptionIv) { target.setEncryptionIv(encryptionIv); return this; }
        public Builder category(FileCategory category) { target.setCategory(category); return this; }
        public Builder vehicleNode(VehicleNode vehicleNode) { target.setVehicleNode(vehicleNode); return this; }
        public Builder productGroup(String productGroup) { target.setProductGroup(productGroup); return this; }
        public Builder productGroupName(String productGroupName) { target.setProductGroupName(productGroupName); return this; }
        public Builder brandName(String brandName) { target.setBrandName(brandName); return this; }
        public Builder modelName(String modelName) { target.setModelName(modelName); return this; }
        public Builder yearFrom(Integer yearFrom) { target.setYearFrom(yearFrom); return this; }
        public Builder yearTo(Integer yearTo) { target.setYearTo(yearTo); return this; }
        public Builder generationCode(String generationCode) { target.setGenerationCode(generationCode); return this; }
        public Builder filmWidth(Double filmWidth) { target.setFilmWidth(filmWidth); return this; }
        public Builder filmWidthUnit(String filmWidthUnit) { target.setFilmWidthUnit(filmWidthUnit); return this; }
        public Builder rollLength(Double rollLength) { target.setRollLength(rollLength); return this; }
        public Builder rollLengthUnit(String rollLengthUnit) { target.setRollLengthUnit(rollLengthUnit); return this; }
        public Builder axisX(Double axisX) { target.setAxisX(axisX); return this; }
        public Builder axisY(Double axisY) { target.setAxisY(axisY); return this; }
        public Builder sourceFileKey(String sourceFileKey) { target.setSourceFileKey(sourceFileKey); return this; }
        public Builder description(String description) { target.setDescription(description); return this; }
        public Builder status(String status) { target.setStatus(status); return this; }
        public Builder user(User user) { target.setUser(user); return this; }
        public Builder dealer(Dealer dealer) { target.setDealer(dealer); return this; }

        public UserSvgFileV2 build() {
            return target;
        }
    }
}
