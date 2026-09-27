package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public class DealerStatsResponse {

    @Schema(description = "Tổng số đại lý", example = "38")
    private long total;

    @Schema(description = "Số đại lý đang hoạt động", example = "35")
    private long active;

    @Schema(description = "Số đại lý sắp hết hạn", example = "2")
    private long expiring;

    @Schema(description = "Số đại lý tạm khoá", example = "1")
    private long locked;

    public DealerStatsResponse() {
    }

    public DealerStatsResponse(long total, long active, long expiring, long locked) {
        this.total = total;
        this.active = active;
        this.expiring = expiring;
        this.locked = locked;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getActive() {
        return active;
    }

    public void setActive(long active) {
        this.active = active;
    }

    public long getExpiring() {
        return expiring;
    }

    public void setExpiring(long expiring) {
        this.expiring = expiring;
    }

    public long getLocked() {
        return locked;
    }

    public void setLocked(long locked) {
        this.locked = locked;
    }
}
