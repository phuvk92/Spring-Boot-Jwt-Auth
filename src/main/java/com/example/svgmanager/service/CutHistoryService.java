package com.example.svgmanager.service;

import com.example.svgmanager.dto.response.CutHistoryResponse;

/**
 * Lịch sử cắt của MÁY đang gọi (F-38 · KX-03) — scope theo claim `sid` của token,
 * không theo user id: "lịch sử trên máy này" trong hợp đồng là theo thiết bị.
 */
public interface CutHistoryService {

    /**
     * Token không gắn máy nào (sid null, máy đã bị gỡ, hoặc enforce tắt) → trả lịch sử
     * rỗng thay vì ném — mất dấu vết phiên không phải lỗi của người đọc lịch sử.
     */
    CutHistoryResponse getHistoryForCurrentDevice();

    /**
     * Ghi một lượt cắt đã xong trên máy của phiên hiện tại (POST /api/v1/cuts).
     *
     * @param request        dữ liệu thống kê lượt cắt
     * @param idempotencyKey khoá chống ghi đôi (tuỳ chọn)
     * @return dòng CutJobResponse vừa ghi (hoặc dòng cũ nếu trùng idempotency-key trong 24h)
     */
    com.example.svgmanager.dto.response.CutJobResponse recordCut(com.example.svgmanager.dto.request.RecordCutRequest request, String idempotencyKey);
}
