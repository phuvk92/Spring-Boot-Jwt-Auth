package com.example.svgmanager.security;

import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.UnauthorizedException;
import com.example.svgmanager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CurrentUserService {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserService.class);

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
    public User getCurrentUser() {
        Jwt jwt = getCurrentJwt()
                .orElseThrow(() -> new UnauthorizedException("No authenticated JWT token found in security context"));

        String keycloakUserId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            username = keycloakUserId;
        }

        Role role = getCurrentRole();

        // 1. Find by Keycloak sub
        Optional<User> userOpt = userRepository.findByKeycloakUserId(keycloakUserId);
        if (userOpt.isPresent()) {
            User existing = userOpt.get();
            if (existing.isDeleted()) {
                throw new UnauthorizedException("User account has been deactivated/deleted");
            }
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
            if (changed) {
                return userRepository.save(existing);
            }
            return existing;
        }

        // 2. Fallback check by username for seeded accounts (e.g., initial Admin / Agent)
        Optional<User> byUsernameOpt = userRepository.findByUsername(username);
        if (byUsernameOpt.isPresent()) {
            User existing = byUsernameOpt.get();
            if (existing.isDeleted()) {
                throw new UnauthorizedException("User account has been deactivated/deleted");
            }
            existing.setKeycloakUserId(keycloakUserId);
            existing.setRole(role);
            return userRepository.save(existing);
        }

        // 3. User was created directly in Keycloak console without application registration
        // Strict boundary: Do NOT auto-provision unknown Keycloak users into PostgreSQL
        log.warn("[UNREGISTERED_USER_REJECTED] Rejected access for unmapped Keycloak user: username='{}', sub='{}'", username, keycloakUserId);
        throw new UnauthorizedException("User account '" + username + "' is not registered in the application. Users must be created through the Admin Application.");
    }

    @Transactional
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
