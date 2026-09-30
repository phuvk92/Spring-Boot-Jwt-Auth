package com.example.svgmanager.svg;

import com.example.svgmanager.exception.ErrorCodes;

/**
 * Lỗi đọc/tách part từ SVG khi upload — map ra 400 ở tầng controller.
 * {@code kind} phân loại cho code lỗi máy đọc được của client/web.
 */
public class SvgImportException extends RuntimeException {

    /** File không phải SVG / XML hỏng / gốc không phải <svg>. */
    public static final String FORMAT = ErrorCodes.UNSUPPORTED_FORMAT;
    /** File không khai đơn vị → không quy ra mm được (DS-108). */
    public static final String UNITS_MISSING = ErrorCodes.SVG_UNITS_MISSING;

    private final String kind;

    public SvgImportException(String message, String kind) {
        super(message);
        this.kind = kind;
    }

    public String getKind() {
        return kind;
    }
}
