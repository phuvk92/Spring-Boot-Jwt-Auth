package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Một mục trong dropdown của app thợ — {value, label} theo CatalogOption của hợp đồng.
 * {@code value} là id (dạng chuỗi), {@code label} là tên hiển thị.
 */
@Schema(description = "CatalogOption — value là id, label là chuỗi hiển thị")
public class CatalogOptionDto {

    private String value;
    private String label;

    public CatalogOptionDto() {
    }

    public CatalogOptionDto(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public static CatalogOptionDto of(Long id, String label) {
        return new CatalogOptionDto(id != null ? String.valueOf(id) : "", label);
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
