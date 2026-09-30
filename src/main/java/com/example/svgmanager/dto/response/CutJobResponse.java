package com.example.svgmanager.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Một bản ghi trong lịch sử cắt — schema CutJob của openapi v0.3.0.
 *
 * ⚠ F-38: CHỈ số liệu. Tuyệt đối không thêm trường hình học/biên dạng/path vào đây —
 * đại lý chỉ được xem số liệu, ràng buộc chặn ở shape DTO chứ không ở màn hình client.
 *
 * `id` và `deviceName` ngoài hợp đồng tối thiểu (id, thời điểm, máy theo AC của issue);
 * trường thêm là tương thích tiến, client bản cũ bỏ qua.
 */
@Schema(description = "Một job đã gửi xuống máy — cố tình không có trường hình học nào (F-38)")
public class CutJobResponse {

    @Schema(description = "Id bản ghi", example = "42")
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Thời điểm gửi lệnh cắt")
    private LocalDateTime at;

    @Schema(description = "Tên máy đã cắt (user_devices.device_name)", example = "Máy xưởng 1")
    private String deviceName;

    @Schema(description = "Nhãn nhóm chi tiết", example = "Đèn trái + đèn phải")
    private String partLabel;

    @Schema(description = "Nhãn xe", example = "Mazda CX-5")
    private String vehicleLabel;

    @Schema(description = "Phim đã dùng — chuỗi nguyên văn kèm đơn vị", example = "0,9 m")
    private String filmUsage;

    @Schema(description = "Thời lượng cắt — chuỗi nguyên văn", example = "2′ 18″")
    private String duration;

    @Schema(description = "Kết quả: completed · recut · misaligned", example = "completed")
    private String outcome;

    public CutJobResponse() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getAt() { return at; }
    public void setAt(LocalDateTime at) { this.at = at; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getPartLabel() { return partLabel; }
    public void setPartLabel(String partLabel) { this.partLabel = partLabel; }
    public String getVehicleLabel() { return vehicleLabel; }
    public void setVehicleLabel(String vehicleLabel) { this.vehicleLabel = vehicleLabel; }
    public String getFilmUsage() { return filmUsage; }
    public void setFilmUsage(String filmUsage) { this.filmUsage = filmUsage; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
}
