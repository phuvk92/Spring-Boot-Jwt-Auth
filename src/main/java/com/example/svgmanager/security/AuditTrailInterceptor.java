package com.example.svgmanager.security;

import com.example.svgmanager.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * Interceptor tự động ghi nhận Audit Trail & Activity Log
 * mỗi khi người dùng (USER) hoặc đại lý (AGENT) gọi bất kỳ API nào trong hệ thống.
 */
@Component
public class AuditTrailInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuditTrailInterceptor.class);
    private static final String START_TIME_ATTR = "audit_trail_start_time";

    private final AuditLogService auditLogService;

    public AuditTrailInterceptor(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            String uri = request.getRequestURI();

            // Bỏ qua các endpoint không phải nghiệp vụ hoặc tránh vòng lặp tự ghi log
            if (uri.startsWith("/api/audit-logs")
                    || uri.startsWith("/swagger-ui")
                    || uri.startsWith("/v3/api-docs")
                    || uri.startsWith("/actuator")) {
                return;
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return;
            }

            // Xác định vai trò của người gọi
            boolean isUser = false;
            boolean isAgent = false;
            boolean isAdmin = false;
            for (GrantedAuthority ga : auth.getAuthorities()) {
                String authority = ga.getAuthority();
                if ("ROLE_USER".equals(authority)) {
                    isUser = true;
                } else if ("ROLE_AGENT".equals(authority)) {
                    isAgent = true;
                } else if ("ROLE_ADMIN".equals(authority)) {
                    isAdmin = true;
                }
            }

            String actorRole = null;
            if (isUser) {
                actorRole = "USER";
            } else if (isAgent) {
                actorRole = "AGENT";
            } else if (isAdmin) {
                actorRole = "ADMIN";
            }

            // Yêu cầu nghiệp vụ: ghi nhận toàn bộ API của USER và ĐẠI LÝ (AGENT)
            if (actorRole == null) {
                return;
            }

            // Xác định danh tính actor (ưu tiên username/email từ JWT)
            String actor = auth.getName();
            if (auth.getPrincipal() instanceof Jwt jwt) {
                String preferredUsername = jwt.getClaimAsString("preferred_username");
                if (StringUtils.hasText(preferredUsername)) {
                    actor = preferredUsername;
                }
            }

            // Tính thời gian xử lý (latency)
            long startTime = request.getAttribute(START_TIME_ATTR) instanceof Long l ? l : System.currentTimeMillis();
            long duration = System.currentTimeMillis() - startTime;

            // Xác định action và entity
            String method = request.getMethod();
            String action = method + " " + uri;
            if (action.length() > 100) {
                action = action.substring(0, 100);
            }

            String entity = "API";
            if (handler instanceof HandlerMethod hm) {
                String beanName = hm.getBeanType().getSimpleName();
                entity = beanName.replace("Controller", "");
            } else {
                String[] segments = uri.split("/");
                if (segments.length >= 3) {
                    entity = segments[2];
                }
            }
            if (entity.length() > 100) {
                entity = entity.substring(0, 100);
            }

            // Trích xuất entityId nếu có trong URI path variables
            Long entityId = extractEntityId(request);

            // Thu thập thông số định danh thiết bị & IP client
            String clientIp = resolveIp(request);
            String device = resolveDevice(request);
            int status = response.getStatus();
            String queryString = request.getQueryString();

            // Tổng hợp chi tiết log
            StringBuilder details = new StringBuilder();
            details.append("HTTP ").append(status)
                   .append(" (").append(duration).append("ms)")
                   .append(" | Path: ").append(uri);
            if (StringUtils.hasText(queryString)) {
                details.append("?").append(queryString);
            }
            details.append(" | IP: ").append(clientIp);
            if (StringUtils.hasText(device)) {
                details.append(" | Device: ").append(device);
            }
            if (handler instanceof HandlerMethod hm) {
                details.append(" | Handler: ").append(hm.getBeanType().getSimpleName()).append(".").append(hm.getMethod().getName());
            }
            if (ex != null) {
                details.append(" | Exception: ").append(ex.getMessage());
            }

            // Ghi nhật ký audit log
            auditLogService.log(actor, actorRole, action, entity, entityId, details.toString());

        } catch (Exception e) {
            log.warn("Error recording audit trail in interceptor: {}", e.getMessage());
        }
    }

    private Long extractEntityId(HttpServletRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (pathVariables != null) {
                for (String val : pathVariables.values()) {
                    try {
                        return Long.parseLong(val);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String resolveIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveDevice(HttpServletRequest request) {
        String device = request.getParameter("device");
        if (StringUtils.hasText(device)) {
            return device.trim();
        }
        String thietBi = request.getParameter("thietBi");
        if (StringUtils.hasText(thietBi)) {
            return thietBi.trim();
        }
        String headerDevice = request.getHeader("X-Device");
        if (StringUtils.hasText(headerDevice)) {
            return headerDevice.trim();
        }
        return null;
    }
}
