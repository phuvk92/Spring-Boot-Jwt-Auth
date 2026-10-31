package com.example.svgmanager.util;

import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ConflictException;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.exception.ServiceUnavailableException;
import com.example.svgmanager.exception.UnauthorizedException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

/**
 * Centralized parser for Keycloak HTTP responses and network exceptions.
 * Converts raw Keycloak errors into structured domain exceptions with stable error codes,
 * targeted field names, and clean user-friendly messages.
 */
public final class KeycloakErrorParser {

    private static final Logger log = LoggerFactory.getLogger(KeycloakErrorParser.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private KeycloakErrorParser() {
    }

    /**
     * Parses an exception caught during interaction with Keycloak Admin REST API.
     * Logs the full technical context for developer debugging, while returning a clean domain exception.
     *
     * @param ex the original exception
     * @param context operational context for logging (e.g. "createUser:agent01")
     * @return the appropriate RuntimeException to be handled by GlobalExceptionHandler
     */
    public static RuntimeException parseException(Exception ex, String context) {
        log.warn("Keycloak operation issue during [{}]: {}", context, ex.getMessage());

        if (ex instanceof BadRequestException || ex instanceof ConflictException
                || ex instanceof ResourceNotFoundException || ex instanceof ServiceUnavailableException
                || ex instanceof UnauthorizedException || ex instanceof ForbiddenException) {
            return (RuntimeException) ex;
        }

        // Check for connection / network issues
        if (ex instanceof ResourceAccessException || isConnectionException(ex)) {
            log.error("Keycloak connection failure during [{}]: {}", context, ex.getMessage(), ex);
            return new ServiceUnavailableException(
                    "Hệ thống xác thực hiện đang tạm thời không khả dụng. Vui lòng thử lại sau.",
                    ErrorCodes.AUTH_SERVICE_UNAVAILABLE
            );
        }

        if (ex instanceof RestClientResponseException restEx) {
            int statusCode = restEx.getStatusCode().value();
            String responseBody = restEx.getResponseBodyAsString();
            return parseResponseBody(statusCode, responseBody, context, restEx);
        }

        log.error("Unexpected error during Keycloak [{}]: {}", context, ex.getMessage(), ex);
        return new BadRequestException(
                "Không thể xử lý yêu cầu với dịch vụ xác thực. Vui lòng thử lại sau.",
                ErrorCodes.KEYCLOAK_ERROR
        );
    }

    /**
     * Inspects HTTP status code and response body from Keycloak to construct a domain exception.
     */
    public static RuntimeException parseResponseBody(int statusCode, String responseBody, String context, Exception originalEx) {
        String field = null;
        String errorMessage = null;
        String error = null;
        String errorDescription = null;

        if (responseBody != null && !responseBody.isBlank()) {
            try {
                JsonNode root = OBJECT_MAPPER.readTree(responseBody);
                if (root.has("field")) {
                    field = root.get("field").asText(null);
                }
                if (root.has("errorMessage")) {
                    errorMessage = root.get("errorMessage").asText(null);
                }
                if (root.has("error")) {
                    error = root.get("error").asText(null);
                }
                if (root.has("error_description")) {
                    errorDescription = root.get("error_description").asText(null);
                }
            } catch (Exception parseEx) {
                log.debug("Could not parse Keycloak response as JSON: {}", responseBody);
            }
        }

        String combined = ((errorMessage != null ? errorMessage : "") + " "
                + (error != null ? error : "") + " "
                + (errorDescription != null ? errorDescription : "") + " "
                + (responseBody != null ? responseBody : "")).toLowerCase();

        // 1. Conflict (409)
        if (statusCode == 409) {
            if ("email".equalsIgnoreCase(field) || combined.contains("email")) {
                return new ConflictException(
                        "Email đã được sử dụng. Vui lòng kiểm tra lại.",
                        ErrorCodes.EMAIL_ALREADY_EXISTS,
                        "email"
                );
            }
            return new ConflictException(
                    "Tên đăng nhập đã tồn tại. Vui lòng chọn tên khác.",
                    ErrorCodes.USERNAME_ALREADY_EXISTS,
                    "username"
            );
        }

        // 2. Not Found (404)
        if (statusCode == 404) {
            return new ResourceNotFoundException(
                    "Không tìm thấy tài khoản người dùng.",
                    ErrorCodes.USER_NOT_FOUND
            );
        }

        // 3. Unauthorized (401)
        if (statusCode == 401) {
            return new UnauthorizedException(
                    "Phiên đăng nhập đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại.",
                    "UNAUTHORIZED"
            );
        }

        // 4. Forbidden (403)
        if (statusCode == 403) {
            return new ForbiddenException(
                    "Bạn không có quyền thực hiện thao tác này.",
                    "FORBIDDEN"
            );
        }

        // 5. Bad Request (400)
        if (statusCode == 400) {
            // Username invalid format
            if ("username".equalsIgnoreCase(field)
                    || combined.contains("error-username-invalid-character")
                    || combined.contains("invalid username")) {
                return new BadRequestException(
                        "Tên đăng nhập không hợp lệ. Chỉ được dùng chữ cái tiếng Anh, số, dấu gạch dưới (_) và dấu gạch ngang (-).",
                        ErrorCodes.USERNAME_INVALID_FORMAT,
                        "username"
                );
            }
            // Username required
            if (combined.contains("username required") || combined.contains("error-username-must-not-be-blank")) {
                return new BadRequestException(
                        "Vui lòng nhập tên đăng nhập.",
                        ErrorCodes.USERNAME_REQUIRED,
                        "username"
                );
            }
            // User exists with same username
            if (combined.contains("user exists with same username") || combined.contains("error-username-must-be-unique")) {
                return new ConflictException(
                        "Tên đăng nhập đã tồn tại. Vui lòng chọn tên khác.",
                        ErrorCodes.USERNAME_ALREADY_EXISTS,
                        "username"
                );
            }
            // User exists with same email
            if (combined.contains("user exists with same email") || combined.contains("error-email-must-be-unique")) {
                return new ConflictException(
                        "Email đã được sử dụng. Vui lòng kiểm tra lại.",
                        ErrorCodes.EMAIL_ALREADY_EXISTS,
                        "email"
                );
            }
            // Email invalid
            if ("email".equalsIgnoreCase(field)
                    || combined.contains("error-email-invalid")
                    || combined.contains("invalidemailmessage")
                    || combined.contains("invalid email")) {
                return new BadRequestException(
                        "Địa chỉ email không hợp lệ.",
                        ErrorCodes.EMAIL_INVALID_FORMAT,
                        "email"
                );
            }
            // Password policy
            if ("password".equalsIgnoreCase(field)
                    || combined.contains("password")
                    || combined.contains("invalidpasswordminlengthmessage")
                    || combined.contains("error-password-policy")) {
                return new BadRequestException(
                        "Mật khẩu không đáp ứng yêu cầu bảo mật. Vui lòng kiểm tra lại.",
                        ErrorCodes.PASSWORD_POLICY_VIOLATION,
                        "password"
                );
            }

            // General 400
            return new BadRequestException(
                    "Yêu cầu không hợp lệ hoặc tham số không đúng định dạng.",
                    "BAD_REQUEST",
                    field
            );
        }

        // 6. Service unavailable / Gateway error (502, 503, 504)
        if (statusCode == 502 || statusCode == 503 || statusCode == 504) {
            return new ServiceUnavailableException(
                    "Hệ thống xác thực hiện đang tạm thời không khả dụng. Vui lòng thử lại sau.",
                    ErrorCodes.AUTH_SERVICE_UNAVAILABLE
            );
        }

        // Fallback for any other code
        return new BadRequestException(
                "Không thể xử lý yêu cầu với dịch vụ xác thực. Vui lòng thử lại sau.",
                ErrorCodes.KEYCLOAK_ERROR,
                field
        );
    }

    private static boolean isConnectionException(Throwable th) {
        Throwable curr = th;
        while (curr != null) {
            if (curr instanceof ConnectException || curr instanceof SocketTimeoutException) {
                return true;
            }
            curr = curr.getCause();
        }
        return false;
    }
}
