package com.example.svgmanager.exception;

public class SvgSecurityException extends RuntimeException {

    private final String code;

    public SvgSecurityException(String message) {
        super(message);
        this.code = ErrorCodes.SVG_SECURITY_VALIDATION_FAILED;
    }

    public SvgSecurityException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
