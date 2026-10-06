package com.example.svgmanager.service.internal.v2.security;

import com.example.svgmanager.exception.SvgPayloadTooLargeException;
import com.example.svgmanager.exception.SvgSecurityException;

public interface SvgSecurityV2Service {

    /**
     * Xác thực toàn diện và làm sạch tệp SVG trước khi mã hoá và lưu trữ.
     *
     * @param rawSvgBytes Mảng byte thô của tệp SVG
     * @param filename    Tên tệp gốc
     * @param mimeType    MIME type do client gửi
     * @return Mảng byte SVG an toàn (sanitized & canonicalized), giữ nguyên hình học cắt
     * @throws SvgSecurityException        Nếu tệp chứa payload độc hại (XXE, XSS, SSRF, active content...)
     * @throws SvgPayloadTooLargeException Nếu kích thước hoặc số phần tử XML vượt ngưỡng cấu hình
     */
    byte[] validateAndSanitize(byte[] rawSvgBytes, String filename, String mimeType)
            throws SvgSecurityException, SvgPayloadTooLargeException;
}
