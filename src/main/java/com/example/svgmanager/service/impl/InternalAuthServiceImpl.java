package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.internal.InternalChangePasswordRequest;
import com.example.svgmanager.dto.internal.InternalChangePasswordResponse;
import com.example.svgmanager.dto.internal.InternalLoginRequest;
import com.example.svgmanager.dto.internal.InternalLoginResponse;
import com.example.svgmanager.dto.internal.InternalLogoutRequest;
import com.example.svgmanager.dto.internal.InternalLogoutResponse;
import com.example.svgmanager.dto.internal.InternalRefreshTokenRequest;
import com.example.svgmanager.dto.internal.InternalUserResponse;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.UnauthorizedException;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.InternalAuthService;
import com.example.svgmanager.service.KeycloakUserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
public class InternalAuthServiceImpl implements InternalAuthService {

    private static final Logger log = LoggerFactory.getLogger(InternalAuthServiceImpl.class);

    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String tokenUri;
    private final String logoutUri;
    private final String clientId;

    @Autowired
    public InternalAuthServiceImpl(
            UserRepository userRepository,
            KeycloakUserService keycloakUserService,
            CurrentUserService currentUserService,
            AuditLogService auditLogService,
            ObjectMapper objectMapper,
            @Value("${app.keycloak.token-uri:${KEYCLOAK_AUTH_SERVER_URL:http://localhost:8180}/realms/${KEYCLOAK_REALM:cutting}/protocol/openid-connect/token}") String tokenUri,
            @Value("${app.keycloak.logout-uri:}") String logoutUri,
            @Value("${app.keycloak.client-id:${KEYCLOAK_CLIENT_ID:cutting-manager-web}}") String clientId
    ) {
        this.userRepository = userRepository;
        this.keycloakUserService = keycloakUserService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
        this.tokenUri = tokenUri;
        this.logoutUri = (logoutUri != null && !logoutUri.isBlank())
                ? logoutUri
                : tokenUri.replaceFirst("/token$", "/logout");
        this.clientId = clientId;
        this.restClient = RestClient.builder().build();
    }

    public InternalAuthServiceImpl(
            UserRepository userRepository,
            KeycloakUserService keycloakUserService,
            CurrentUserService currentUserService,
            AuditLogService auditLogService,
            ObjectMapper objectMapper,
            RestClient restClient,
            String tokenUri,
            String logoutUri,
            String clientId
    ) {
        this.userRepository = userRepository;
        this.keycloakUserService = keycloakUserService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
        this.tokenUri = tokenUri;
        this.logoutUri = (logoutUri != null && !logoutUri.isBlank())
                ? logoutUri
                : tokenUri.replaceFirst("/token$", "/logout");
        this.clientId = clientId;
    }

    @Override
    @Transactional
    public InternalLoginResponse login(InternalLoginRequest request) {
        String loginIdentifier = request.username() != null ? request.username().trim() : "";
        String device = request.device();
        String ipAddress = request.ipAddress();
        String clientInfo = formatClientInfo(device, ipAddress);

        // 1. Verify user exists in application database by email (gmail) or username
        Optional<User> userOpt = userRepository.findByEmail(loginIdentifier);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(loginIdentifier.toLowerCase());
        }
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(loginIdentifier);
        }

        if (userOpt.isEmpty()) {
            log.warn("[INTERNAL_LOGIN_REJECTED] User not found in application DB: loginIdentifier='{}'{}", loginIdentifier, clientInfo);
            throw new UnauthorizedException("Invalid username or password");
        }

        User user = userOpt.get();

        // 2. Verify user is active, not deleted, and linked to Keycloak
        if (!user.isEnabled() || user.isDeleted() || user.getKeycloakUserId() == null) {
            log.warn("[INTERNAL_LOGIN_REJECTED] Inactive, deleted or unlinked user: id={}, username='{}', email='{}', enabled={}, deleted={}{}",
                    user.getId(), user.getUsername(), user.getEmail(), user.isEnabled(), user.isDeleted(), clientInfo);
            throw new UnauthorizedException("Invalid username or password");
        }

        // 3. Delegate authentication to Keycloak token endpoint
        String keycloakUser = (user.getUsername() != null && !user.getUsername().isBlank())
                ? user.getUsername()
                : loginIdentifier;

        Map<String, Object> tokenResponse;
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "password");
            form.add("client_id", clientId);
            form.add("username", keycloakUser);
            form.add("password", request.password());

            ResponseEntity<Map> response = restClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toEntity(Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                tokenResponse = response.getBody();
            } else {
                log.warn("[INTERNAL_LOGIN_FAILED] Non-2xx response from Keycloak for user '{}': status={}{}", keycloakUser, response.getStatusCode(), clientInfo);
                auditLogService.log(user.getUsername(), user.getRole() != null ? user.getRole().name() : "USER",
                        "LOGIN_FAILED", "User", user.getId(), "Keycloak returned " + response.getStatusCode() + clientInfo);
                throw new UnauthorizedException("Invalid username or password");
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 400 || e.getStatusCode().value() == 401) {
                log.warn("[INTERNAL_LOGIN_FAILED] Invalid credentials for user '{}': {}{}", keycloakUser, e.getMessage(), clientInfo);
                auditLogService.log(user.getUsername(), user.getRole() != null ? user.getRole().name() : "USER",
                        "LOGIN_FAILED", "User", user.getId(), "Invalid credentials" + clientInfo);
                throw new UnauthorizedException("Invalid username or password");
            }
            log.error("[INTERNAL_LOGIN_ERROR] Keycloak client error during login for user '{}': {}{}", keycloakUser, e.getMessage(), clientInfo);
            throw new UnauthorizedException("Authentication service is temporarily unavailable");
        } catch (HttpServerErrorException e) {
            log.error("[INTERNAL_LOGIN_ERROR] Keycloak server error during login for user '{}': {}{}", keycloakUser, e.getMessage(), clientInfo);
            throw new UnauthorizedException("Authentication service is temporarily unavailable");
        } catch (Exception e) {
            log.error("[INTERNAL_LOGIN_ERROR] Keycloak authentication service error: {}{}", e.getMessage(), clientInfo);
            throw new UnauthorizedException("Authentication service is temporarily unavailable");
        }

        // 4. Map user response
        Long dealerId = (user.getDealer() != null) ? user.getDealer().getId() : null;
        String dealerName = (user.getDealer() != null) ? user.getDealer().getName() : null;
        String displayName = (user.getFullName() != null && !user.getFullName().isBlank()) ? user.getFullName() : user.getUsername();
        String roleStr = (user.getRole() != null) ? user.getRole().name() : "USER";

        InternalUserResponse userResponse = new InternalUserResponse(
                user.getId(),
                user.getUsername(),
                displayName,
                dealerId,
                dealerName,
                roleStr
        );

        String accessToken = (String) tokenResponse.get("access_token");
        String refreshToken = (String) tokenResponse.get("refresh_token");
        String tokenType = tokenResponse.containsKey("token_type") ? (String) tokenResponse.get("token_type") : "Bearer";
        Long expiresIn = tokenResponse.containsKey("expires_in") ? ((Number) tokenResponse.get("expires_in")).longValue() : 3600L;

        // 5. Audit log successful login
        auditLogService.log(user.getUsername(), roleStr, "LOGIN_SUCCESS", "User", user.getId(),
                "Client máy cắt đăng nhập thành công: " + (user.getEmail() != null ? user.getEmail() : user.getUsername()) + clientInfo);

        log.info("[INTERNAL_LOGIN_SUCCESS] User '{}' (email='{}', id={}, dealerId={}) successfully logged in via Client app{}",
                user.getUsername(), user.getEmail(), user.getId(), dealerId, clientInfo);

        return new InternalLoginResponse(accessToken, refreshToken, tokenType, expiresIn, userResponse);
    }

    @Override
    @Transactional
    public InternalLoginResponse refreshToken(InternalRefreshTokenRequest request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new BadRequestException("Refresh token cannot be blank");
        }

        String device = request.device();
        String ipAddress = request.ipAddress();
        String clientInfo = formatClientInfo(device, ipAddress);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("refresh_token", request.refreshToken().trim());

        Map<String, Object> tokenResponse;
        try {
            ResponseEntity<Map> response = restClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toEntity(Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                tokenResponse = response.getBody();
            } else {
                log.warn("[INTERNAL_REFRESH_TOKEN_FAILED] Keycloak returned status: {}{}", response.getStatusCode(), clientInfo);
                throw new UnauthorizedException("Invalid or expired refresh token");
            }
        } catch (HttpClientErrorException e) {
            log.warn("[INTERNAL_REFRESH_TOKEN_FAILED] Refresh token rejected by Keycloak: {}{}", e.getMessage(), clientInfo);
            throw new UnauthorizedException("Invalid or expired refresh token");
        } catch (Exception e) {
            log.error("[INTERNAL_REFRESH_TOKEN_ERROR] Error connecting to Keycloak token endpoint: {}{}", e.getMessage(), clientInfo);
            throw new UnauthorizedException("Authentication service is temporarily unavailable");
        }

        String accessToken = (String) tokenResponse.get("access_token");
        String newRefreshToken = (String) tokenResponse.getOrDefault("refresh_token", request.refreshToken());
        String tokenType = tokenResponse.containsKey("token_type") ? (String) tokenResponse.get("token_type") : "Bearer";
        Long expiresIn = tokenResponse.containsKey("expires_in") ? ((Number) tokenResponse.get("expires_in")).longValue() : 3600L;

        // Extract user from access token payload
        User user = extractAndValidateUser(accessToken);

        Long dealerId = (user.getDealer() != null) ? user.getDealer().getId() : null;
        String dealerName = (user.getDealer() != null) ? user.getDealer().getName() : null;
        String displayName = (user.getFullName() != null && !user.getFullName().isBlank()) ? user.getFullName() : user.getUsername();
        String roleStr = (user.getRole() != null) ? user.getRole().name() : "USER";

        InternalUserResponse userResponse = new InternalUserResponse(
                user.getId(),
                user.getUsername(),
                displayName,
                dealerId,
                dealerName,
                roleStr
        );

        auditLogService.log(user.getUsername(), roleStr, "TOKEN_REFRESH", "User", user.getId(),
                "Client máy cắt làm mới token thành công: " + (user.getEmail() != null ? user.getEmail() : user.getUsername()) + clientInfo);

        log.info("[INTERNAL_REFRESH_TOKEN_SUCCESS] Token refreshed for user '{}' (id={}, dealerId={}){}",
                user.getUsername(), user.getId(), dealerId, clientInfo);

        return new InternalLoginResponse(accessToken, newRefreshToken, tokenType, expiresIn, userResponse);
    }

    @Override
    @Transactional
    public InternalLogoutResponse logout(InternalLogoutRequest request) {
        String refreshToken = (request != null && request.refreshToken() != null) ? request.refreshToken().trim() : null;
        String clientInfo = (request != null) ? formatClientInfo(request.device(), request.ipAddress()) : "";

        String username = "anonymous";
        String roleStr = "USER";
        Long userId = null;

        // 1. Check if caller has an active JWT bearer token
        Optional<Jwt> currentJwtOpt = currentUserService.getCurrentJwt();
        if (currentJwtOpt.isPresent()) {
            Jwt jwt = currentJwtOpt.get();
            username = jwt.getClaimAsString("preferred_username");
            if (username == null || username.isBlank()) {
                username = jwt.getSubject();
            }
            try {
                User currentUser = currentUserService.getCurrentUser();
                userId = currentUser.getId();
                roleStr = currentUser.getRole() != null ? currentUser.getRole().name() : "USER";
            } catch (Exception ignored) {
            }
        }

        // 2. Revoke refresh token via Keycloak logout endpoint
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
                form.add("client_id", clientId);
                form.add("refresh_token", refreshToken);

                ResponseEntity<Void> response = restClient.post()
                        .uri(logoutUri)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(form)
                        .retrieve()
                        .toBodilessEntity();

                log.info("[INTERNAL_LOGOUT_KEYCLOAK] Keycloak logout endpoint responded with status {}{}", response.getStatusCode(), clientInfo);
            } catch (HttpClientErrorException e) {
                // Keycloak returns 400 if token was already expired or revoked; logout is still successfully acknowledged
                log.warn("[INTERNAL_LOGOUT_KEYCLOAK] Keycloak token already invalid or expired during logout: {}{}", e.getMessage(), clientInfo);
            } catch (Exception e) {
                log.error("[INTERNAL_LOGOUT_ERROR] Error during Keycloak logout call: {}{}", e.getMessage(), clientInfo);
            }

            // If username wasn't resolved from Bearer token, attempt extraction from refresh token payload for audit
            if ("anonymous".equals(username)) {
                try {
                    String[] parts = refreshToken.split("\\.");
                    if (parts.length >= 2) {
                        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                        JsonNode payload = objectMapper.readTree(payloadJson);
                        String sub = payload.path("sub").asText(null);
                        String prefUser = payload.path("preferred_username").asText(sub);
                        if (prefUser != null) {
                            username = prefUser;
                        }
                        if (sub != null) {
                            userRepository.findByKeycloakUserId(sub).ifPresent(u -> {
                                auditLogService.log(u.getUsername(), u.getRole() != null ? u.getRole().name() : "USER",
                                        "LOGOUT", "User", u.getId(), "Client máy cắt đăng xuất: " + (u.getEmail() != null ? u.getEmail() : u.getUsername()) + clientInfo);
                            });
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (userId != null) {
            auditLogService.log(username, roleStr, "LOGOUT", "User", userId,
                    "Client máy cắt đăng xuất thành công: " + username + clientInfo);
        }

        log.info("[INTERNAL_LOGOUT_SUCCESS] User '{}' logged out successfully{}", username, clientInfo);

        return new InternalLogoutResponse(true, "Logged out successfully");
    }

    @Override
    @Transactional
    public InternalChangePasswordResponse changePassword(InternalChangePasswordRequest request) {
        String clientInfo = (request != null) ? formatClientInfo(request.device(), request.ipAddress()) : "";

        // 1. Validate request inputs
        if (request == null || request.currentPassword() == null || request.currentPassword().isBlank()) {
            throw new BadRequestException("Current password is required");
        }
        if (request.newPassword() == null || request.newPassword().isBlank()) {
            throw new BadRequestException("New password is required");
        }
        if (request.confirmPassword() == null || request.confirmPassword().isBlank()) {
            throw new BadRequestException("Confirm password is required");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("Confirm password does not match new password");
        }
        if (request.newPassword().equals(request.currentPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }

        // 2. Identify currently authenticated user
        User currentUser = currentUserService.getCurrentUser();
        String username = currentUser.getUsername();
        String keycloakUserId = currentUser.getKeycloakUserId();

        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            log.error("[INTERNAL_CHANGE_PASSWORD_ERROR] User '{}' has no Keycloak user ID{}", username, clientInfo);
            throw new BadRequestException("User account is not properly linked with authentication service");
        }

        // 3. Verify current password by authenticating against Keycloak token endpoint
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "password");
            form.add("client_id", clientId);
            form.add("username", username);
            form.add("password", request.currentPassword());

            ResponseEntity<Map> response = restClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toEntity(Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("[INTERNAL_CHANGE_PASSWORD_REJECTED] Current password verification failed for user '{}'{}", username, clientInfo);
                throw new BadRequestException("Current password is incorrect");
            }
        } catch (HttpClientErrorException e) {
            log.warn("[INTERNAL_CHANGE_PASSWORD_REJECTED] Current password is incorrect for user '{}'{}", username, clientInfo);
            throw new BadRequestException("Current password is incorrect");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("[INTERNAL_CHANGE_PASSWORD_ERROR] Error verifying current password for user '{}': {}{}", username, e.getMessage(), clientInfo);
            throw new UnauthorizedException("Authentication service is temporarily unavailable");
        }

        // 4. Update password via Keycloak admin service
        keycloakUserService.resetPassword(keycloakUserId, request.newPassword());

        // 5. Audit log
        auditLogService.log(
                username,
                currentUser.getRole().name(),
                "PASSWORD_CHANGED",
                "User",
                currentUser.getId(),
                "Client máy cắt đổi mật khẩu thành công" + clientInfo
        );

        log.info("[INTERNAL_PASSWORD_CHANGED] Successfully changed password for user '{}'{}", username, clientInfo);

        return new InternalChangePasswordResponse(true, "Password changed successfully");
    }

    private User extractAndValidateUser(String accessToken) {
        String username = null;
        String sub = null;
        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length >= 2) {
                String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                JsonNode payload = objectMapper.readTree(payloadJson);
                sub = payload.path("sub").asText(null);
                username = payload.path("preferred_username").asText(sub);
            }
        } catch (Exception e) {
            log.warn("[INTERNAL_EXTRACT_USER_WARN] Could not parse access token payload: {}", e.getMessage());
        }

        Optional<User> userOpt = Optional.empty();
        if (sub != null && !sub.isBlank()) {
            userOpt = userRepository.findByKeycloakUserId(sub);
        }
        if (userOpt.isEmpty() && username != null && !username.isBlank()) {
            userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByEmail(username);
            }
        }

        if (userOpt.isEmpty()) {
            log.warn("[INTERNAL_REFRESH_TOKEN_REJECTED] User not found in application DB: sub='{}', username='{}'", sub, username);
            throw new UnauthorizedException("User account not found");
        }

        User user = userOpt.get();
        if (!user.isEnabled() || user.isDeleted()) {
            log.warn("[INTERNAL_REFRESH_TOKEN_REJECTED] User is inactive or deleted: id={}, username='{}'", user.getId(), user.getUsername());
            throw new UnauthorizedException("User account is inactive or disabled");
        }

        return user;
    }

    private String formatClientInfo(String device, String ipAddress) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(device)) {
            sb.append("Thiết bị: ").append(device.trim());
        }
        if (StringUtils.hasText(ipAddress)) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("IP: ").append(ipAddress.trim());
        }
        return sb.length() > 0 ? " [" + sb + "]" : "";
    }
}
