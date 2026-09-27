package com.example.svgmanager.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Update Category Request")
public class UpdateCategoryRequest {

    @Schema(description = "Category value / code", example = "Ferrari", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Category value/code is required")
    @Size(max = 100, message = "Category value cannot exceed 100 characters")
    private String value;

    @Schema(description = "Category label / display name", example = "Ferrari")
    @Size(max = 255, message = "Category label cannot exceed 255 characters")
    private String label;

    @Schema(description = "Parent Category ID (null for root category)", example = "1")
    private Long parentId;

    @Schema(description = "Display order for sorting", example = "1")
    private Integer displayOrder = 0;

    @Schema(description = "Vehicle brand / make (Hãng xe)", example = "Toyota")
    @Size(max = 100, message = "Brand cannot exceed 100 characters")
    private String brand;

    @Schema(description = "Vehicle model / line (Dòng xe)", example = "Camry")
    @Size(max = 100, message = "Model cannot exceed 100 characters")
    private String model;

    @Schema(description = "Vehicle manufacturing year (Năm sản xuất)", example = "2024")
    @Size(max = 50, message = "Year cannot exceed 50 characters")
    private String year;

    public UpdateCategoryRequest() {
    }

    public UpdateCategoryRequest(String value, String label, Long parentId, Integer displayOrder) {
        this(value, label, parentId, displayOrder, null, null, null);
    }

    public UpdateCategoryRequest(String value, String label, Long parentId, Integer displayOrder, String brand, String model, String year) {
        this.value = value;
        this.label = label;
        this.parentId = parentId;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.brand = brand;
        this.model = model;
        this.year = year;
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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }
}
