package com.example.svgmanager.service.impl;

import com.example.svgmanager.entity.Role;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ConflictException;
import com.example.svgmanager.service.KeycloakUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.*;

@Service
public class KeycloakUserServiceImpl implements KeycloakUserService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakUserServiceImpl.class);

    @Value("${app.keycloak.sync-enabled:true}")
    private boolean syncEnabled;

    @Value("${app.keycloak.base-url:${KEYCLOAK_BASE_URL:http://localhost:8180}}")
    private String baseUrl;

    @Value("${app.keycloak.admin-url:${KEYCLOAK_ADMIN_URL:}}")
    private String adminUrl;

    @Value("${app.keycloak.admin-username:${KEYCLOAK_ADMIN_USERNAME:admin}}")
    private String adminUsername;

    @Value("${app.keycloak.admin-password:${KEYCLOAK_ADMIN_PASSWORD:admin}}")
    private String adminPassword;

    @Value("${app.keycloak.realm:${KEYCLOAK_REALM:cutting}}")
    private String realm;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public KeycloakUserServiceImpl(ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().build();
        this.objectMapper = objectMapper;
    }

    private String getBaseUrl() {
        if (StringUtils.hasText(adminUrl)) {
            return adminUrl.replaceAll("/+$", "");
        }
        if (StringUtils.hasText(baseUrl)) {
            return baseUrl.replaceAll("/+$", "");
        }
        return "http://localhost:8180";
    }

    private String getAdminToken() {
        String tokenUri = getBaseUrl() + "/realms/master/protocol/openid-connect/token";
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", "admin-cli");
        formData.add("username", adminUsername);
        formData.add("password", adminPassword);

        try {
            ResponseEntity<Map> response = restClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .toEntity(Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
            throw new BadRequestException("Failed to obtain admin token from Keycloak");
        } catch (Exception e) {
            log.error("Error obtaining Keycloak admin token: {}", e.getMessage());
            throw new BadRequestException("Keycloak service is unavailable: " + e.getMessage());
        }
    }

    @Override
    public String createUser(String username, String email, String password, Role role, boolean enabled) {
        return createUser(username, email, password, role, enabled, null);
    }

    @Override
    public String createUser(String username, String email, String password, Role role, boolean enabled, String fullName) {
        if (!syncEnabled) {
            log.info("[KEYCLOAK_SYNC_DISABLED] Simulated Keycloak user creation for username '{}'", username);
            return UUID.randomUUID().toString();
        }

        String adminToken = getAdminToken();
        String createUrl = getBaseUrl() + "/admin/realms/" + realm + "/users";

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("username", username);
        userPayload.put("email", email);
        userPayload.put("enabled", enabled);
        userPayload.put("emailVerified", true);
        userPayload.put("requiredActions", Collections.emptyList());

        if (StringUtils.hasText(fullName)) {
            String[] parts = fullName.trim().split("\\s+", 2);
            userPayload.put("firstName", parts[0]);
            userPayload.put("lastName", parts.length > 1 ? parts[1] : parts[0]);
        } else {
            userPayload.put("firstName", username);
            userPayload.put("lastName", "User");
        }

        if (StringUtils.hasText(password)) {
            userPayload.put("credentials", List.of(Map.of(
                    "type", "password",
                    "value", password,
                    "temporary", false
            )));
        }

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(createUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userPayload)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode() == HttpStatus.CREATED) {
                URI location = response.getHeaders().getLocation();
                String keycloakUserId = null;
                if (location != null) {
                    String path = location.getPath();
                    keycloakUserId = path.substring(path.lastIndexOf('/') + 1);
                }

                if (!StringUtils.hasText(keycloakUserId)) {
                    keycloakUserId = findUserIdByUsername(username, adminToken);
                }

                if (StringUtils.hasText(keycloakUserId) && role != null) {
                    assignRoleInternal(keycloakUserId, role.name(), adminToken);
                }

                log.info("[KEYCLOAK_USER_CREATED] Created user in Keycloak: id={}, username='{}', role={}",
                        keycloakUserId, username, role);
                return keycloakUserId;
            }

            throw new BadRequestException("Failed to create user in Keycloak");
        } catch (HttpClientErrorException.Conflict e) {
            log.warn("Keycloak conflict creating user '{}': {}", username, e.getMessage());
            throw new ConflictException("User already exists in Keycloak with username or email");
        } catch (ConflictException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create user in Keycloak: {}", e.getMessage());
            throw new BadRequestException("Keycloak user creation failed: " + e.getMessage());
        }
    }

    private String findUserIdByUsername(String username, String adminToken) {
        String searchUrl = getBaseUrl() + "/admin/realms/" + realm + "/users?username=" + username + "&exact=true";
        try {
            ResponseEntity<List> response = restClient.get()
                    .uri(searchUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .retrieve()
                    .toEntity(List.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isEmpty()) {
                Map<?, ?> first = (Map<?, ?>) response.getBody().get(0);
                return (String) first.get("id");
            }
        } catch (Exception e) {
            log.warn("Failed to lookup Keycloak user id for username '{}': {}", username, e.getMessage());
        }
        return null;
    }

    @Override
    public void updateUser(String keycloakUserId, String email, Boolean enabled) {
        if (!syncEnabled || !StringUtils.hasText(keycloakUserId)) {
            return;
        }

        String adminToken = getAdminToken();
        String updateUrl = getBaseUrl() + "/admin/realms/" + realm + "/users/" + keycloakUserId;

        Map<String, Object> updatePayload = new HashMap<>();
        if (StringUtils.hasText(email)) {
            updatePayload.put("email", email);
        }
        if (enabled != null) {
            updatePayload.put("enabled", enabled);
        }

        try {
            restClient.put()
                    .uri(updateUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(updatePayload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[KEYCLOAK_USER_UPDATED] Updated user in Keycloak: id={}, email='{}', enabled={}",
                    keycloakUserId, email, enabled);
        } catch (Exception e) {
            log.error("Failed to update user in Keycloak: id={}, error={}", keycloakUserId, e.getMessage());
            throw new BadRequestException("Keycloak user update failed: " + e.getMessage());
        }
    }

    @Override
    public void deleteUser(String keycloakUserId) {
        if (!syncEnabled || !StringUtils.hasText(keycloakUserId)) {
            return;
        }

        String adminToken = getAdminToken();
        String deleteUrl = getBaseUrl() + "/admin/realms/" + realm + "/users/" + keycloakUserId;

        try {
            restClient.delete()
                    .uri(deleteUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[KEYCLOAK_USER_DELETED] Deleted user from Keycloak: id={}", keycloakUserId);
        } catch (Exception e) {
            log.error("Failed to delete user in Keycloak: id={}, error={}", keycloakUserId, e.getMessage());
        }
    }

    @Override
    public void enableUser(String keycloakUserId) {
        updateUser(keycloakUserId, null, true);
    }

    @Override
    public void disableUser(String keycloakUserId) {
        updateUser(keycloakUserId, null, false);
    }

    @Override
    public void assignRole(String keycloakUserId, Role role) {
        if (!syncEnabled || !StringUtils.hasText(keycloakUserId) || role == null) {
            return;
        }
        String adminToken = getAdminToken();
        assignRoleInternal(keycloakUserId, role.name(), adminToken);
    }

    @Override
    public void updateRole(String keycloakUserId, Role oldRole, Role newRole) {
        if (!syncEnabled || !StringUtils.hasText(keycloakUserId) || newRole == null) {
            return;
        }
        String adminToken = getAdminToken();

        // 1. Remove old role if present
        if (oldRole != null && oldRole != newRole) {
            removeRoleInternal(keycloakUserId, oldRole.name(), adminToken);
        }

        // 2. Assign new role
        assignRoleInternal(keycloakUserId, newRole.name(), adminToken);
        log.info("[KEYCLOAK_ROLE_CHANGED] Changed role for user {} from {} to {}", keycloakUserId, oldRole, newRole);
    }

    private void removeRoleInternal(String keycloakUserId, String roleName, String adminToken) {
        try {
            String roleUrl = getBaseUrl() + "/admin/realms/" + realm + "/roles/" + roleName;
            ResponseEntity<Map> roleResp = restClient.get()
                    .uri(roleUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .retrieve()
                    .toEntity(Map.class);

            if (roleResp.getStatusCode().is2xxSuccessful() && roleResp.getBody() != null) {
                Map<String, Object> roleObj = roleResp.getBody();
                String mappingUrl = getBaseUrl() + "/admin/realms/" + realm + "/users/" + keycloakUserId + "/role-mappings/realm";

                restClient.method(HttpMethod.DELETE)
                        .uri(mappingUrl)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(List.of(roleObj))
                        .retrieve()
                        .toBodilessEntity();

                log.info("[KEYCLOAK_ROLE_REMOVED] Role {} removed from user {}", roleName, keycloakUserId);
            }
        } catch (Exception e) {
            log.warn("Failed to remove realm role {} from user {}: {}", roleName, keycloakUserId, e.getMessage());
        }
    }

    private void assignRoleInternal(String keycloakUserId, String roleName, String adminToken) {
        try {
            String roleUrl = getBaseUrl() + "/admin/realms/" + realm + "/roles/" + roleName;
            ResponseEntity<Map> roleResp = restClient.get()
                    .uri(roleUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .retrieve()
                    .toEntity(Map.class);

            if (roleResp.getStatusCode().is2xxSuccessful() && roleResp.getBody() != null) {
                Map<String, Object> roleObj = roleResp.getBody();
                String mappingUrl = getBaseUrl() + "/admin/realms/" + realm + "/users/" + keycloakUserId + "/role-mappings/realm";

                restClient.post()
                        .uri(mappingUrl)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(List.of(roleObj))
                        .retrieve()
                        .toBodilessEntity();

                log.info("[KEYCLOAK_ROLE_ASSIGNED] Role {} assigned to user {}", roleName, keycloakUserId);
            }
        } catch (Exception e) {
            log.warn("Failed to assign realm role {} to user {}: {}", roleName, keycloakUserId, e.getMessage());
        }
    }

    @Override
    public void resetPassword(String keycloakUserId, String newPassword) {
        if (!syncEnabled || !StringUtils.hasText(keycloakUserId) || !StringUtils.hasText(newPassword)) {
            return;
        }

        String adminToken = getAdminToken();
        String resetUrl = getBaseUrl() + "/admin/realms/" + realm + "/users/" + keycloakUserId + "/reset-password";

        Map<String, Object> payload = Map.of(
                "type", "password",
                "value", newPassword,
                "temporary", false
        );

        try {
            restClient.put()
                    .uri(resetUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[KEYCLOAK_PASSWORD_RESET] Password reset in Keycloak for user id={}", keycloakUserId);
        } catch (Exception e) {
            log.error("Failed to reset password in Keycloak: id={}, error={}", keycloakUserId, e.getMessage());
            throw new BadRequestException("Failed to reset password: " + e.getMessage());
        }
    }
}
