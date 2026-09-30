package com.example.svgmanager.integration;

import com.example.svgmanager.dto.request.CreateUserRequest;
import com.example.svgmanager.dto.request.UpdateUserRequest;
import com.example.svgmanager.dto.request.UpdateUserRoleRequest;
import com.example.svgmanager.dto.request.UpdateUserStatusRequest;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.entity.Dealer;
import com.example.svgmanager.repository.DealerRepository;
import com.example.svgmanager.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AgentDataScopeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SvgFileRepository svgFileRepository;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    private User admin;
    private User agentA;
    private User agentB;
    private User userA;
    private User userB;

    private SvgFile svgA;
    private SvgFile svgB;

    private SvgFile svgAdmin;

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
                .claim("preferred_username", "user_a")
                .claim("email", "user_a@example.com")
                .claim("realm_access", Map.of("roles", List.of("USER")))
        ).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtUserB() {
        return jwt().jwt(builder -> builder
                .subject("keycloak-userB-sub")
                .claim("preferred_username", "user_b")
                .claim("email", "user_b@example.com")
                .claim("realm_access", Map.of("roles", List.of("USER")))
        ).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @BeforeEach
    void setUp() {
        // Clean all existing data to prevent isolation failures
        svgFileRepository.deleteAll();
        userRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // 2. Setup users
        admin = userRepository.save(User.builder()
                .keycloakUserId("keycloak-admin-sub")
                .username("admin")
                .email("admin@example.com")
                .fullName("Super Admin")
                .phone("+84900000001")
                .role(Role.ADMIN)
                .enabled(true)
                .deleted(false)
                .build());

        agentA = userRepository.save(User.builder()
                .keycloakUserId("keycloak-agentA-sub")
                .username("agent_a")
                .email("agent_a@example.com")
                .fullName("Agent Alpha")
                .phone("+84900000002")
                .role(Role.AGENT)
                .enabled(true)
                .deleted(false)
                .build());

        agentB = userRepository.save(User.builder()
                .keycloakUserId("keycloak-agentB-sub")
                .username("agent_b")
                .email("agent_b@example.com")
                .fullName("Agent Beta")
                .phone("+84900000003")
                .role(Role.AGENT)
                .enabled(true)
                .deleted(false)
                .build());

        userA = userRepository.save(User.builder()
                .keycloakUserId("keycloak-userA-sub")
                .username("user_a")
                .email("user_a@example.com")
                .fullName("User Alpha")
                .phone("+84900000004")
                .role(Role.USER)
                .agent(agentA)
                .enabled(true)
                .deleted(false)
                .build());

        userB = userRepository.save(User.builder()
                .keycloakUserId("keycloak-userB-sub")
                .username("user_b")
                .email("user_b@example.com")
                .fullName("User Beta")
                .phone("+84900000005")
                .role(Role.USER)
                .agent(agentB)
                .enabled(true)
                .deleted(false)
                .build());

        // 3. Setup SVG Files
        svgA = svgFileRepository.save(SvgFile.builder()
                .originalFilename("agentA_drawing.svg")
                .storedFilename("agentA_drawing_stored.svg")
                .filePath("/tmp/agentA_drawing.svg")
                .fileSize(1024L)
                .contentType("image/svg+xml")
                .uploadedBy(agentA)
                .agent(agentA)
                .build());

        svgB = svgFileRepository.save(SvgFile.builder()
                .originalFilename("userB_drawing.svg")
                .storedFilename("userB_drawing_stored.svg")
                .filePath("/tmp/userB_drawing.svg")
                .fileSize(2048L)
                .contentType("image/svg+xml")
                .uploadedBy(userB)
                .agent(agentB)
                .build());

        svgAdmin = svgFileRepository.save(SvgFile.builder()
                .originalFilename("admin_drawing.svg")
                .storedFilename("admin_drawing_stored.svg")
                .filePath("/tmp/admin_drawing.svg")
                .fileSize(4096L)
                .contentType("image/svg+xml")
                .uploadedBy(admin)
                .agent(null)
                .build());

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Case 1: Agent A only sees SVGs uploaded by Agent A and User A (Scoped to Agent A)")
    void agentA_OnlySeesOwnAndManagedUsersSvgs() throws Exception {
        mockMvc.perform(get("/api/svg").with(jwtAgentA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(svgA.getId()))
                .andExpect(jsonPath("$.content[0].originalFilename").value("agentA_drawing.svg"));
    }

    @Test
    @DisplayName("Case 2: Agent B only sees SVGs uploaded by User B (Scoped to Agent B)")
    void agentB_OnlySeesOwnAndManagedUsersSvgs() throws Exception {
        mockMvc.perform(get("/api/svg").with(jwtAgentB()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(svgB.getId()))
                .andExpect(jsonPath("$.content[0].originalFilename").value("userB_drawing.svg"));
    }

    @Test
    @DisplayName("Case 3: Admin sees all SVGs from all agents and users")
    void admin_SeesAllSvgs() throws Exception {
        mockMvc.perform(get("/api/svg").with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("Case 4: Agent A cannot access details of SVG belonging to Agent B scope -> 404")
    void agentA_CannotAccessSvgOfAgentB() throws Exception {
        mockMvc.perform(get("/api/svg/" + svgB.getId()).with(jwtAgentA()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Case 5: Agent A cannot delete SVG belonging to Agent B scope -> 404")
    void agentA_CannotDeleteSvgOfAgentB() throws Exception {
        mockMvc.perform(delete("/api/svg/" + svgB.getId()).with(jwtAgentA()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Case 6: Agent A uploading SVG automatically scopes it to Agent A")
    void agentA_UploadSvg_AutoScopedToAgentA() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "car_pattern.svg", "image/svg+xml",
                "<svg><rect width=\"100\" height=\"100\"/></svg>".getBytes()
        );

        mockMvc.perform(multipart("/api/svg")
                        .file(file)
                        .with(jwtAgentA()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.agentId").value(agentA.getId()))
                .andExpect(jsonPath("$.uploadedBy.username").value("agent_a"));
    }

    @Test
    @DisplayName("Case 7: Agent A listing users only sees User A (users under own agent scope)")
    void agentA_ListingUsers_OnlySeesManagedUsers() throws Exception {
        mockMvc.perform(get("/api/users").with(jwtAgentA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].username").value("user_a"));
    }

    @Test
    @DisplayName("Case 8: Admin listing users sees all users across all agents")
    void admin_ListingUsers_SeesAllUsers() throws Exception {
        mockMvc.perform(get("/api/users").with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    @DisplayName("Case 9: Agent A cannot access user details of User B -> 404")
    void agentA_CannotAccessUserDetailsOfUserB() throws Exception {
        mockMvc.perform(get("/api/users/" + userB.getId()).with(jwtAgentA()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Case 10: Agent A creating a user automatically associates user with Agent A")
    void agentA_CreateUser_AutoAssignsAgentA() throws Exception {
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username("new_user_under_a")
                .email("new_user_under_a@example.com")
                .password("Password123!")
                .role(Role.USER)
                .build();

        mockMvc.perform(post("/api/users")
                        .with(jwtAgentA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new_user_under_a"))
                .andExpect(jsonPath("$.agentId").value(agentA.getId()))
                .andExpect(jsonPath("$.agentUsername").value("agent_a"));
    }

    @Test
    @DisplayName("Case 11: Agent A cannot update user of Agent B -> 404")
    void agentA_CannotUpdateUserOfAgentB() throws Exception {
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setEmail("hacked@example.com");

        mockMvc.perform(put("/api/users/" + userB.getId())
                        .with(jwtAgentA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Case 12: Agent A cannot promote a user to AGENT or ADMIN -> 403 Forbidden")
    void agentA_CannotPromoteUserToAgentOrAdmin() throws Exception {
        CreateUserRequest createAgentRequest = CreateUserRequest.builder()
                .username("rogue_agent")
                .email("rogue_agent@example.com")
                .password("Password123!")
                .role(Role.AGENT)
                .build();

        mockMvc.perform(post("/api/users")
                        .with(jwtAgentA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAgentRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Case 13: Normal USER role cannot list users -> 403 Forbidden")
    void normalUser_CannotListUsers() throws Exception {
        mockMvc.perform(get("/api/users").with(jwtUserA()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Case 14: Normal USER role cannot delete SVG files -> 403 Forbidden")
    void normalUser_CannotDeleteSvg() throws Exception {
        mockMvc.perform(delete("/api/svg/" + svgA.getId()).with(jwtUserA()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Case 15: Agent A cannot delete User B -> 404")
    void agentA_CannotDeleteUserOfAgentB() throws Exception {
        mockMvc.perform(delete("/api/users/" + userB.getId()).with(jwtAgentA()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Case 16: SVG preview modal endpoint obeys Agent scoping -> Agent A cannot preview Agent B's SVG")
    void agentA_CannotPreviewSvgOfAgentB() throws Exception {
        mockMvc.perform(get("/api/svg/" + svgB.getId() + "/content").with(jwtAgentA()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Case 18: Unauthenticated request to /api/svg is rejected with 401")
    void unauthenticatedAccess_Returns401() throws Exception {
        mockMvc.perform(get("/api/svg"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // USER MANAGEMENT & IDENTITY INTEGRATION TESTS
    // ==========================================

    @Test
    @DisplayName("Case 29: ADMIN creates user with business profile (fullName, phone) and assigns Agent -> 201 Created")
    void admin_CreateUser_WithBusinessProfile_AndAgent() throws Exception {
        CreateUserRequest req = CreateUserRequest.builder()
                .username("new_biz_user")
                .email("new_biz_user@example.com")
                .fullName("Nguyen Van A")
                .phone("+84912345678")
                .password("Password123!")
                .role(Role.USER)
                .agentId(agentA.getId())
                .enabled(true)
                .build();

        mockMvc.perform(post("/api/users")
                .with(jwtAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new_biz_user"))
                .andExpect(jsonPath("$.fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.phone").value("+84912345678"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.agentId").value(agentA.getId()))
                .andExpect(jsonPath("$.keycloakUserId").isNotEmpty());
    }

    @Test
    @DisplayName("Case 30: ADMIN updates user role and status -> role & status synchronized")
    void admin_UpdateUser_RoleAndStatus_Synchronized() throws Exception {
        // 1. Change user role to AGENT
        UpdateUserRoleRequest roleReq = new UpdateUserRoleRequest(Role.AGENT);
        mockMvc.perform(patch("/api/users/" + userA.getId() + "/role")
                .with(jwtAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roleReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("AGENT"));

        // 2. Change status to disabled
        UpdateUserStatusRequest statusReq = new UpdateUserStatusRequest(false);
        mockMvc.perform(patch("/api/users/" + userA.getId() + "/status")
                .with(jwtAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @DisplayName("Case 31: Deleting user who owns SVG files -> soft-deleted to preserve SVG history and foreign keys")
    void deleteUser_WithSvgFiles_SoftDeletesUser() throws Exception {
        // userB has svgB
        mockMvc.perform(delete("/api/users/" + userB.getId())
                .with(jwtAdmin()))
                .andExpect(status().isNoContent());

        // Ensure user is marked deleted in database
        User deletedUser = userRepository.findById(userB.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(deletedUser.isDeleted());
        org.junit.jupiter.api.Assertions.assertFalse(deletedUser.isEnabled());

        // Ensure user does not appear in active user listings
        mockMvc.perform(get("/api/users").with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.username == 'user_b')]").isEmpty());

        // SvgB still exists with valid uploadedBy foreign key reference
        SvgFile preservedSvg = svgFileRepository.findById(svgB.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(userB.getId(), preservedSvg.getUploadedBy().getId());
    }

    @Test
    @DisplayName("Case 32: Deleting unreferenced leaf user -> hard-deleted from database")
    void deleteUser_WithoutSvgFiles_HardDeletesUser() throws Exception {
        // Create a standalone user without files
        User standalone = userRepository.save(User.builder()
                .keycloakUserId("standalone-sub-123")
                .username("standalone")
                .email("standalone@example.com")
                .role(Role.USER)
                .enabled(true)
                .deleted(false)
                .build());

        mockMvc.perform(delete("/api/users/" + standalone.getId())
                .with(jwtAdmin()))
                .andExpect(status().isNoContent());

        // Ensure record is deleted completely
        org.junit.jupiter.api.Assertions.assertTrue(userRepository.findById(standalone.getId()).isEmpty());
    }

    @Test
    @DisplayName("Case 33: Unregistered Keycloak user (not in application DB) -> 401 Unauthorized")
    void unregisteredKeycloakUser_CannotAccessApp_Returns401() throws Exception {
        // User with sub that does NOT exist in DB and username that does not exist in DB
        SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtUnregistered = jwt().jwt(builder -> builder
                .subject("keycloak-rogue-console-sub")
                .claim("preferred_username", "rogue_console_user")
                .claim("email", "rogue@example.com")
                .claim("realm_access", Map.of("roles", List.of("USER")))
        ).authorities(new SimpleGrantedAuthority("ROLE_USER"));

        mockMvc.perform(get("/api/svg").with(jwtUnregistered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not registered in the application")));
    }

    @Test
    @DisplayName("Case 34: Creating user without username automatically uses email as username -> 201 Created")
    void createUser_WithoutUsername_DefaultsToEmail() throws Exception {
        CreateUserRequest req = CreateUserRequest.builder()
                .email("auto_email_user@example.com")
                .fullName("Auto Email User")
                .password("Password123!")
                .role(Role.USER)
                .agentId(agentA.getId())
                .enabled(true)
                .build();

        mockMvc.perform(post("/api/users")
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("auto_email_user@example.com"))
                .andExpect(jsonPath("$.email").value("auto_email_user@example.com"));
    }

    @Test
    @DisplayName("Case 35: Creating user without email fails validation -> 400 Bad Request")
    void createUser_WithoutEmail_FailsValidation() throws Exception {
        CreateUserRequest req = CreateUserRequest.builder()
                .username("no_email_user")
                .password("Password123!")
                .role(Role.USER)
                .build();

        mockMvc.perform(post("/api/users")
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Case 36: Agent creating user defaults to agent's dealer -> 201 Created with dealer populated")
    void agentCreateUser_DefaultsToAgentDealer() throws Exception {
        Dealer dealerA = dealerRepository.save(Dealer.builder()
                .code("DEALER_AGENT_A")
                .name("Alpha Auto Film")
                .status("ACTIVE")
                .build());
        agentA.setDealer(dealerA);
        userRepository.save(agentA);

        CreateUserRequest req = CreateUserRequest.builder()
                .email("tech_cutter@alpha.com")
                .fullName("Tho Cat Alpha")
                .password("Password123!")
                .role(Role.USER)
                .build();

        mockMvc.perform(post("/api/users")
                        .with(jwtAgentA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("tech_cutter@alpha.com"))
                .andExpect(jsonPath("$.email").value("tech_cutter@alpha.com"))
                .andExpect(jsonPath("$.dealerId").value(dealerA.getId()))
                .andExpect(jsonPath("$.dealerName").value("Alpha Auto Film"));
    }

    @Test
    @DisplayName("Case 38: Agent cannot create user for another dealer -> 403 Forbidden")
    void agent_CannotCreateUserForAnotherDealer() throws Exception {
        Dealer dealerA = dealerRepository.save(Dealer.builder()
                .code("DEALER_A3")
                .name("Alpha Auto Film 3")
                .status("ACTIVE")
                .build());
        Dealer dealerB = dealerRepository.save(Dealer.builder()
                .code("DEALER_B3")
                .name("Beta Auto Film 3")
                .status("ACTIVE")
                .build());
        agentA.setDealer(dealerA);
        userRepository.save(agentA);

        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("intruder@beta.com");
        req.setPassword("Secret@123456");
        req.setFullName("Intruder");
        req.setRole(Role.USER);
        req.setDealerId(dealerB.getId()); // Attempting to assign user to dealerB

        mockMvc.perform(post("/api/users")
                        .with(jwtAgentA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
