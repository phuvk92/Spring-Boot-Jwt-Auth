package com.example.svgmanager.service.internal.v2.security;

import com.example.svgmanager.exception.SvgSecurityException;

/**
 * Abstraction cho việc quét malware, virus và tệp thực thi độc hại giả mạo SVG.
 * Dễ dàng tích hợp ClamAV daemon hoặc giải pháp Scanner Enterprise trong tương lai.
 */
public interface FileSecurityScanner {

    /**
     * Quét nội dung tệp nhị phân trước khi đưa vào phân tích XML.
     *
     * @param fileBytes Mảng byte thô của tệp
     * @param filename  Tên tệp gốc
     * @throws SvgSecurityException Nếu phát hiện tệp thực thi, archive zip bomb hoặc chữ ký độc hại
     */
    void scan(byte[] fileBytes, String filename) throws SvgSecurityException;
}
