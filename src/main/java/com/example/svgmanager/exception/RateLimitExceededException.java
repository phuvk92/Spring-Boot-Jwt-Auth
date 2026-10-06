package com.example.svgmanager.exception;

public class RateLimitExceededException extends RuntimeException {

    private final String code;

    public RateLimitExceededException(String message) {
        super(message);
        this.code = ErrorCodes.RATE_LIMIT_EXCEEDED;
    }

    public RateLimitExceededException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
