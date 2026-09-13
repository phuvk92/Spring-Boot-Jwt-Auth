package com.example.svgmanager.util;

import com.example.svgmanager.exception.UnauthorizedException;
import com.example.svgmanager.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static UserPrincipal getRequiredCurrentUserPrincipal() {
        return getCurrentUserPrincipal()
                .orElseThrow(() -> new UnauthorizedException("User is not authenticated"));
    }

    public static Long getCurrentUserId() {
        return getRequiredCurrentUserPrincipal().getId();
    }
}
