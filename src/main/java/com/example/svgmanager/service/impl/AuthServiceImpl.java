package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.request.ChangePasswordRequest;
import com.example.svgmanager.dto.request.LoginRequest;
import com.example.svgmanager.dto.request.RefreshTokenRequest;
import com.example.svgmanager.dto.response.AuthResponse;
import com.example.svgmanager.dto.response.MessageResponse;
import com.example.svgmanager.dto.response.UserResponse;
import com.example.svgmanager.dto.response.UserSummaryResponse;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.UnauthorizedException;
import com.example.svgmanager.mapper.UserMapper;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.AuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Value("${app.keycloak.token-uri:${KEYCLOAK_TOKEN_URI:http://localhost:8180/realms/cutting/protocol/openid-connect/token}}")
    private String tokenUri;

    @Value("${app.keycloak.client-id:${KEYCLOAK_CLIENT_ID:cutting-manager-web}}")
    private String clientId;

    @Value("${app.keycloak.admin-url:${KEYCLOAK_ADMIN_URL:}}")
    private String adminUrl;

    @Value("${app.keycloak.admin-username:${KEYCLOAK_ADMIN_USERNAME:admin}}")
    private String adminUsername;

    @Value("${app.keycloak.admin-password:${KEYCLOAK_ADMIN_PASSWORD:admin}}")
    private String adminPassword;

    @Value("${app.keycloak.realm:${KEYCLOAK_REALM:cutting}}")
    private String realm;

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public AuthServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            CurrentUserService currentUserService,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", clientId);
        formData.add("username", request.getUsername());
        formData.add("password", request.getPassword());

        return exchangeToken(formData);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new BadRequestException("Refresh token cannot be blank");
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", clientId);
        formData.add("refresh_token", request.getRefreshToken());

        return exchangeToken(formData);
    }

    private AuthResponse exchangeToken(MultiValueMap<String, String> formData) {
        try {
            log.info("Requesting Keycloak token from {}", tokenUri);
            ResponseEntity<Map> response = restClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .toEntity(Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String accessToken = (String) body.get("access_token");
                String refreshToken = (String) body.get("refresh_token");
                String tokenType = (String) body.getOrDefault("token_type", "Bearer");
                Number expiresInNum = (Number) body.getOrDefault("expires_in", 3600);
                Long expiresIn = expiresInNum.longValue();

                UserSummaryResponse summary = extractAndSyncUser(accessToken);

                return AuthResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .tokenType(tokenType)
                        .expiresIn(expiresIn)
                        .user(summary)
                        .build();
            }

            throw new UnauthorizedException("Invalid credentials or authentication failed");
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.BadRequest e) {
            log.warn("Keycloak authentication rejected: {}", e.getMessage());
            throw new UnauthorizedException("Invalid username or password");
        } catch (Exception e) {
            log.error("Failed to authenticate with Keycloak at {}: {}", tokenUri, e.getMessage());
            throw new UnauthorizedException("Authentication service is temporarily unavailable: " + e.getMessage());
        }
    }

    private UserSummaryResponse extractAndSyncUser(String accessToken) {
        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode payload = objectMapper.readTree(payloadJson);

            String sub = payload.path("sub").asText();
            String username = payload.path("preferred_username").asText(sub);
            String email = payload.path("email").asText(username + "@keycloak.local");

            Role role = Role.USER;
            JsonNode realmRoles = payload.path("realm_access").path("roles");
            if (realmRoles.isArray()) {
                for (JsonNode r : realmRoles) {
                    String roleName = r.asText();
                    if ("ADMIN".equalsIgnoreCase(roleName)) {
                        role = Role.ADMIN;
                        break;
                    } else if ("AGENT".equalsIgnoreCase(roleName)) {
                        role = Role.AGENT;
                    }
                }
            }

            // Sync user in database
            User user = userRepository.findByKeycloakUserId(sub).orElse(null);
            if (user == null) {
                user = userRepository.findByUsername(username).orElse(null);
                if (user != null) {
                    user.setKeycloakUserId(sub);
                    user.setRole(role);
                    user = userRepository.save(user);
                } else {
                    user = User.builder()
                            .keycloakUserId(sub)
                            .username(username)
                            .email(email)
                            .role(role)
                            .enabled(true)
                            .build();
                    user = userRepository.save(user);
                }
            } else {
                if (user.getRole() != role) {
                    user.setRole(role);
                    user = userRepository.save(user);
                }
            }

            return UserSummaryResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse user claims from token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        User user = currentUserService.getCurrentUser();
        return userMapper.toUserResponse(user);
    }

    private String getBaseKeycloakUrl() {
        if (adminUrl != null && !adminUrl.isBlank()) {
            return adminUrl.replaceAll("/+$", "");
        }
        int idx = tokenUri.indexOf("/realms/");
        if (idx > 0) {
            return tokenUri.substring(0, idx);
        }
        return "http://localhost:8180";
    }

    private String getAdminAccessToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", "admin-cli");
        formData.add("username", adminUsername);
        formData.add("password", adminPassword);

        String masterTokenUri = getBaseKeycloakUrl() + "/realms/master/protocol/openid-connect/token";
        try {
            ResponseEntity<Map> response = restClient.post()
                    .uri(masterTokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .toEntity(Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
            throw new UnauthorizedException("Unable to obtain Keycloak admin token");
        } catch (Exception e) {
            log.error("Failed to obtain Keycloak admin token at {}: {}", masterTokenUri, e.getMessage());
            throw new UnauthorizedException("Failed to obtain Keycloak admin token: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public MessageResponse changePassword(ChangePasswordRequest request) {
        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new BadRequestException("Current password cannot be blank");
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new BadRequestException("New password cannot be blank");
        }
        if (request.getConfirmPassword() != null && !request.getConfirmPassword().isBlank()) {
            if (!request.getConfirmPassword().equals(request.getNewPassword())) {
                throw new BadRequestException("Confirm password does not match new password");
            }
        }

        User currentUser = currentUserService.getCurrentUser();
        String username = currentUser.getUsername();

        // 1. Verify current password by authenticating against Keycloak token endpoint
        MultiValueMap<String, String> verifyData = new LinkedMultiValueMap<>();
        verifyData.add("grant_type", "password");
        verifyData.add("client_id", clientId);
        verifyData.add("username", username);
        verifyData.add("password", request.getCurrentPassword());

        try {
            ResponseEntity<Map> verifyResponse = restClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(verifyData)
                    .retrieve()
                    .toEntity(Map.class);

            if (!verifyResponse.getStatusCode().is2xxSuccessful() || verifyResponse.getBody() == null) {
                throw new BadRequestException("Current password is incorrect");
            }
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.BadRequest e) {
            log.warn("Change password rejected: incorrect current password for user '{}'", username);
            throw new BadRequestException("Current password is incorrect");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error verifying current password for user '{}': {}", username, e.getMessage());
            throw new BadRequestException("Failed to verify current password: " + e.getMessage());
        }

        // 2. Obtain admin access token to call Keycloak Admin API
        String adminToken = getAdminAccessToken();
        String keycloakUserId = currentUser.getKeycloakUserId();

        // 3. If keycloakUserId is not cached, look it up by username
        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            String searchUserUri = getBaseKeycloakUrl() + "/admin/realms/" + realm + "/users?username=" + username + "&exact=true";
            try {
                ResponseEntity<List> searchResponse = restClient.get()
                        .uri(searchUserUri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .retrieve()
                        .toEntity(List.class);

                if (searchResponse.getStatusCode().is2xxSuccessful() && searchResponse.getBody() != null && !searchResponse.getBody().isEmpty()) {
                    Map<?, ?> firstUser = (Map<?, ?>) searchResponse.getBody().get(0);
                    keycloakUserId = (String) firstUser.get("id");
                    currentUser.setKeycloakUserId(keycloakUserId);
                    userRepository.save(currentUser);
                }
            } catch (Exception e) {
                log.error("Failed to query Keycloak user ID for '{}': {}", username, e.getMessage());
                throw new BadRequestException("Failed to query Keycloak user profile");
            }
        }

        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            throw new BadRequestException("User not found in Keycloak realm");
        }

        // 4. Reset password via Keycloak Admin API
        String resetPasswordUri = getBaseKeycloakUrl() + "/admin/realms/" + realm + "/users/" + keycloakUserId + "/reset-password";
        Map<String, Object> passwordPayload = Map.of(
                "type", "password",
                "value", request.getNewPassword(),
                "temporary", false
        );

        try {
            ResponseEntity<Void> resetResponse = restClient.put()
                    .uri(resetPasswordUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(passwordPayload)
                    .retrieve()
                    .toBodilessEntity();

            if (resetResponse.getStatusCode().is2xxSuccessful()) {
                log.info("[PASSWORD_CHANGED] Password changed successfully for user '{}'", username);
                return MessageResponse.of("Password changed successfully");
            }
            throw new BadRequestException("Failed to update password in Keycloak");
        } catch (Exception e) {
            log.error("Failed to reset password in Keycloak for user '{}': {}", username, e.getMessage());
            throw new BadRequestException("Failed to update password: " + e.getMessage());
        }
    }
}
