package com.example.svgmanager.integration;

import com.example.svgmanager.dto.request.CreateDealerRequest;
import com.example.svgmanager.dto.request.CreateUserRequest;
import com.example.svgmanager.dto.request.UpdateDealerRequest;
import com.example.svgmanager.dto.request.UpdateUserRequest;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.repository.DealerRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.service.KeycloakUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UsernameValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KeycloakUserService keycloakUserService;

    private User adminUser;

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtAdmin() {
        return jwt().jwt(builder -> builder
                .subject("kc-admin-uuid")
                .claim("preferred_username", "admin_user")
                .claim("email", "admin@domain.com")
                .claim("realm_access", Map.of("roles", List.of("ADMIN")))
        ).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        dealerRepository.deleteAll();

        adminUser = userRepository.save(User.builder()
                .username("admin_user")
                .email("admin@domain.com")
                .role(Role.ADMIN)
                .keycloakUserId("kc-admin-uuid")
                .enabled(true)
                .deleted(false)
                .build());

        when(keycloakUserService.createUser(any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn("kc-new-uuid");
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin123", "Admin123", "admin_123", "admin-123", "AGENT_001"})
    @DisplayName("Create User with valid username formats succeeds -> 201 Created")
    void createUser_ValidUsernames_Success(String validUsername) throws Exception {
        CreateUserRequest req = CreateUserRequest.builder()
                .username(validUsername)
                .email(validUsername.toLowerCase() + "@example.com")
                .password("StrongPass123!")
                .role(Role.USER)
                .build();

        mockMvc.perform(post("/api/users")
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(validUsername));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "user 123",      // contains space
            "user@123",      // special char @
            "user#123",      // special char #
            "user.name",      // dot is not allowed
            "người_dùng",    // Vietnamese accented chars
            "user!123",      // exclamation mark
            "ab",            // length < 3
            "   ",           // whitespace only
            "___",           // only symbols, no alphanumeric
            "---"            // only symbols, no alphanumeric
    })
    @DisplayName("Create User with invalid username formats fails -> 400 Bad Request")
    void createUser_InvalidUsernames_Fails(String invalidUsername) throws Exception {
        CreateUserRequest req = CreateUserRequest.builder()
                .username(invalidUsername)
                .email("test_invalid@example.com")
                .password("StrongPass123!")
                .role(Role.USER)
                .build();

        mockMvc.perform(post("/api/users")
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Create User with duplicate username returns 409 Conflict with code USERNAME_ALREADY_EXISTS")
    void createUser_DuplicateUsername_ReturnsConflict() throws Exception {
        userRepository.save(User.builder()
                .username("existing_user")
                .email("existing@example.com")
                .role(Role.USER)
                .keycloakUserId("kc-exist-1")
                .enabled(true)
                .deleted(false)
                .build());

        CreateUserRequest req = CreateUserRequest.builder()
                .username("existing_user")
                .email("another@example.com")
                .password("StrongPass123!")
                .role(Role.USER)
                .build();

        mockMvc.perform(post("/api/users")
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("Update User keeping same username succeeds -> 200 OK")
    void updateUser_KeepSameUsername_Success() throws Exception {
        User target = userRepository.save(User.builder()
                .username("user_keep")
                .email("keep@example.com")
                .role(Role.USER)
                .keycloakUserId("kc-keep-1")
                .enabled(true)
                .deleted(false)
                .build());

        UpdateUserRequest req = UpdateUserRequest.builder()
                .username("user_keep")
                .email("keep_new_email@example.com")
                .build();

        mockMvc.perform(put("/api/users/" + target.getId())
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user_keep"))
                .andExpect(jsonPath("$.email").value("keep_new_email@example.com"));
    }

    @Test
    @DisplayName("Update User with valid new username succeeds -> 200 OK")
    void updateUser_ValidNewUsername_Success() throws Exception {
        User target = userRepository.save(User.builder()
                .username("old_username")
                .email("update@example.com")
                .role(Role.USER)
                .keycloakUserId("kc-upd-1")
                .enabled(true)
                .deleted(false)
                .build());

        UpdateUserRequest req = UpdateUserRequest.builder()
                .username("new_cool_username")
                .email("update@example.com")
                .build();

        mockMvc.perform(put("/api/users/" + target.getId())
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new_cool_username"));
    }

    @Test
    @DisplayName("Update User with duplicate username of another user returns 409 Conflict")
    void updateUser_DuplicateUsernameOfAnother_ReturnsConflict() throws Exception {
        userRepository.save(User.builder()
                .username("user_taken")
                .email("taken@example.com")
                .role(Role.USER)
                .keycloakUserId("kc-taken-1")
                .enabled(true)
                .deleted(false)
                .build());

        User target = userRepository.save(User.builder()
                .username("my_username")
                .email("mine@example.com")
                .role(Role.USER)
                .keycloakUserId("kc-mine-1")
                .enabled(true)
                .deleted(false)
                .build());

        UpdateUserRequest req = UpdateUserRequest.builder()
                .username("user_taken")
                .email("mine@example.com")
                .build();

        mockMvc.perform(put("/api/users/" + target.getId())
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_EXISTS"));
    }
}
