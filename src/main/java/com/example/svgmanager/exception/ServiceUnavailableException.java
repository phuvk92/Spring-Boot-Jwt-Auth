package com.example.svgmanager.exception;

/**
 * Dịch vụ phụ thuộc (Keycloak) không phản hồi → 503.
 * Tách khỏi 401 để client không hiểu nhầm "Keycloak chập chờn" thành "phiên đã chết".
 */
public class ServiceUnavailableException extends RuntimeException {

    private final String code;

    public ServiceUnavailableException(String message) {
        this(message, ErrorCodes.AUTH_SERVICE_UNAVAILABLE);
    }

    public ServiceUnavailableException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
