package com.example.svgmanager.security;

import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.UnauthorizedException;
import com.example.svgmanager.exception.UserAccountExpiredException;
import com.example.svgmanager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Service
public class CurrentUserService {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserService.class);
    public static final String CACHED_AUTH_USER_ATTR = "CACHED_AUTH_USER";

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<Jwt> getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        return Optional.empty();
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    public boolean isAgent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_AGENT"::equals);
    }

    public boolean isUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_USER"::equals);
    }

    public Role getCurrentRole() {
        if (isAdmin()) {
            return Role.ADMIN;
        }
        if (isAgent()) {
            return Role.AGENT;
        }
        return Role.USER;
    }

    public String getCurrentKeycloakUserId() {
        return getCurrentJwt()
                .map(Jwt::getSubject)
                .orElse(null);
    }

    @Transactional
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    @Transactional
    public Optional<User> getOptionalCurrentUser() {
        if (getCurrentJwt().isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(getCurrentUser());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Transactional
    public User getCurrentUser() {
        Jwt jwt = getCurrentJwt()
                .orElseThrow(() -> new UnauthorizedException("No authenticated JWT token found in security context"));

        String keycloakUserId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            username = keycloakUserId;
        }

        Role role = getCurrentRole();

        // 0. Check request-scoped cache first to prevent duplicate DB queries in the same request thread
        RequestAttributes requestAttrs = RequestContextHolder.getRequestAttributes();
        if (requestAttrs instanceof ServletRequestAttributes servletAttrs) {
            Object cached = servletAttrs.getRequest().getAttribute(CACHED_AUTH_USER_ATTR);
            if (cached instanceof User cachedUser && keycloakUserId.equals(cachedUser.getKeycloakUserId())) {
                validateAccountStatus(cachedUser);
                return cachedUser;
            }
        }

        // 1. Find by Keycloak sub (with Dealer eagerly fetched)
        Optional<User> userOpt = userRepository.findByKeycloakUserIdWithDealer(keycloakUserId);
        if (userOpt.isPresent()) {
            User existing = userOpt.get();
            validateAccountStatus(existing);
            boolean changed = false;
            if (!username.equals(existing.getUsername())) {
                if (!userRepository.existsByUsernameAndIdNot(username, existing.getId())) {
                    existing.setUsername(username);
                    changed = true;
                }
            }
            if (existing.getRole() != role) {
                existing.setRole(role);
                changed = true;
            }
            User result = changed ? userRepository.save(existing) : existing;
            if (requestAttrs instanceof ServletRequestAttributes servletAttrs) {
                servletAttrs.getRequest().setAttribute(CACHED_AUTH_USER_ATTR, result);
            }
            return result;
        }

        // 2. Fallback check by username for seeded accounts (e.g., initial Admin / Agent)
        Optional<User> byUsernameOpt = userRepository.findByUsernameWithDealer(username);
        if (byUsernameOpt.isPresent()) {
            User existing = byUsernameOpt.get();
            validateAccountStatus(existing);
            existing.setKeycloakUserId(keycloakUserId);
            existing.setRole(role);
            User saved = userRepository.save(existing);
            if (requestAttrs instanceof ServletRequestAttributes servletAttrs) {
                servletAttrs.getRequest().setAttribute(CACHED_AUTH_USER_ATTR, saved);
            }
            return saved;
        }

        // 3. User was created directly in Keycloak console without application registration
        // Strict boundary: Do NOT auto-provision unknown Keycloak users into PostgreSQL
        log.warn("[UNREGISTERED_USER_REJECTED] Rejected access for unmapped Keycloak user: username='{}', sub='{}'", username, keycloakUserId);
        throw new UnauthorizedException("User account '" + username + "' is not registered in the application. Users must be created through the Admin Application.");
    }

    private void validateAccountStatus(User user) {
        if (user.isDeleted() || !user.isEnabled()) {
            log.warn("[USER_INACTIVE_OR_DELETED] Access rejected for inactive/deleted user: id={}, username='{}', enabled={}, deleted={}",
                    user.getId(), user.getUsername(), user.isEnabled(), user.isDeleted());
            throw new UnauthorizedException("Tài khoản người dùng đã bị vô hiệu hóa hoặc đã bị xóa. Vui lòng liên hệ quản trị viên.", ErrorCodes.ACCOUNT_DISABLED);
        }
        if (user.getRole() != Role.ADMIN && user.isExpired()) {
            log.warn("[USER_ACCOUNT_EXPIRED] Access rejected for expired user: id={}, username='{}', expirationDate={}",
                    user.getId(), user.getUsername(), user.getExpirationDate());
            throw new UserAccountExpiredException("Tài khoản của bạn đã hết hạn sử dụng. Vui lòng liên hệ quản trị viên hoặc đại lý.");
        }
    }

    @Transactional
    public Long getCurrentDealerId() {
        if (isAdmin()) {
            return null; // Admin has unrestricted data scope
        }
        User user = getCurrentUser();
        return user.getDealer() != null ? user.getDealer().getId() : null;
    }

    public Long getCurrentAgentId() {
        if (isAdmin()) {
            return null; // Admin has unrestricted data scope
        }

        User user = getCurrentUser();
        if (user.getRole() == Role.AGENT) {
            return user.getId();
        } else if (user.getRole() == Role.USER && user.getAgent() != null) {
            return user.getAgent().getId();
        }
        return null;
    }
}
