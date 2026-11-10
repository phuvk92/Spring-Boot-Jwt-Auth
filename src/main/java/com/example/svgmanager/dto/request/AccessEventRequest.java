package com.example.svgmanager.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Một sự kiện truy cập app cắt đẩy lên — POST /api/v1/access-log (F-54 · CL-34, contract 0.4.0).
 * Chỉ siêu dữ liệu: KHÔNG nhận hình học hay nội dung bản vẽ dưới bất kỳ dạng nào.
 *
 * Mọi trường đều do client tự khai nên không dùng để phân quyền; {@code at} giữ dạng chuỗi
 * vì chỉ để ghi lại — server tự lấy thời điểm nhận làm mốc audit.
 */
@Schema(description = "Sự kiện truy cập từ app cắt — chỉ siêu dữ liệu (F-54)")
public class AccessEventRequest {

    @Schema(description = "Loại sự kiện: session_start | template_download. Giá trị lạ vẫn được ghi nguyên văn.",
            example = "template_download", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Loại sự kiện không được để trống")
    @Size(max = 50, message = "Loại sự kiện tối đa 50 ký tự")
    private String kind;

    @Schema(description = "Thời điểm phía client (ISO-8601)", example = "2026-11-10T14:53:29+07:00")
    @Size(max = 64, message = "Thời điểm tối đa 64 ký tự")
    private String at;

    @Schema(description = "Ngữ cảnh — template_download mang \"{fileKey} · {tên file}\"",
            example = "audi-a1-2016--37b63383 · Audi A1 2016")
    @Size(max = 500, message = "Chi tiết tối đa 500 ký tự")
    private String detail;

    @Schema(description = "Số mẫu đã tải trong phiên, client tự đếm — đầu mối phát hiện tải hàng loạt", example = "3")
    private Integer downloadsInSession;

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getAt() { return at; }
    public void setAt(String at) { this.at = at; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public Integer getDownloadsInSession() { return downloadsInSession; }
    public void setDownloadsInSession(Integer downloadsInSession) { this.downloadsInSession = downloadsInSession; }
}
