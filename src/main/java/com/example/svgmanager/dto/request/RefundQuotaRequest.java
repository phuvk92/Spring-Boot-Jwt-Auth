package com.example.svgmanager.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Yêu cầu hoàn trả lượt Cut")
public class RefundQuotaRequest {

    @Schema(description = "Lý do hoàn trả (ví dụ: máy kẹt dao, file lỗi)", example = "Máy kẹt dao trong quá trình cắt chi tiết nắp capo")
    private String reason;

    public RefundQuotaRequest() {
    }

    public RefundQuotaRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
