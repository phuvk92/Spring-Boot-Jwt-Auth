package com.example.svgmanager.util;

import com.example.svgmanager.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<Authentication> getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return Optional.of(authentication);
        }
        return Optional.empty();
    }

    public static Optional<Jwt> getCurrentJwt() {
        return getAuthentication()
                .map(Authentication::getPrincipal)
                .filter(Jwt.class::isInstance)
                .map(Jwt.class::cast);
    }

    public static String getCurrentUsername() {
        return getAuthentication()
                .map(Authentication::getName)
                .orElseThrow(() -> new UnauthorizedException("User is not authenticated"));
    }

    public static boolean hasRole(String role) {
        String targetRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return getAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(targetRole::equals))
                .orElse(false);
    }

    public static boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    public static boolean isAgent() {
        return hasRole("ROLE_AGENT");
    }
}
