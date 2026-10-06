package com.example.svgmanager.exception;

public class SvgPayloadTooLargeException extends PayloadTooLargeException {

    public SvgPayloadTooLargeException(String message) {
        super(message, ErrorCodes.SVG_SIZE_LIMIT_EXCEEDED);
    }

    public SvgPayloadTooLargeException(String message, String code) {
        super(message, code);
    }
}
