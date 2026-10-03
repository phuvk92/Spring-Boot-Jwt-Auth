package com.example.svgmanager.exception;

public class PayloadTooLargeException extends RuntimeException {

    private final String code;

    public PayloadTooLargeException(String message) {
        this(message, null);
    }

    public PayloadTooLargeException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
