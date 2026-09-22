package com.example.svgmanager.integration;

import com.example.svgmanager.dto.request.CreateUserRequest;
import com.example.svgmanager.dto.request.UpdateUserRequest;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AgentDataScopeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SvgFileRepository svgFileRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User admin;
    private User agentA;
    private User agentB;
    private User userA1;
    private User userB1;
    private SvgFile svgA1;
    private SvgFile svgB1;

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtAdmin() {
        return jwt().jwt(builder -> builder
                .subject("keycloak-admin-sub")
                .claim("preferred_username", "admin")
                .claim("email", "admin@example.com")
                .claim("realm_access", Map.of("roles", List.of("ADMIN")))
        ).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtAgentA() {
        return jwt().jwt(builder -> builder
                .subject("keycloak-agentA-sub")
                .claim("preferred_username", "agent_a")
                .claim("email", "agent_a@example.com")
                .claim("realm_access", Map.of("roles", List.of("AGENT")))
        ).authorities(new SimpleGrantedAuthority("ROLE_AGENT"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtAgentB() {
        return jwt().jwt(builder -> builder
                .subject("keycloak-agentB-sub")
                .claim("preferred_username", "agent_b")
                .claim("email", "agent_b@example.com")
                .claim("realm_access", Map.of("roles", List.of("AGENT")))
        ).authorities(new SimpleGrantedAuthority("ROLE_AGENT"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtUserA() {
        return jwt().jwt(builder -> builder
                .subject("keycloak-userA-sub")
                .claim("preferred_username", "user_a1")
                .claim("email", "user_a1@example.com")
                .claim("realm_access", Map.of("roles", List.of("USER")))
        ).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @BeforeEach
    void setUp() {
        svgFileRepository.deleteAll();
        userRepository.deleteAll();

        admin = userRepository.save(User.builder()
                .keycloakUserId("keycloak-admin-sub")
                .username("admin")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        agentA = userRepository.save(User.builder()
                .keycloakUserId("keycloak-agentA-sub")
                .username("agent_a")
                .email("agent_a@example.com")
                .role(Role.AGENT)
                .enabled(true)
                .build());

        agentB = userRepository.save(User.builder()
                .keycloakUserId("keycloak-agentB-sub")
                .username("agent_b")
                .email("agent_b@example.com")
                .role(Role.AGENT)
                .enabled(true)
                .build());

        userA1 = userRepository.save(User.builder()
                .keycloakUserId("keycloak-userA-sub")
                .username("user_a1")
                .email("user_a1@example.com")
                .role(Role.USER)
                .agent(agentA)
                .enabled(true)
                .build());

        userB1 = userRepository.save(User.builder()
                .keycloakUserId("keycloak-userB-sub")
                .username("user_b1")
                .email("user_b1@example.com")
                .role(Role.USER)
                .agent(agentB)
                .enabled(true)
                .build());

        svgA1 = svgFileRepository.save(SvgFile.builder()
                .originalFilename("agent_a_graphic.svg")
                .storedFilename("stored-a1.svg")
                .filePath("target/test-svg-storage/stored-a1.svg")
                .fileSize(100L)
                .contentType("image/svg+xml")
                .checksum("checksumA1")
                .uploadedBy(agentA)
                .agent(agentA)
                .build());

        svgB1 = svgFileRepository.save(SvgFile.builder()
                .originalFilename("agent_b_graphic.svg")
                .storedFilename("stored-b1.svg")
                .filePath("target/test-svg-storage/stored-b1.svg")
                .fileSize(100L)
                .contentType("image/svg+xml")
                .checksum("checksumB1")
                .uploadedBy(agentB)
                .agent(agentB)
                .build());
    }

    // ==================== USER DATA SCOPE & IDOR TESTS ====================

    @Test
    @DisplayName("Case 1: ADMIN sees all users")
    void admin_CanSeeAllUsers() throws Exception {
        mockMvc.perform(get("/api/users").with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements", is(5)));
    }

    @Test
    @DisplayName("Case 3: AGENT A sees only own users")
    void agentA_CanSeeOnlyOwnUsers() throws Exception {
        mockMvc.perform(get("/api/users").with(jwtAgentA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].username", is("user_a1")));
    }

    @Test
    @DisplayName("Case 4: AGENT A cannot see AGENT B user by ID (IDOR Protection -> 404)")
    void agentA_CannotGetUserOfAgentB() throws Exception {
        mockMvc.perform(get("/api/users/" + userB1.getId()).with(jwtAgentA()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Case 5: AGENT A cannot update AGENT B user (IDOR Protection -> 404)")
    void agentA_CannotUpdateUserOfAgentB() throws Exception {
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setEmail("hacked_b@example.com");
        updateRequest.setEnabled(true);

        mockMvc.perform(put("/api/users/" + userB1.getId())
                        .with(jwtAgentA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Case 6: AGENT A cannot delete AGENT B user (IDOR Protection -> 404)")
    void agentA_CannotDeleteUserOfAgentB() throws Exception {
        mockMvc.perform(delete("/api/users/" + userB1.getId()).with(jwtAgentA()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Case 9: AGENT A creates user -> Backend automatically assigns agent_id = Agent A")
    void agentA_CreateUser_AutomaticallyAssignedToAgentA() throws Exception {
        CreateUserRequest createRequest = new CreateUserRequest();
        createRequest.setUsername("new_user_under_a");
        createRequest.setEmail("new_user_a@example.com");
        createRequest.setPassword("Password123!");
        createRequest.setRole(Role.USER);

        mockMvc.perform(post("/api/users")
                        .with(jwtAgentA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("new_user_under_a")))
                .andExpect(jsonPath("$.agentId", is(agentA.getId().intValue())));
    }

    @Test
    @DisplayName("Case 11 & 12: USER cannot access /api/users (403 Forbidden)")
    void user_CannotAccessUserManagement() throws Exception {
        mockMvc.perform(get("/api/users").with(jwtUserA()))
                .andExpect(status().isForbidden());
    }

    // ==================== SVG DATA SCOPE & IDOR TESTS ====================

    @Test
    @DisplayName("Case 2: ADMIN sees all SVG files")
    void admin_CanSeeAllSvgFiles() throws Exception {
        mockMvc.perform(get("/api/svg").with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("Case 7: AGENT A sees only own SVG files")
    void agentA_CanSeeOnlyOwnSvgFiles() throws Exception {
        mockMvc.perform(get("/api/svg").with(jwtAgentA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].originalFilename", is("agent_a_graphic.svg")));
    }

    @Test
    @DisplayName("Case 8: AGENT A cannot delete AGENT B SVG file (IDOR Protection -> 404)")
    void agentA_CannotDeleteSvgOfAgentB() throws Exception {
        mockMvc.perform(delete("/api/svg/" + svgB1.getId()).with(jwtAgentA()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Case 10: AGENT A upload SVG -> Backend automatically assigns agent_id = Agent A")
    void agentA_UploadSvg_AutomaticallyAssignedToAgentA() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "circle.svg",
                "image/svg+xml",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><circle cx=\"50\" cy=\"50\" r=\"40\"/></svg>".getBytes()
        );

        mockMvc.perform(multipart("/api/svg/upload")
                        .file(file)
                        .with(jwtAgentA()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.agentId", is(agentA.getId().intValue())))
                .andExpect(jsonPath("$.uploadedBy.username", is("agent_a")));
    }

    @Test
    @DisplayName("Case 13: Unauthenticated request -> 401 Unauthorized")
    void unauthenticatedRequest_Returns401() throws Exception {
        mockMvc.perform(get("/api/svg"))
                .andExpect(status().isUnauthorized());
    }
}
