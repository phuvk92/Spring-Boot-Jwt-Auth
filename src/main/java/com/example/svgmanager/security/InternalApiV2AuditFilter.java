package com.example.svgmanager.security;

import com.example.svgmanager.dto.internal.v2.InternalApiV2AuditEvent;
import com.example.svgmanager.entity.Dealer;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.service.internal.v2.AuditLogV2Service;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Filter tập trung tự động ghi nhận Audit Log cho mọi request gọi tới Internal API V2.
 * Nằm trong Security Filter Chain, ghi nhận mọi tình huống: thành công, validation lỗi,
 * authentication thất bại, authorization thất bại, 404, exception, server error.
 */
@Component
public class InternalApiV2AuditFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InternalApiV2AuditFilter.class);

    public static final String AUDIT_ERROR_CODE_ATTR = "audit_error_code";
    private static final String INTERNAL_V2_PREFIX = "/api/internal/v2";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{8,64}$");

    private static final Map<String, String> KNOWN_CLIENT_NAMES = Map.of(
            "cutting-client", "Cutting Machine Client",
            "cutting-manager-web", "Web Admin Portal",
            "admin-cli", "Admin CLI Tool"
    );

    private final AuditLogV2Service auditLogV2Service;
    private final UserRepository userRepository;

    public InternalApiV2AuditFilter(AuditLogV2Service auditLogV2Service, UserRepository userRepository) {
        this.auditLogV2Service = auditLogV2Service;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        // Chỉ audit các request nằm trong phạm vi Internal V2
        return !path.startsWith(INTERNAL_V2_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startNs = System.nanoTime();
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 1. Phục hồi hoặc sinh mới Request ID an toàn chống log injection
        String rawRequestId = request.getHeader(REQUEST_ID_HEADER);
        String requestId = resolveRequestId(rawRequestId);

        // 2. Phục hồi hoặc sinh mới Trace ID
        String rawTraceId = request.getHeader(TRACE_ID_HEADER);
        String traceId = resolveTraceId(rawTraceId, requestId);

        // 3. Đưa Request ID vào Header response và MDC
        response.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put("requestId", requestId);

        // 4. Đo kích thước request nếu xác định được
        long requestSize = request.getContentLengthLong();
        if (requestSize < 0) {
            String clHeader = request.getHeader("Content-Length");
            if (StringUtils.hasText(clHeader)) {
                try {
                    requestSize = Long.parseLong(clHeader.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        Long requestSizeBytes = requestSize >= 0 ? requestSize : null;

        // 5. Bọc response với CountingResponseWrapper nhẹ để đếm response size O(1) memory
        CountingResponseWrapper countingResponse = new CountingResponseWrapper(response);

        Throwable caughtException = null;
        try {
            filterChain.doFilter(request, countingResponse);
        } catch (Throwable ex) {
            caughtException = ex;
            throw ex;
        } finally {
            try {
                long durationMs = (System.nanoTime() - startNs) / 1_000_000L;
                int status = countingResponse.getStatus();
                if (caughtException != null && status == HttpServletResponse.SC_OK) {
                    status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                }

                // 6. Xác định kết quả HTTP
                String result = (status >= 200 && status < 400) ? "SUCCESS" : "FAIL";

                // 7. Xác định Error Code
                String errorCode = resolveErrorCode(request, status, caughtException);

                // 8. Thu thập Principal, User, Role, Dealer và Client ID đã xác minh
                IdentityDetails identity = resolveIdentity();

                // 9. Thu thập Client IP và User-Agent an toàn
                String clientIp = resolveClientIp(request);
                String userAgent = sanitizeUserAgent(request.getHeader("User-Agent"));

                // 10. Thu thập Response size
                long bytesWritten = countingResponse.getBytesWritten();
                Long responseSizeBytes = bytesWritten > 0 ? bytesWritten : null;
                if (responseSizeBytes == null) {
                    String clHeader = countingResponse.getHeader("Content-Length");
                    if (StringUtils.hasText(clHeader)) {
                        try {
                            long cl = Long.parseLong(clHeader.trim());
                            if (cl >= 0) {
                                responseSizeBytes = cl;
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                // 11. Tạo và lưu Audit Event
                InternalApiV2AuditEvent auditEvent = InternalApiV2AuditEvent.builder()
                        .requestId(requestId)
                        .traceId(traceId)
                        .clientId(identity.clientId())
                        .clientName(identity.clientName())
                        .principalType(identity.principalType())
                        .userId(identity.userId())
                        .keycloakUserId(identity.keycloakUserId())
                        .username(identity.username())
                        .role(identity.role())
                        .dealerId(identity.dealerId())
                        .dealerName(identity.dealerName())
                        .httpMethod(method)
                        .endpoint(path)
                        .apiVersion("V2")
                        .httpStatus(status)
                        .result(result)
                        .errorCode(errorCode)
                        .executionTimeMs(durationMs)
                        .clientIp(clientIp)
                        .userAgent(userAgent)
                        .requestSizeBytes(requestSizeBytes)
                        .responseSizeBytes(responseSizeBytes)
                        .build();

                auditLogV2Service.recordAuditLog(auditEvent);

            } catch (Exception ex) {
                log.error("[INTERNAL_V2_AUDIT_ERROR] Error creating audit log for request '{}': {}", requestId, ex.getMessage());
            } finally {
                MDC.remove("requestId");
            }
        }
    }

    private String resolveRequestId(String raw) {
        if (StringUtils.hasText(raw)) {
            String trimmed = raw.trim();
            if (REQUEST_ID_PATTERN.matcher(trimmed).matches()) {
                return trimmed;
            }
            log.warn("[AUDIT_REQUEST_ID_INVALID] Rejected invalid X-Request-ID header: '{}', generating new UUID", trimmed);
        }
        return UUID.randomUUID().toString();
    }

    private String resolveTraceId(String rawTraceId, String requestId) {
        if (StringUtils.hasText(rawTraceId)) {
            String trimmed = rawTraceId.trim();
            if (REQUEST_ID_PATTERN.matcher(trimmed).matches()) {
                return trimmed;
            }
        }
        return requestId;
    }

    private String resolveErrorCode(HttpServletRequest request, int status, Throwable caught) {
        if (status < 400) {
            return null;
        }

        Object attrCode = request.getAttribute(AUDIT_ERROR_CODE_ATTR);
        if (attrCode != null && StringUtils.hasText(attrCode.toString())) {
            return attrCode.toString().trim();
        }

        if (caught != null) {
            String exName = caught.getClass().getSimpleName();
            return exName.replace("Exception", "").toUpperCase();
        }

        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 409 -> "CONFLICT";
            case 413 -> "PAYLOAD_TOO_LARGE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            case 429 -> "RATE_LIMIT_EXCEEDED";
            case 503 -> "SERVICE_UNAVAILABLE";
            default -> (status >= 500) ? "INTERNAL_SERVER_ERROR" : "HTTP_" + status;
        };
    }

    private IdentityDetails resolveIdentity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return new IdentityDetails(null, null, "ANONYMOUS", null, null, null, null, null, null);
        }

        if (auth.getPrincipal() instanceof Jwt jwt) {
            // Lấy client_id từ claim JWT đã được Keycloak ký
            String clientId = jwt.getClaimAsString("azp");
            if (!StringUtils.hasText(clientId)) {
                clientId = jwt.getClaimAsString("client_id");
            }
            String clientName = resolveClientName(clientId);

            String keycloakUserId = jwt.getSubject();
            String username = jwt.getClaimAsString("preferred_username");
            if (!StringUtils.hasText(username)) {
                username = keycloakUserId;
            }

            String role = null;
            for (GrantedAuthority ga : auth.getAuthorities()) {
                String authority = ga.getAuthority();
                if ("ROLE_ADMIN".equals(authority)) {
                    role = "ADMIN";
                    break;
                } else if ("ROLE_AGENT".equals(authority)) {
                    role = "AGENT";
                } else if ("ROLE_USER".equals(authority) && role == null) {
                    role = "USER";
                }
            }
            if (role == null && StringUtils.hasText(auth.getName())) {
                role = "USER";
            }

            // Tìm thông tin User và Dealer trong database nội bộ (Fetch join để tránh LazyInitializationException)
            Long userId = null;
            Long dealerId = null;
            String dealerName = null;
            try {
                Optional<User> userOpt = Optional.empty();
                if (StringUtils.hasText(keycloakUserId)) {
                    userOpt = userRepository.findByKeycloakUserIdWithDealer(keycloakUserId);
                }
                if (userOpt.isEmpty() && StringUtils.hasText(username)) {
                    userOpt = userRepository.findByUsernameWithDealer(username);
                }
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    userId = user.getId();
                    if (user.getDealer() != null) {
                        dealerId = user.getDealer().getId();
                        dealerName = user.getDealer().getName();
                    }
                }
            } catch (Exception ex) {
                log.warn("[AUDIT_IDENTITY_LOOKUP_WARN] Could not resolve user/dealer entity: {}", ex.getMessage());
            }

            return new IdentityDetails(clientId, clientName, "USER", userId, keycloakUserId, username, role, dealerId, dealerName);
        }

        // Trường hợp Authenticated khác nhưng không phải JWT người dùng
        return new IdentityDetails(null, null, "CLIENT_ONLY", null, null, auth.getName(), null, null, null);
    }

    private String resolveClientName(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            return null;
        }
        return KNOWN_CLIENT_NAMES.getOrDefault(clientId, clientId);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            String firstIp = xForwardedFor.split(",")[0].trim();
            // Basic sanitization
            if (firstIp.length() <= 50) {
                return firstIp;
            }
        }
        return request.getRemoteAddr();
    }

    private String sanitizeUserAgent(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return null;
        }
        String clean = userAgent.replaceAll("[\\r\\n]", " ").trim();
        return clean.length() > 500 ? clean.substring(0, 500) : clean;
    }

    private record IdentityDetails(
            String clientId,
            String clientName,
            String principalType,
            Long userId,
            String keycloakUserId,
            String username,
            String role,
            Long dealerId,
            String dealerName
    ) {}
}
