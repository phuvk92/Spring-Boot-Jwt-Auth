package com.example.svgmanager.exception;

public class ResourceNotFoundException extends RuntimeException {

    /** Mã lỗi máy đọc được (vd FILE_NOT_FOUND) — null nếu lỗi không cần client phân nhánh. */
    private final String code;

    public ResourceNotFoundException(String message) {
        super(message);
        this.code = null;
    }

    public ResourceNotFoundException(String message, String code) {
        super(message);
        this.code = code;
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.code = null;
    }

    public String getCode() {
        return code;
    }
}
