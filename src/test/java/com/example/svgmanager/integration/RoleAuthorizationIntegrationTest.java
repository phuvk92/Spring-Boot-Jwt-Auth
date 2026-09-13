package com.example.svgmanager.integration;

import com.example.svgmanager.dto.request.CreateUserRequest;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.repository.RefreshTokenRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.security.JwtTokenProvider;
import com.example.svgmanager.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RoleAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SvgFileRepository svgFileRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String agentToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        svgFileRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        User admin = User.builder()
                .username("rbac_admin")
                .email("rbac_admin@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        admin = userRepository.save(admin);
        adminToken = jwtTokenProvider.generateTokenFromUser(UserPrincipal.create(admin));

        User agent = User.builder()
                .username("rbac_agent")
                .email("rbac_agent@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.AGENT)
                .enabled(true)
                .build();
        agent = userRepository.save(agent);
        agentToken = jwtTokenProvider.generateTokenFromUser(UserPrincipal.create(agent));

        User user = User.builder()
                .username("rbac_user")
                .email("rbac_user@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.USER)
                .enabled(true)
                .build();
        user = userRepository.save(user);
        userToken = jwtTokenProvider.generateTokenFromUser(UserPrincipal.create(user));
    }

    @Test
    @DisplayName("RBAC: Unauthenticated requests to protected endpoints return 401 Unauthorized")
    void unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/svg")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RBAC: ADMIN can access user management endpoints")
    void admin_CanAccessUserManagement() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        CreateUserRequest request = CreateUserRequest.builder()
                .username("new_agent_account")
                .email("new_agent@example.com")
                .password("Password123!")
                .role(Role.AGENT)
                .enabled(true)
                .build();

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("RBAC: AGENT and USER are forbidden (403) from accessing user management")
    void nonAdmin_CannotAccessUserManagement() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: AGENT can upload SVG, but USER cannot upload SVG (403)")
    void svgUpload_RbacRules() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.svg",
                "image/svg+xml",
                "<svg><circle r='5'/></svg>".getBytes(StandardCharsets.UTF_8)
        );

        // AGENT upload succeeds
        mockMvc.perform(multipart("/api/svg/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isCreated());

        // USER upload is forbidden
        mockMvc.perform(multipart("/api/svg/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: Only ADMIN can delete SVG; AGENT and USER receive 403 Forbidden")
    void svgDelete_RbacRules() throws Exception {
        // First, upload an SVG as ADMIN
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample_to_delete.svg",
                "image/svg+xml",
                "<svg><rect width='5' height='5'/></svg>".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/svg/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());

        Long svgId = svgFileRepository.findAll().get(0).getId();

        // USER cannot delete SVG
        mockMvc.perform(delete("/api/svg/{id}", svgId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // AGENT cannot delete SVG
        mockMvc.perform(delete("/api/svg/{id}", svgId)
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isForbidden());

        // ADMIN can delete SVG
        mockMvc.perform(delete("/api/svg/{id}", svgId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("RBAC: All roles (ADMIN, AGENT, USER) can view and preview SVGs")
    void svgView_AllowedForAllAuthenticatedRoles() throws Exception {
        mockMvc.perform(get("/api/svg")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/svg")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/svg")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }
}
