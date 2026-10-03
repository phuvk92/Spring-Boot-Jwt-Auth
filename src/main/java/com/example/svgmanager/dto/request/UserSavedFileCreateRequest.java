package com.example.svgmanager.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Yêu cầu lưu file SVG của người dùng")
public class UserSavedFileCreateRequest {

    @Schema(description = "Tên hiển thị file", example = "BMW_X5_G05_DOOR.svg")
    private String fileName;

    @Schema(description = "Nội dung SVG dạng văn bản (nếu gửi JSON thay vì multipart)", nullable = true)
    private String svgContent;

    @Schema(description = "ID danh mục (file_categories)", nullable = true, example = "1")
    private Long categoryId;

    @Schema(description = "ID cấu hình xe / vehicle node", nullable = true, example = "3")
    private Long vehicleNodeId;

    @Schema(description = "ID cấu hình xe (alias)", nullable = true)
    private Long vehicleConfigurationId;

    @Schema(description = "Hãng xe", nullable = true, example = "BMW")
    private String brandName;

    @Schema(description = "Dòng xe / Model", nullable = true, example = "X5")
    private String modelName;

    @Schema(description = "Năm từ", nullable = true, example = "2019")
    private Integer yearFrom;

    @Schema(description = "Năm đến", nullable = true, example = "2023")
    private Integer yearTo;

    @Schema(description = "Mã khung / thế hệ", nullable = true, example = "G05")
    private String generationCode;

    @Schema(description = "Mã nhóm sản phẩm", nullable = true, example = "PPF_EXTERIOR")
    private String productGroup;

    @Schema(description = "Tên nhóm sản phẩm", nullable = true, example = "PPF Exterior")
    private String productGroupName;

    @Schema(description = "Khổ phim (Y)", nullable = true, example = "1520")
    private Double filmWidth;

    @Schema(description = "Đơn vị khổ phim", defaultValue = "MM", example = "MM")
    private String filmWidthUnit = "MM";

    @Schema(description = "Dài cuộn (X)", nullable = true, example = "3500")
    private Double rollLength;

    @Schema(description = "Đơn vị dài cuộn", defaultValue = "MM", example = "MM")
    private String rollLengthUnit = "MM";

    @Schema(description = "Trục X", nullable = true, example = "3500")
    private Double axisX;

    @Schema(description = "Trục Y", nullable = true, example = "1520")
    private Double axisY;

    @Schema(description = "file_key của part file trong kho mà bản này được tạo từ", nullable = true, example = "audi-q6-2024-full")
    private String sourceFileKey;

    @Schema(description = "Mô tả / ghi chú", nullable = true, example = "Mẫu cắt cửa xe BMW X5 G05")
    private String description;

    public UserSavedFileCreateRequest() {}

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getSvgContent() { return svgContent; }
    public void setSvgContent(String svgContent) { this.svgContent = svgContent; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getVehicleNodeId() {
        return vehicleNodeId != null ? vehicleNodeId : vehicleConfigurationId;
    }
    public void setVehicleNodeId(Long vehicleNodeId) { this.vehicleNodeId = vehicleNodeId; }
    public Long getVehicleConfigurationId() { return vehicleConfigurationId; }
    public void setVehicleConfigurationId(Long vehicleConfigurationId) { this.vehicleConfigurationId = vehicleConfigurationId; }
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
    public String getProductGroup() { return productGroup; }
    public void setProductGroup(String productGroup) { this.productGroup = productGroup; }
    public String getProductGroupName() { return productGroupName; }
    public void setProductGroupName(String productGroupName) { this.productGroupName = productGroupName; }
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
}
