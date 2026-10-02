package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Part trong hợp đồng openapi v0.3.0 (KX-32) — part bên trong một file thiết kế.
 * Tên trường khớp fixture {@code contracts/mock-samples/08-files-id-parts.json}.
 */
@Schema(description = "Part matching 08-files-id-parts.json contract")
public class PartDto {

    @Schema(description = "Id part trong phạm vi file (giữ dấu tiếng Việt)", example = "đèn-trái")
    private String id;

    @Schema(example = "Đèn trái")
    private String name;

    @Schema(description = "Vị trí trên xe — danh mục MỞ, chỉ hiển thị, không lọc", example = "Kính & đèn")
    private String zone;

    @Schema(description = "Chuỗi nguyên văn kèm đơn vị, server không cộng/đổi đơn vị", example = "0,24 m")
    private String filmUsage;

    private String note;

    @Schema(description = "Màu tô hiệu lực của part (#RRGGBB) đọc từ file SVG (NGO-415)", example = "#5CC6D0")
    private String color;

    public PartDto() {
    }

    public PartDto(String id, String name, String zone, String filmUsage, String note) {
        this(id, name, zone, filmUsage, note, null);
    }

    public PartDto(String id, String name, String zone, String filmUsage, String note, String color) {
        this.id = id;
        this.name = name;
        this.zone = zone;
        this.filmUsage = filmUsage;
        this.note = note;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getFilmUsage() {
        return filmUsage;
    }

    public void setFilmUsage(String filmUsage) {
        this.filmUsage = filmUsage;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
