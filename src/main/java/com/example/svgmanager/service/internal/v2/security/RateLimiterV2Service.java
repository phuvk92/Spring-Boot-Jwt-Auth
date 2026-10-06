package com.example.svgmanager.service.internal.v2.security;

import com.example.svgmanager.exception.RateLimitExceededException;

public interface RateLimiterV2Service {

    /**
     * Kiểm tra tần suất gọi API của người dùng cho một action cụ thể.
     *
     * @param userId ID người dùng
     * @param action Tên hành động (UPLOAD, PREVIEW, DOWNLOAD, SHARE)
     * @throws RateLimitExceededException nếu vượt quá giới hạn cấu hình
     */
    void checkRateLimit(Long userId, String action) throws RateLimitExceededException;
}
