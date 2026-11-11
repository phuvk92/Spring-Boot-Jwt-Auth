package com.example.svgmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe, memory-bounded in-memory LRU cache for decrypted and loaded SVG preview bytes.
 * Provides high-throughput preview serving while strictly isolating entries by version checksum.
 */
@Service
public class SvgPreviewCacheService {

    private static final Logger log = LoggerFactory.getLogger(SvgPreviewCacheService.class);

    private static final int MAX_ENTRIES = 1000;

    private final Map<String, byte[]> lruCache = new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private final Object lock = new Object();
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);

    public byte[] getV1(Long fileId, String checksum) {
        if (fileId == null) return null;
        String key = "v1:" + fileId + ":" + (checksum != null ? checksum : "default");
        return get(key);
    }

    public void putV1(Long fileId, String checksum, byte[] bytes) {
        if (fileId == null || bytes == null || bytes.length == 0) return;
        String key = "v1:" + fileId + ":" + (checksum != null ? checksum : "default");
        put(key, bytes);
    }

    public void invalidateV1(Long fileId) {
        if (fileId == null) return;
        String prefix = "v1:" + fileId + ":";
        invalidatePrefix(prefix);
    }

    public byte[] getV2(Long fileId, String encryptedChecksum) {
        if (fileId == null) return null;
        String key = "v2:" + fileId + ":" + (encryptedChecksum != null ? encryptedChecksum : "default");
        return get(key);
    }

    public byte[] getV2(String cacheKey) {
        if (cacheKey == null) return null;
        return get("v2:" + cacheKey);
    }

    public void putV2(Long fileId, String encryptedChecksum, byte[] bytes) {
        if (fileId == null || bytes == null || bytes.length == 0) return;
        String key = "v2:" + fileId + ":" + (encryptedChecksum != null ? encryptedChecksum : "default");
        put(key, bytes);
    }

    public void putV2(String cacheKey, byte[] bytes) {
        if (cacheKey == null || bytes == null || bytes.length == 0) return;
        put("v2:" + cacheKey, bytes);
    }

    public void invalidateV2(Long fileId) {
        if (fileId == null) return;
        String prefix = "v2:" + fileId + ":";
        invalidatePrefix(prefix);
    }

    private byte[] get(String key) {
        synchronized (lock) {
            byte[] cached = lruCache.get(key);
            if (cached != null) {
                hits.incrementAndGet();
                return cached;
            }
            misses.incrementAndGet();
            return null;
        }
    }

    private void put(String key, byte[] bytes) {
        synchronized (lock) {
            lruCache.put(key, bytes);
        }
    }

    private void invalidatePrefix(String prefix) {
        synchronized (lock) {
            lruCache.keySet().removeIf(k -> k.startsWith(prefix));
        }
    }

    public void clear() {
        synchronized (lock) {
            lruCache.clear();
        }
    }

    public long getHits() {
        return hits.get();
    }

    public long getMisses() {
        return misses.get();
    }

    public int size() {
        synchronized (lock) {
            return lruCache.size();
        }
    }
}
