package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Catalog option matching 06-catalog-level.json contract")
public class CatalogOptionDto {

    @Schema(description = "Value identifier", example = "Ngoại thất")
    private String value;

    @Schema(description = "Display label in dropdown", example = "Ngoại thất")
    private String label;

    public CatalogOptionDto() {
    }

    public CatalogOptionDto(String value, String label) {
        this.value = value;
        this.label = label;
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
}
