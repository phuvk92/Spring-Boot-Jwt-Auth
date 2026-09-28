package com.example.svgmanager.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DesignFile trong hợp đồng openapi v0.3.0 (KX-30) — một file thiết kế trong kho.
 * Tên trường khớp fixture {@code contracts/mock-samples/07-files.json} — đổi là client vỡ.
 */
@Schema(description = "DesignFile matching 07-files.json contract")
public class DesignFileDto {

    @Schema(description = "Id chuỗi (slug kèm khoá xe, giữ dấu tiếng Việt)", example = "abarth-695-695-2024-hatchback-3-cửa--full-body")
    private String id;

    @Schema(description = "Tên do đội nội dung đặt", example = "Ngoại thất — full body 7 mảnh")
    private String name;

    @Schema(description = "Nhóm chi tiết — cấp 1 của bộ lọc", example = "Ngoại thất")
    private String category;

    @Schema(description = "Số part trong file (DS-08c)", example = "7")
    private int partCount;

    @Schema(description = "Tổng phim cả file — chuỗi nguyên văn kèm đơn vị", example = "6,46 m")
    private String filmUsage;

    private String note;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Mẫu cập nhật lần cuối")
    private LocalDateTime updatedAt;

    public DesignFileDto() {
    }

    public DesignFileDto(String id, String name, String category, int partCount,
                         String filmUsage, String note, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.partCount = partCount;
        this.filmUsage = filmUsage;
        this.note = note;
        this.updatedAt = updatedAt;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getPartCount() {
        return partCount;
    }

    public void setPartCount(int partCount) {
        this.partCount = partCount;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
