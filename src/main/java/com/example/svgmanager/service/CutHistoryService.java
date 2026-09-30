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
}
