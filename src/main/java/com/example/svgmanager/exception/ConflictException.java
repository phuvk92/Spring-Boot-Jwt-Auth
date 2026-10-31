package com.example.svgmanager.exception;

public class ConflictException extends RuntimeException {
    private final String code;
    private final String field;

    public ConflictException(String message) {
        this(message, null, null);
    }

    public ConflictException(String message, String code) {
        this(message, code, null);
    }

    public ConflictException(String message, String code, String field) {
        super(message);
        this.code = code;
        this.field = field;
    }

    /** Mã lỗi hợp đồng (vd NODE_NAME_TAKEN); null → ErrorResponse.code rỗng như cũ. */
    public String getCode() {
        return code;
    }

    public String getField() {
        return field;
    }
}
