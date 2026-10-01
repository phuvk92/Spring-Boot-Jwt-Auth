package com.example.svgmanager.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * PartOutline trong hợp đồng openapi v0.3.0 (F-56 — GET /api/v1/files/{id}/geometry).
 * {@code pathData} là chuỗi đường dẫn SVG trong hệ toạ độ của chính part, gốc ở góc
 * trên-trái hộp bao — hình học HIỂN THỊ, lệnh cắt không sinh từ chuỗi này (RB-07).
 * Tên trường khớp fixture {@code contracts/mock-samples/09-files-id-geometry.json}.
 */
@Schema(description = "PartOutline matching 09-files-id-geometry.json contract")
public class PartOutlineDto {

    @Schema(description = "Id đầy đủ của part: <fileId>--<layout>--<partKey>, giữ dấu tiếng Việt",
            example = "abarth-695-695-2024-hatchback-3-cửa--full-body--nested--đèn-trái")
    private String partId;

    @Schema(example = "Đèn trái")
    private String name;

    @Schema(description = "Bố cục part: nested (đã xếp) hoặc raw (chưa xếp) — SA-DanhMucXe-v2 §8", example = "nested")
    private String layout;

    @Schema(description = "Đường dẫn SVG, gốc ở góc trên-trái hộp bao của part")
    private String pathData;

    @Schema(description = "Hộp bao, luôn mm (DS-86) — inch chỉ là lớp hiển thị client", example = "420")
    private Double widthMm;

    @Schema(example = "198")
    private Double heightMm;

    @Schema(description = "Vị trí hộp bao trên vùng cắt, mm, gốc trên-trái vùng cắt", example = "30")
    @JsonProperty("xMm")
    private Double xMm;

    @JsonProperty("yMm")
    private Double yMm;

    @Schema(description = "Cho dòng meta của part (DS-57)", example = "14")
    private Integer nodeCount;

    private Integer holeCount;

    public PartOutlineDto() {
    }

    public PartOutlineDto(String partId, String name, String pathData, Double widthMm,
                          Double heightMm, Double xMm, Double yMm,
                          Integer nodeCount, Integer holeCount) {
        this(partId, name, "nested", pathData, widthMm, heightMm, xMm, yMm, nodeCount, holeCount);
    }

    public PartOutlineDto(String partId, String name, String layout, String pathData, Double widthMm,
                          Double heightMm, Double xMm, Double yMm,
                          Integer nodeCount, Integer holeCount) {
        this.partId = partId;
        this.name = name;
        this.layout = layout;
        this.pathData = pathData;
        this.widthMm = widthMm;
        this.heightMm = heightMm;
        this.xMm = xMm;
        this.yMm = yMm;
        this.nodeCount = nodeCount;
        this.holeCount = holeCount;
    }

    public String getPartId() {
        return partId;
    }

    public void setPartId(String partId) {
        this.partId = partId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public String getPathData() {
        return pathData;
    }

    public void setPathData(String pathData) {
        this.pathData = pathData;
    }

    public Double getWidthMm() {
        return widthMm;
    }

    public void setWidthMm(Double widthMm) {
        this.widthMm = widthMm;
    }

    public Double getHeightMm() {
        return heightMm;
    }

    public void setHeightMm(Double heightMm) {
        this.heightMm = heightMm;
    }

    public Double getxMm() {
        return xMm;
    }

    public void setxMm(Double xMm) {
        this.xMm = xMm;
    }

    public Double getyMm() {
        return yMm;
    }

    public void setyMm(Double yMm) {
        this.yMm = yMm;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public Integer getHoleCount() {
        return holeCount;
    }

    public void setHoleCount(Integer holeCount) {
        this.holeCount = holeCount;
    }
}
