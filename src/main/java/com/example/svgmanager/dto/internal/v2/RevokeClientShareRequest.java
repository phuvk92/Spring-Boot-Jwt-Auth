package com.example.svgmanager.dto.internal.v2;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request thu hồi lượt chia sẻ (Unshare)")
public class RevokeClientShareRequest {

    @Schema(description = "Lý do thu hồi lượt chia sẻ (tuỳ chọn)", example = "Đã hoàn thành thi công cắt decal")
    private String reason;

    public RevokeClientShareRequest() {
    }

    public RevokeClientShareRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
