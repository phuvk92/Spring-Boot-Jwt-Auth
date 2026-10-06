package com.example.svgmanager.service.internal.v2.security;

import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryRateLimiterV2ServiceImpl implements RateLimiterV2Service {

    private static final Logger log = LoggerFactory.getLogger(InMemoryRateLimiterV2ServiceImpl.class);

    private final SvgSecurityProperties properties;
    private final Map<String, Deque<Long>> requestHistory = new ConcurrentHashMap<>();

    public InMemoryRateLimiterV2ServiceImpl(SvgSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public synchronized void checkRateLimit(Long userId, String action) throws RateLimitExceededException {
        if (!properties.getRateLimit().isEnabled() || userId == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L; // Cửa sổ trượt 1 phút

        String key = userId + ":" + action;
        Deque<Long> timestamps = requestHistory.computeIfAbsent(key, k -> new ArrayDeque<>());

        // Loại bỏ các request đã quá 1 phút
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        int maxLimit = properties.getRateLimit().getMaxRequestsPerMinute();
        if (timestamps.size() >= maxLimit) {
            log.warn("[RATE_LIMIT_TRIGGERED] User {} exceeded rate limit for action {} (current: {}, max: {})",
                    userId, action, timestamps.size(), maxLimit);
            throw new RateLimitExceededException(
                    "Bạn đã thao tác quá nhanh (" + maxLimit + " yêu cầu/phút). Vui lòng thử lại sau 1 phút.",
                    ErrorCodes.RATE_LIMIT_EXCEEDED
            );
        }

        timestamps.addLast(now);
    }
}
