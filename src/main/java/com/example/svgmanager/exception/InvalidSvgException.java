package com.example.svgmanager.exception;

public class InvalidSvgException extends RuntimeException {
    public InvalidSvgException(String message) {
        super(message);
    }

    public InvalidSvgException(String message, Throwable cause) {
        super(message, cause);
    }
}
