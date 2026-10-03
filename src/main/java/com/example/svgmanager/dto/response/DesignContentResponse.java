package com.example.svgmanager.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Nội dung bản làm việc để mở lại — GET /api/v1/designs/{id}/content?version=N (F-36).
 */
@Schema(description = "Nội dung bản làm việc để mở lại")
public class DesignContentResponse {

    @Schema(description = "Mã bản làm việc (design_key)", example = "wd-2401")
    private String id;

    @Schema(description = "Số thứ tự phiên bản", example = "1")
    private int version;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Thời điểm lưu phiên bản này")
    private LocalDateTime savedAt;

    /**
     * Chuỗi payload nguyên văn. Dùng @JsonRawValue nếu payload là JSON hợp lệ,
     * hoặc serialize dưới dạng String/JSON tuỳ thuộc.
     */
    @JsonRawValue
    @Schema(description = "Nội dung bố cục hoặc chuỗi mã hoá của bản làm việc")
    private String payload;

    public DesignContentResponse() {
    }

    public DesignContentResponse(String id, int version, LocalDateTime savedAt, String payload) {
        this.id = id;
        this.version = version;
        this.savedAt = savedAt;
        this.payload = payload;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public LocalDateTime getSavedAt() { return savedAt; }
    public void setSavedAt(LocalDateTime savedAt) { this.savedAt = savedAt; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}
