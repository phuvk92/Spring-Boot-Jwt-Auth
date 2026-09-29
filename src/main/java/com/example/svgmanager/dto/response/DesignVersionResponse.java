package com.example.svgmanager.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Một lần lưu của cùng một bản làm việc — schema DesignVersion (F-37). Chỉ metadata.
 */
@Schema(description = "Một phiên bản của bản làm việc — chỉ metadata, không hình học")
public class DesignVersionResponse {

    @Schema(example = "wd-2401-v4")
    private String id;

    @Schema(description = "Số thứ tự tăng dần — thợ nói \"bản 12\" thay vì nói giờ")
    private int number;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime savedAt;

    @Schema(example = "MAY-XUONG-01")
    private String savedByDeviceName;

    @Schema(nullable = true)
    private String note;

    private boolean isCurrent;

    // Tên JSON phải là "isCurrent" theo hợp đồng — Jackson bóc "is" prefix ra "current",
    // nên phải gán @JsonProperty rõ ràng.

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }
    public LocalDateTime getSavedAt() { return savedAt; }
    public void setSavedAt(LocalDateTime savedAt) { this.savedAt = savedAt; }
    public String getSavedByDeviceName() { return savedByDeviceName; }
    public void setSavedByDeviceName(String v) { this.savedByDeviceName = v; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    @JsonProperty("isCurrent")
    public boolean isCurrent() { return isCurrent; }
    @JsonProperty("isCurrent")
    public void setCurrent(boolean current) { isCurrent = current; }
}
