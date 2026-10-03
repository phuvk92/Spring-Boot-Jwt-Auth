package com.example.svgmanager.dto.request;

import com.example.svgmanager.util.FlexibleIsoLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Body ghi nhận một lượt cắt đã hoàn thành xuống máy — POST /api/v1/cuts (F-38).
 * ⚠ Cố tình KHÔNG nhận trường hình học dưới bất kỳ dạng nào (F-38: lịch sử chỉ có thống kê).
 */
@Schema(description = "Yêu cầu ghi một lượt cắt đã xong — chỉ chứa số liệu thống kê (F-38)")
public class RecordCutRequest {

    @Schema(description = "Thời điểm gửi lệnh cắt xuống máy (ISO-8601 có hoặc không có offset/Z)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Thời điểm cắt không được để trống")
    @JsonDeserialize(using = FlexibleIsoLocalDateTimeDeserializer.class)
    private LocalDateTime cutAt;

    @Schema(description = "Nhãn nhóm chi tiết đã cắt", example = "Đèn trái + đèn phải")
    @Size(max = 255, message = "Nhãn chi tiết tối đa 255 ký tự")
    private String partLabel;

    @Schema(description = "Nhãn xe", example = "Mazda CX-5")
    @Size(max = 255, message = "Nhãn xe tối đa 255 ký tự")
    private String vehicleLabel;

    @Schema(description = "Nhãn phim hiển thị", example = "0,9 m")
    @Size(max = 100, message = "Nhãn phim tối đa 100 ký tự")
    private String filmUsage;

    @Schema(description = "Số mét phim đã dùng dạng số để tổng hợp thống kê", example = "0.900")
    @PositiveOrZero(message = "Số mét phim không được âm")
    private BigDecimal filmUsageMeters;

    @Schema(description = "Thời lượng cắt", example = "2′ 18″")
    @Size(max = 50, message = "Thời lượng tối đa 50 ký tự")
    private String duration;

    @Schema(description = "Mã bản làm việc (nếu cắt từ bản đã lưu)", example = "wd-2401")
    @Size(max = 255, message = "Mã thiết kế tối đa 255 ký tự")
    private String designId;

    @Schema(description = "Phiên bản của bản làm việc", example = "1")
    private Integer designVersion;

    public RecordCutRequest() {
    }

    public LocalDateTime getCutAt() { return cutAt; }
    public void setCutAt(LocalDateTime cutAt) { this.cutAt = cutAt; }
    public String getPartLabel() { return partLabel; }
    public void setPartLabel(String partLabel) { this.partLabel = partLabel; }
    public String getVehicleLabel() { return vehicleLabel; }
    public void setVehicleLabel(String vehicleLabel) { this.vehicleLabel = vehicleLabel; }
    public String getFilmUsage() { return filmUsage; }
    public void setFilmUsage(String filmUsage) { this.filmUsage = filmUsage; }
    public BigDecimal getFilmUsageMeters() { return filmUsageMeters; }
    public void setFilmUsageMeters(BigDecimal filmUsageMeters) { this.filmUsageMeters = filmUsageMeters; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public String getDesignId() { return designId; }
    public void setDesignId(String designId) { this.designId = designId; }
    public Integer getDesignVersion() { return designVersion; }
    public void setDesignVersion(Integer designVersion) { this.designVersion = designVersion; }
}
