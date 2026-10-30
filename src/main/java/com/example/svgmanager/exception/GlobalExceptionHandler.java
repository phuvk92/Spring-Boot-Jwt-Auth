package com.example.svgmanager.exception;

import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.security.InternalApiV2AuditFilter;
import com.example.svgmanager.svg.SvgImportException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    private void setAuditErrorCode(HttpServletRequest request, String code) {
        if (request != null && code != null) {
            request.setAttribute(InternalApiV2AuditFilter.AUDIT_ERROR_CODE_ATTR, code);
        }
    }

    private String resolveMessage(String code, String defaultMessage) {
        if (code == null) {
            return defaultMessage;
        }
        try {
            Locale locale = LocaleContextHolder.getLocale();
            String msg = messageSource.getMessage("error." + code, null, null, locale);
            if (msg != null && !msg.isBlank()) {
                return msg;
            }
        } catch (Exception ignored) {
        }
        return defaultMessage;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Resource not found: {}", ex.getMessage());
        String code = ex.getCode() != null ? ex.getCode() : "NOT_FOUND";
        setAuditErrorCode(request, code);
        String message = ex.getCode() != null ? resolveMessage(ex.getCode(), ex.getMessage()) : ex.getMessage();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .code(ex.getCode())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
            org.springframework.web.HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request
    ) {
        log.warn("Unsupported media type on {}: {}", request.getRequestURI(), ex.getMessage());
        setAuditErrorCode(request, "UNSUPPORTED_MEDIA_TYPE");
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .error(HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase())
                .message(resolveMessage("UNSUPPORTED_MEDIA_TYPE", ex.getMessage()))
                .path(request.getRequestURI())
                .code("UNSUPPORTED_MEDIA_TYPE")
                .build();
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException ex,
            HttpServletRequest request
    ) {
        log.warn("Bad request: {}", ex.getMessage());
        String code = ex.getCode() != null ? ex.getCode() : "BAD_REQUEST";
        setAuditErrorCode(request, code);
        String message = ex.getCode() != null ? resolveMessage(ex.getCode(), ex.getMessage()) : ex.getMessage();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .code(ex.getCode())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleMissingOrInvalidParams(
            Exception ex,
            HttpServletRequest request
    ) {
        log.warn("Missing or invalid parameter on {}: {}", request.getRequestURI(), ex.getMessage());
        setAuditErrorCode(request, "BAD_REQUEST");
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(resolveMessage("BAD_REQUEST", ex.getMessage()))
                .path(request.getRequestURI())
                .code("BAD_REQUEST")
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(
            ConflictException ex,
            HttpServletRequest request
    ) {
        log.warn("Conflict: {}", ex.getMessage());
        String code = ex.getCode() != null ? ex.getCode() : "CONFLICT";
        setAuditErrorCode(request, code);
        String message = ex.getCode() != null ? resolveMessage(ex.getCode(), ex.getMessage()) : ex.getMessage();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .code(ex.getCode())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(UserAccountExpiredException.class)
    public ResponseEntity<ErrorResponse> handleUserAccountExpiredException(
            UserAccountExpiredException ex,
            HttpServletRequest request
    ) {
        log.warn("User account expired: {}", ex.getMessage());
        String code = ex.getCode() != null ? ex.getCode() : ErrorCodes.USER_ACCOUNT_EXPIRED;
        setAuditErrorCode(request, code);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message(resolveMessage(code, ex.getMessage()))
                .path(request.getRequestURI())
                .code(code)
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex,
            HttpServletRequest request
    ) {
        log.warn("Unauthorized: {}", ex.getMessage());
        String code = ex.getCode() != null ? ex.getCode() : "UNAUTHORIZED";
        setAuditErrorCode(request, code);
        String message = ex.getCode() != null ? resolveMessage(ex.getCode(), ex.getMessage()) : ex.getMessage();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .code(ex.getCode())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(
            ForbiddenException ex,
            HttpServletRequest request
    ) {
        log.warn("Forbidden: {}", ex.getMessage());
        String code = ex.getCode() != null ? ex.getCode() : "FORBIDDEN";
        setAuditErrorCode(request, code);
        String message = ex.getCode() != null ? resolveMessage(ex.getCode(), ex.getMessage()) : ex.getMessage();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .code(ex.getCode())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailableException(
            ServiceUnavailableException ex,
            HttpServletRequest request
    ) {
        log.error("Dependent service unavailable: {}", ex.getMessage());
        String code = ex.getCode() != null ? ex.getCode() : "SERVICE_UNAVAILABLE";
        setAuditErrorCode(request, code);
        String message = ex.getCode() != null ? resolveMessage(ex.getCode(), ex.getMessage()) : ex.getMessage();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .code(ex.getCode())
                .build();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler(SvgImportException.class)
    public ResponseEntity<ErrorResponse> handleSvgImportException(
            SvgImportException ex,
            HttpServletRequest request
    ) {
        log.warn("SVG upload rejected at part-extraction: {}", ex.getMessage());
        setAuditErrorCode(request, ex.getKind());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(resolveMessage(ex.getKind(), ex.getMessage()))
                .path(request.getRequestURI())
                .code(ex.getKind())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(InvalidSvgException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSvgException(
            InvalidSvgException ex,
            HttpServletRequest request
    ) {
        log.warn("Invalid or malicious SVG rejected: {}", ex.getMessage());
        setAuditErrorCode(request, "INVALID_SVG");
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(resolveMessage("INVALID_SVG", ex.getMessage()))
                .path(request.getRequestURI())
                .code("INVALID_SVG")
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorageException(
            FileStorageException ex,
            HttpServletRequest request
    ) {
        log.error("File storage error: {}", ex.getMessage(), ex);
        setAuditErrorCode(request, "FILE_STORAGE_ERROR");
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(resolveMessage("FILE_STORAGE_ERROR", ex.getMessage()))
                .path(request.getRequestURI())
                .code("FILE_STORAGE_ERROR")
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        log.warn("Bad credentials: {}", ex.getMessage());
        setAuditErrorCode(request, "BAD_CREDENTIALS");
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message(resolveMessage("BAD_CREDENTIALS", "Invalid username or password"))
                .path(request.getRequestURI())
                .code("BAD_CREDENTIALS")
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledException(
            DisabledException ex,
            HttpServletRequest request
    ) {
        log.warn("Account is disabled: {}", ex.getMessage());
        setAuditErrorCode(request, "ACCOUNT_DISABLED");
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message(resolveMessage("ACCOUNT_DISABLED", "User account is disabled. Please contact an administrator."))
                .path(request.getRequestURI())
                .code("ACCOUNT_DISABLED")
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        log.warn("Access denied: {}", ex.getMessage());
        setAuditErrorCode(request, "FORBIDDEN");
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message(resolveMessage("FORBIDDEN", "Access denied: You do not have permission to access this resource"))
                .path(request.getRequestURI())
                .code("FORBIDDEN")
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("Validation error on {}: {}", request.getRequestURI(), errorMessage);
        setAuditErrorCode(request, "VALIDATION_ERROR");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(errorMessage.isEmpty() ? resolveMessage("VALIDATION_ERROR", "Validation failed for request") : errorMessage)
                .path(request.getRequestURI())
                .code("VALIDATION_ERROR")
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        log.warn("Upload size limit exceeded: {}", ex.getMessage());
        setAuditErrorCode(request, ErrorCodes.SVG_SIZE_LIMIT_EXCEEDED);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .error(HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase())
                .message(resolveMessage(ErrorCodes.SVG_SIZE_LIMIT_EXCEEDED, "Uploaded file exceeds the maximum allowed size limit"))
                .path(request.getRequestURI())
                .code(ErrorCodes.SVG_SIZE_LIMIT_EXCEEDED)
                .build();
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            org.springframework.http.converter.HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        log.warn("Message not readable on {}: {}", request.getRequestURI(), ex.getMessage());
        String msg = ex.getMessage();
        if (msg != null && (msg.contains("StreamReadConstraints") || msg.contains("exceeds the maximum allowed") || msg.contains("max-string-length"))) {
            setAuditErrorCode(request, ErrorCodes.DESIGN_TOO_LARGE);
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.PAYLOAD_TOO_LARGE.value())
                    .error(HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase())
                    .message(resolveMessage(ErrorCodes.DESIGN_TOO_LARGE, "Bản làm việc vượt quá dung lượng tối đa cho phép (20 MB)."))
                    .path(request.getRequestURI())
                    .code(ErrorCodes.DESIGN_TOO_LARGE)
                    .build();
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
        }

        setAuditErrorCode(request, ErrorCodes.DESIGN_INVALID);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(resolveMessage(ErrorCodes.DESIGN_INVALID, "Dữ liệu gửi lên không đúng định dạng JSON hợp lệ."))
                .path(request.getRequestURI())
                .code(ErrorCodes.DESIGN_INVALID)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(PayloadTooLargeException.class)
    public ResponseEntity<ErrorResponse> handlePayloadTooLargeException(
            PayloadTooLargeException ex,
            HttpServletRequest request
    ) {
        log.warn("Payload too large: {}", ex.getMessage());
        String code = ex.getCode() != null ? ex.getCode() : "PAYLOAD_TOO_LARGE";
        setAuditErrorCode(request, code);
        String message = ex.getCode() != null ? resolveMessage(ex.getCode(), ex.getMessage()) : resolveMessage("PAYLOAD_TOO_LARGE", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .error(HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .code(ex.getCode())
                .build();
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    @ExceptionHandler(SvgSecurityException.class)
    public ResponseEntity<ErrorResponse> handleSvgSecurityException(
            SvgSecurityException ex,
            HttpServletRequest request
    ) {
        log.warn("[SVG_SECURITY_REJECTED] {}: {}", request.getRequestURI(), ex.getMessage());
        String code = ex.getCode() != null ? ex.getCode() : "SVG_SECURITY_REJECTED";
        setAuditErrorCode(request, code);
        String message = ex.getCode() != null ? resolveMessage(ex.getCode(), ex.getMessage()) : resolveMessage("SVG_SECURITY_REJECTED", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .code(ex.getCode())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitException(
            RateLimitExceededException ex,
            HttpServletRequest request
    ) {
        log.warn("[RATE_LIMIT_EXCEEDED] {}: {}", request.getRequestURI(), ex.getMessage());
        String code = ex.getCode() != null ? ex.getCode() : "RATE_LIMIT_EXCEEDED";
        setAuditErrorCode(request, code);
        String message = ex.getCode() != null ? resolveMessage(ex.getCode(), ex.getMessage()) : resolveMessage("RATE_LIMIT_EXCEEDED", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .code(ex.getCode())
                .build();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception occurred on {}", request.getRequestURI(), ex);
        setAuditErrorCode(request, "INTERNAL_SERVER_ERROR");
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(resolveMessage("INTERNAL_SERVER_ERROR", "An unexpected internal server error occurred"))
                .path(request.getRequestURI())
                .code("INTERNAL_SERVER_ERROR")
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
