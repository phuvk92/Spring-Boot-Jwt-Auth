package com.example.svgmanager.security;

import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.service.UserDeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * F-57 — mọi request dữ liệu của app cắt ({@code /api/internal/**}) phải đến từ một phiên còn gắn
 * với máy ACTIVE. Máy bị gỡ → 401 {@code SESSION_REVOKED} ngay ở request kế tiếp, không đợi token hết hạn.
 *
 * Không phải @Component: đăng ký tay trong SecurityConfig, chạy SAU khi JWT đã được xác thực.
 */
public class DeviceSessionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DeviceSessionFilter.class);

    private static final String INTERNAL_PREFIX = "/api/internal/";

    /**
     * Endpoint app-cắt ngoài {@code /api/internal/**} cũng phải gắn phiên máy ACTIVE —
     * máy bị gỡ → 401 SESSION_REVOKED như phần còn lại (AC của NGO-165).
     * Liệt kê tường minh thay vì cả prefix /api/v1/ vì catalog/files vẫn phục vụ portal web.
     */
    private static final Set<String> EXTRA_ENFORCED_PATHS = Set.of(
            "/api/v1/branding"
    );

    /** Đường tự xử lý thiết bị trong service (đăng nhập, làm mới, đăng xuất) — không cần phiên sẵn có. */
    private static final Set<String> EXEMPT = Set.of(
            "/api/internal/auth/login",
            "/api/internal/auth/refresh",
            "/api/internal/auth/refresh-token",
            "/api/internal/auth/logout"
    );

    private final UserDeviceService userDeviceService;
    private final ObjectMapper objectMapper;

    public DeviceSessionFilter(UserDeviceService userDeviceService, ObjectMapper objectMapper) {
        this.userDeviceService = userDeviceService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !userDeviceService.isEnforced()
                || !(path.startsWith(INTERNAL_PREFIX) || EXTRA_ENFORCED_PATHS.contains(path))
                || EXEMPT.contains(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            // Chưa xác thực — để Spring Security trả 401 như thường
            chain.doFilter(request, response);
            return;
        }

        String sessionId = jwt.getClaimAsString("sid");
        if (userDeviceService.isSessionActive(sessionId)) {
            chain.doFilter(request, response);
            return;
        }

        log.warn("[DEVICE_SESSION_REJECTED] {} {} sid='{}' user='{}'",
                request.getMethod(), request.getRequestURI(), sessionId, jwt.getClaimAsString("preferred_username"));
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Máy này đã bị gỡ khỏi tài khoản hoặc phiên không còn hiệu lực. Vui lòng đăng nhập lại.")
                .path(request.getRequestURI())
                .code(ErrorCodes.SESSION_REVOKED)
                .build());
    }
}
