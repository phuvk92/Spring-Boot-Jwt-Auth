package com.example.svgmanager.exception;

public class UnauthorizedException extends RuntimeException {

    /** Mã lỗi máy đọc được (vd SESSION_LIMIT) — null nếu lỗi không cần client phân nhánh. */
    private final String code;

    public UnauthorizedException(String message) {
        this(message, null);
    }

    public UnauthorizedException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
