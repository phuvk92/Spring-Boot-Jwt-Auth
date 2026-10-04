package com.example.svgmanager.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Thông tin chi tiết bản file SVG đã lưu của người dùng")
public class UserSavedFileResponse {

    @Schema(description = "ID của bản lưu", example = "50001")
    private Long id;

    @Schema(description = "Tên hiển thị của file SVG", example = "BMW_X5_G05_DOOR.svg")
    private String fileName;

    @Schema(description = "Tên file gốc lúc tải lên", example = "BMW_X5_G05_DOOR.svg")
    private String originalFileName;

    @Schema(description = "Danh mục file")
    private CategoryDto category;

    @Schema(description = "Cấu hình / mẫu xe liên kết")
    private VehicleConfigurationDto vehicleConfiguration;

    @Schema(description = "Kích thước / khổ cắt")
    private CutSizeDto cutSize;

    @Schema(description = "Mô tả / ghi chú", example = "Mẫu cắt cửa xe BMW X5 G05")
    private String description;

    @Schema(description = "Thời gian tạo")
    private LocalDateTime createdAt;

    @Schema(description = "Thời gian cập nhật")
    private LocalDateTime updatedAt;

    @Schema(description = "Người dùng tạo bản lưu")
    private UserSummaryDto createdBy;

    @Schema(description = "Đại lý của người dùng")
    private DealerSummaryDto dealer;

    @Schema(description = "Dung lượng file tính bằng bytes", example = "1258291")
    private Long fileSize;

    @Schema(description = "MIME Type", example = "image/svg+xml")
    private String mimeType;

    @Schema(description = "Mã băm SHA-256", example = "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    private String checksum;

    @Schema(description = "Trạng thái bản lưu", example = "ACTIVE")
    private String status;

    @Schema(description = "file_key của part file trong kho mà bản này được tạo từ", example = "audi-q6-2024-full")
    private String sourceFileKey;

    @Schema(description = "Tên part file nguồn trong kho nếu còn", example = "Audi Q6 2024.svg")
    private String sourceFileName;

    public UserSavedFileResponse() {
    }

    public static class CategoryDto {
        private Long id;
        private String name;

        public CategoryDto() {}
        public CategoryDto(Long id, String name) {
            this.id = id;
            this.name = name;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class VehicleConfigurationDto {
        private Long id;
        private String productGroup;
        private String productGroupName;
        private String brandName;
        private String modelName;
        private Integer yearFrom;
        private Integer yearTo;
        private String generationCode;

        public VehicleConfigurationDto() {}
        public VehicleConfigurationDto(Long id, String productGroup, String productGroupName,
                                       String brandName, String modelName, Integer yearFrom, Integer yearTo,
                                       String generationCode) {
            this.id = id;
            this.productGroup = productGroup;
            this.productGroupName = productGroupName;
            this.brandName = brandName;
            this.modelName = modelName;
            this.yearFrom = yearFrom;
            this.yearTo = yearTo;
            this.generationCode = generationCode;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
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
    }

    public static class CutSizeDto {
        private Double filmWidth;
        private String filmWidthUnit;
        private Double rollLength;
        private String rollLengthUnit;
        private Double axisX;
        private Double axisY;

        public CutSizeDto() {}
        public CutSizeDto(Double filmWidth, String filmWidthUnit, Double rollLength, String rollLengthUnit, Double axisX, Double axisY) {
            this.filmWidth = filmWidth;
            this.filmWidthUnit = filmWidthUnit;
            this.rollLength = rollLength;
            this.rollLengthUnit = rollLengthUnit;
            this.axisX = axisX;
            this.axisY = axisY;
        }
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
    }

    public static class UserSummaryDto {
        private Long id;
        private String username;
        private String displayName;

        public UserSummaryDto() {}
        public UserSummaryDto(Long id, String username, String displayName) {
            this.id = id;
            this.username = username;
            this.displayName = displayName;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
    }

    public static class DealerSummaryDto {
        private Long id;
        private String name;

        public DealerSummaryDto() {}
        public DealerSummaryDto(Long id, String name) {
            this.id = id;
            this.name = name;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public CategoryDto getCategory() { return category; }
    public void setCategory(CategoryDto category) { this.category = category; }
    public VehicleConfigurationDto getVehicleConfiguration() { return vehicleConfiguration; }
    public void setVehicleConfiguration(VehicleConfigurationDto vehicleConfiguration) { this.vehicleConfiguration = vehicleConfiguration; }
    public CutSizeDto getCutSize() { return cutSize; }
    public void setCutSize(CutSizeDto cutSize) { this.cutSize = cutSize; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public UserSummaryDto getCreatedBy() { return createdBy; }
    public void setCreatedBy(UserSummaryDto createdBy) { this.createdBy = createdBy; }
    public DealerSummaryDto getDealer() { return dealer; }
    public void setDealer(DealerSummaryDto dealer) { this.dealer = dealer; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSourceFileKey() { return sourceFileKey; }
    public void setSourceFileKey(String sourceFileKey) { this.sourceFileKey = sourceFileKey; }
    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }
}
