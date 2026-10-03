package com.example.svgmanager.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Nội dung một lần Lưu bản làm việc — schema DesignSaveRequest của openapi v0.3.0 (F-36 · DS-05).
 * Gồm siêu dữ liệu của bản làm việc và toàn bộ bố cục trong payload (DS-156).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Nội dung một lần Lưu bản làm việc (F-36)")
public class DesignSaveRequest {

    @NotBlank(message = "Tên bản làm việc không được để trống")
    @Schema(description = "Tên bản làm việc (DS-155)", example = "Abarth-695-2024")
    private String name;

    @Schema(description = "Nhóm chi tiết (Ngoại thất, Nội thất, Window film)", nullable = true)
    private String category;

    @Schema(description = "Nhãn xe lúc sao", nullable = true, example = "Abarth 695 · 2024")
    private String vehicleLabel;

    @Schema(description = "Con trỏ về mẫu gốc trong kho (DS-84b); null với bản nhập ngoài", nullable = true)
    private String sourceTemplateId;

    @Schema(description = "Mốc updatedAt của mẫu gốc lúc sao", nullable = true)
    private LocalDateTime sourceTemplateUpdatedAt;

    /**
     * Payload chứa bố cục / dữ liệu mã hoá (CL-39).
     * Server chấp nhận cả Object json lẫn chuỗi base64/mã hoá hoặc raw JSON node.
     */
    @NotNull(message = "Payload không được để trống")
    @Schema(description = "Bố cục đầy đủ của bản vẽ (JSON object hoặc chuỗi mã hoá CL-39)")
    private JsonNode payload;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getVehicleLabel() { return vehicleLabel; }
    public void setVehicleLabel(String vehicleLabel) { this.vehicleLabel = vehicleLabel; }
    public String getSourceTemplateId() { return sourceTemplateId; }
    public void setSourceTemplateId(String sourceTemplateId) { this.sourceTemplateId = sourceTemplateId; }
    public LocalDateTime getSourceTemplateUpdatedAt() { return sourceTemplateUpdatedAt; }
    public void setSourceTemplateUpdatedAt(LocalDateTime sourceTemplateUpdatedAt) { this.sourceTemplateUpdatedAt = sourceTemplateUpdatedAt; }
    public JsonNode getPayload() { return payload; }
    public void setPayload(JsonNode payload) { this.payload = payload; }
}
