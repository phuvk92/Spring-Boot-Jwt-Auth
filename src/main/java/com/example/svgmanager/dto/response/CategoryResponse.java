package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Hierarchical Category catalog response")
public class CategoryResponse {

    @Schema(description = "Category ID", example = "1")
    private Long id;

    @Schema(description = "Category value", example = "Ngoại thất")
    private String value;

    @Schema(description = "Category display label", example = "Ngoại thất")
    private String label;

    @Schema(description = "Category hierarchy level", example = "category")
    private String level;

    @Schema(description = "Parent Category ID", example = "null")
    private Long parentId;

    @Schema(description = "Display order", example = "1")
    private Integer displayOrder;

    @Schema(description = "Vehicle brand / make (Hãng xe)", example = "Toyota")
    private String brand;

    @Schema(description = "Vehicle model / line (Dòng xe)", example = "Camry")
    private String model;

    @Schema(description = "Vehicle manufacturing year (Năm sản xuất)", example = "2024")
    private String year;

    @Schema(description = "Children subcategories")
    private List<CategoryResponse> children = new ArrayList<>();

    public CategoryResponse() {
    }

    public CategoryResponse(Long id, String value, String label, String level, Long parentId, Integer displayOrder, List<CategoryResponse> children) {
        this(id, value, label, level, parentId, displayOrder, children, null, null, null);
    }

    public CategoryResponse(Long id, String value, String label, String level, Long parentId, Integer displayOrder, List<CategoryResponse> children, String brand, String model, String year) {
        this.id = id;
        this.value = value;
        this.label = label;
        this.level = level;
        this.parentId = parentId;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.children = children != null ? children : new ArrayList<>();
        this.brand = brand;
        this.model = model;
        this.year = year;
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

    public List<CategoryResponse> getChildren() {
        return children;
    }

    public void setChildren(List<CategoryResponse> children) {
        this.children = children;
    }
}
