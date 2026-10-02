package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DesignFileGeometry trong hợp đồng openapi v0.3.0 (F-56 · KX-43 · DS-08c) —
 * MỘT lượt tải toàn bộ hình học của MỘT file cho MỘT tab Design Center.
 * Client giữ trong RAM, không ghi đĩa (RB-01) — phản hồi là JSON thường,
 * không Content-Disposition, không header khuyến khích cache xuống đĩa.
 */
@Schema(description = "DesignFileGeometry matching 09-files-id-geometry.json contract")
public class DesignFileGeometryDto {

    @Schema(example = "abarth-695-695-2024-hatchback-3-cửa--full-body")
    private String fileId;

    @Schema(example = "Ngoại thất — full body 7 mảnh")
    private String name;

    private List<PartOutlineDto> parts;

    @Schema(description = "Khổ cắt file khai (NGO-399); null → client dùng mặc định 15000 × 700", nullable = true)
    private CutAreaDto cutArea;

    public DesignFileGeometryDto() {
    }

    public DesignFileGeometryDto(String fileId, String name, List<PartOutlineDto> parts) {
        this.fileId = fileId;
        this.name = name;
        this.parts = parts;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PartOutlineDto> getParts() {
        return parts;
    }

    public void setParts(List<PartOutlineDto> parts) {
        this.parts = parts;
    }

    public CutAreaDto getCutArea() {
        return cutArea;
    }

    public void setCutArea(CutAreaDto cutArea) {
        this.cutArea = cutArea;
    }
}
