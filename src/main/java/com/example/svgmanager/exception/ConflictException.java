package com.example.svgmanager.exception;

public class ConflictException extends RuntimeException {
    private final String code;

    public ConflictException(String message) {
        this(message, null);
    }

    public ConflictException(String message, String code) {
        super(message);
        this.code = code;
    }

    /** Mã lỗi hợp đồng (vd NODE_NAME_TAKEN); null → ErrorResponse.code rỗng như cũ. */
    public String getCode() {
        return code;
    }
}
