package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Category summary embedded in SVG details")
public class CategorySummaryResponse {

    @Schema(description = "Category ID", example = "14")
    private Long id;

    @Schema(description = "Category name / label", example = "Hatchback 3 cửa")
    private String name;

    @Schema(description = "Category value", example = "Hatchback 3 cửa")
    private String value;

    @Schema(description = "Category level", example = "submodel")
    private String level;

    @Schema(description = "Full hierarchical path", example = "Ngoại thất / Abarth / 695 / 695 / 2024 / Hatchback 3 cửa")
    private String fullPath;

    @Schema(description = "Vehicle brand / make (Hãng xe)", example = "Toyota")
    private String brand;

    @Schema(description = "Vehicle model / line (Dòng xe)", example = "Camry")
    private String model;

    @Schema(description = "Vehicle manufacturing year (Năm sản xuất)", example = "2024")
    private String year;

    public CategorySummaryResponse() {
    }

    public CategorySummaryResponse(Long id, String name, String value, String level, String fullPath) {
        this(id, name, value, level, fullPath, null, null, null);
    }

    public CategorySummaryResponse(Long id, String name, String value, String level, String fullPath, String brand, String model, String year) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.level = level;
        this.fullPath = fullPath;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
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
