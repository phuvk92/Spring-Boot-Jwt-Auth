package com.example.svgmanager.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Một bản làm việc đã lưu — schema SavedDesign của openapi v0.3.0 (F-37 · KX-02).
 * Đủ sáu cột cho bảng KX-02: tên bản lưu, xe, part, thời điểm sửa, khổ cắt, trạng thái.
 * CHỈ metadata — không trường hình học nào (F-38 chặn ở shape DTO).
 */
@Schema(description = "Bản làm việc của thợ trên cloud — bản sao riêng (D1), không phải mẫu trong kho")
public class SavedDesignResponse {

    @Schema(example = "wd-2401")
    private String id;

    @Schema(description = "DS-155 — mặc định tên mẫu gốc, bản thứ hai thêm hậu tố", example = "Abarth-695-2024")
    private String name;

    @Schema(example = "Abarth 695 · 2024")
    private String vehicleLabel;

    @Schema(description = "Ngoại thất | Nội thất | Window film")
    private String category;

    @Schema(description = "Con trỏ mẫu gốc (DS-84b); null = nhập từ file ngoài (F-33)", nullable = true)
    private String sourceTemplateId;

    @Schema(nullable = true)
    private String sourceTemplateName;

    @Schema(description = "F-58 ⚠ D8 — mẫu gốc đã đổi kể từ lúc sao")
    private boolean sourceTemplateChanged;

    @Schema(description = "DS-08c — một bản vẽ là cả tập part")
    private int partCount;

    @Schema(description = "Chuỗi nguyên văn kèm đơn vị — chờ A6a", example = "6,46 m")
    private String filmUsage;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Khổ cắt đã xếp lúc lưu — chuỗi nguyên văn (DS-156)", example = "1500 × 1500")
    private String cutArea;

    private boolean hasBeenCut;

    @Schema(example = "MAY-XUONG-01")
    private String lastSavedByDeviceName;

    private int versionCount;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVehicleLabel() { return vehicleLabel; }
    public void setVehicleLabel(String vehicleLabel) { this.vehicleLabel = vehicleLabel; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSourceTemplateId() { return sourceTemplateId; }
    public void setSourceTemplateId(String v) { this.sourceTemplateId = v; }
    public String getSourceTemplateName() { return sourceTemplateName; }
    public void setSourceTemplateName(String v) { this.sourceTemplateName = v; }
    public boolean isSourceTemplateChanged() { return sourceTemplateChanged; }
    public void setSourceTemplateChanged(boolean v) { this.sourceTemplateChanged = v; }
    public int getPartCount() { return partCount; }
    public void setPartCount(int partCount) { this.partCount = partCount; }
    public String getFilmUsage() { return filmUsage; }
    public void setFilmUsage(String filmUsage) { this.filmUsage = filmUsage; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCutArea() { return cutArea; }
    public void setCutArea(String cutArea) { this.cutArea = cutArea; }
    public boolean isHasBeenCut() { return hasBeenCut; }
    public void setHasBeenCut(boolean hasBeenCut) { this.hasBeenCut = hasBeenCut; }
    public String getLastSavedByDeviceName() { return lastSavedByDeviceName; }
    public void setLastSavedByDeviceName(String v) { this.lastSavedByDeviceName = v; }
    public int getVersionCount() { return versionCount; }
    public void setVersionCount(int versionCount) { this.versionCount = versionCount; }
}
