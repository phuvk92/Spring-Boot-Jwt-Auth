package com.example.svgmanager.exception;

public class BadRequestException extends RuntimeException {

    /** Mã lỗi máy đọc được (vd SESSION_LIMIT) — null nếu lỗi không cần client phân nhánh. */
    private final String code;
    /** Trường dữ liệu gặp lỗi (vd username, email, password) — null nếu không gắn với trường cụ thể. */
    private final String field;

    public BadRequestException(String message) {
        this(message, null, null);
    }

    public BadRequestException(String message, String code) {
        this(message, code, null);
    }

    public BadRequestException(String message, String code, String field) {
        super(message);
        this.code = code;
        this.field = field;
    }

    public String getCode() {
        return code;
    }

    public String getField() {
        return field;
    }
}
