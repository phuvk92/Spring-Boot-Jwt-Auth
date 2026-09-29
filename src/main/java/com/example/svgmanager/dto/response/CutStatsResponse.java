package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Bốn ô thống kê của màn Lịch sử cắt — schema CutStats trong openapi v0.3.0.
 *
 * `filmUsed` có thể null: câu A6a (đại lượng "số đo cắt" chính xác) còn treo với khách,
 * nên khi không có số liệu nguồn dạng số thì trả null thay vì bịa con số tính tiền.
 * `period` null khi chưa có job nào.
 */
@Schema(description = "Thống kê tổng hợp của lịch sử cắt")
public class CutStatsResponse {

    @Schema(description = "Tổng số job", example = "12")
    private int jobCount;

    @Schema(description = "Tổng phim đã dùng — chuỗi nguyên văn; null khi thiếu số liệu nguồn (A6a)", example = "212 m")
    private String filmUsed;

    @Schema(description = "Số job cắt lại (recut — đếm riêng với misaligned)", example = "1")
    private int recutCount;

    @Schema(description = "Số xe khác nhau đã cắt", example = "8")
    private int vehicleCount;

    @Schema(description = "Khoảng ngày phủ của danh sách; null khi rỗng", example = "10/08 – 16/08/2026")
    private String period;

    public CutStatsResponse() {
    }

    public CutStatsResponse(int jobCount, String filmUsed, int recutCount, int vehicleCount, String period) {
        this.jobCount = jobCount;
        this.filmUsed = filmUsed;
        this.recutCount = recutCount;
        this.vehicleCount = vehicleCount;
        this.period = period;
    }

    public int getJobCount() { return jobCount; }
    public void setJobCount(int jobCount) { this.jobCount = jobCount; }
    public String getFilmUsed() { return filmUsed; }
    public void setFilmUsed(String filmUsed) { this.filmUsed = filmUsed; }
    public int getRecutCount() { return recutCount; }
    public void setRecutCount(int recutCount) { this.recutCount = recutCount; }
    public int getVehicleCount() { return vehicleCount; }
    public void setVehicleCount(int vehicleCount) { this.vehicleCount = vehicleCount; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
}
