package com.example.svgmanager.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Yêu cầu điều chỉnh số lượt Cut của đại lý từ Admin")
public class AdjustQuotaRequest {

    @Schema(description = "Số lượng điều chỉnh (dương)", example = "100")
    @NotNull(message = "Số lượng điều chỉnh không được để trống")
    private Integer amount;

    @Schema(description = "Loại điều chỉnh: ADD (cộng thêm), SUBTRACT (giảm trừ), SET (thiết lập số dư mới)", example = "ADD")
    @NotNull(message = "Loại điều chỉnh không được để trống")
    private String type; // ADD, SUBTRACT, SET

    @Schema(description = "Lý do / Ghi chú điều chỉnh", example = "Cấp thêm lượt cắt khuyến mãi tháng 11")
    private String notes;

    public AdjustQuotaRequest() {
    }

    public AdjustQuotaRequest(Integer amount, String type, String notes) {
        this.amount = amount;
        this.type = type;
        this.notes = notes;
    }

    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
