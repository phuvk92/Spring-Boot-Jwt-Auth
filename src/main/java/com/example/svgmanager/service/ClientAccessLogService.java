package com.example.svgmanager.service;

import com.example.svgmanager.dto.request.AccessEventRequest;

/**
 * Nhật ký truy cập của app cắt (F-54 · CL-34) ghi vào audit_logs — để admin thấy ở trang Audit
 * "ai mở file nào, trên máy nào, lúc nào" thay vì phải đoán từ các dòng GET /api/v1/files/...
 * (các dòng đó lẫn cả lượt xem trước trong danh sách).
 */
public interface ClientAccessLogService {

    /** Action audit cho một lượt mở file trong kho thiết kế (sự kiện {@code template_download}). */
    String OPEN_FILE = "OPEN_FILE";

    /**
     * Ghi một sự kiện của user đang đăng nhập. Không ném vì dữ liệu lạ: log là một chiều,
     * sự kiện hỏng vẫn được ghi với những gì đọc được.
     *
     * @param clientIp IP của máy gọi (đã qua proxy) — ghi vào chi tiết
     */
    void record(AccessEventRequest event, String clientIp);
}
