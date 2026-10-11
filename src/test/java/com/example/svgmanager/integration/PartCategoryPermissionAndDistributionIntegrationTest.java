package com.example.svgmanager.integration;

import com.example.svgmanager.dto.request.CreateUserRequest;
import com.example.svgmanager.dto.request.UpdateUserRequest;
import com.example.svgmanager.entity.*;
import com.example.svgmanager.repository.*;
import com.example.svgmanager.service.FileStorageService;
import com.example.svgmanager.service.KeycloakUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PartCategoryPermissionAndDistributionIntegrationTest {

    private static final String VALID_SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\"><rect width=\"100\" height=\"100\" fill=\"red\"/></svg>";
    private static final long ADMIN_LIBRARY_ID_OFFSET = 1_000_000_000L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private PartLibraryCategoryRepository partLibraryCategoryRepository;

    @Autowired
    private UserPartCategoryPermissionRepository userPartCategoryPermissionRepository;

    @Autowired
    private SvgFileRepository svgFileRepository;

    @Autowired
    private UserSvgFileRepository userSvgFileRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @MockBean
    private KeycloakUserService keycloakUserService;

    private User adminUser;
    private User agentUserA;
    private User agentUserB;
    private User userA;
    private User userB;
    private User userC;

    private Dealer dealerA;
    private Dealer dealerB;

    private PartLibraryCategory catExterior;
    private PartLibraryCategory catInterior;

    private SvgFile svgExterior;
    private SvgFile svgInterior;

    private UserSvgFile savedFileUserA;
    private UserSvgFile savedFileUserB;

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asAdmin() {
        return jwt().jwt(j -> j.subject("kc-admin-perm").claim("preferred_username", "admin_perm"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asAgentA() {
        return jwt().jwt(j -> j.subject("kc-agent-a").claim("preferred_username", "agent_a"))
                .authorities(new SimpleGrantedAuthority("ROLE_AGENT"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asAgentB() {
        return jwt().jwt(j -> j.subject("kc-agent-b").claim("preferred_username", "agent_b"))
                .authorities(new SimpleGrantedAuthority("ROLE_AGENT"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUserA() {
        return jwt().jwt(j -> j.subject("kc-user-a").claim("preferred_username", "user_a"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUserB() {
        return jwt().jwt(j -> j.subject("kc-user-b").claim("preferred_username", "user_b"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUserC() {
        return jwt().jwt(j -> j.subject("kc-user-c").claim("preferred_username", "user_c"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @BeforeEach
    void setUp() {
        when(keycloakUserService.createUser(anyString(), anyString(), anyString(), any(Role.class), anyBoolean())).thenReturn("mock-kc-id");
        when(keycloakUserService.createUser(anyString(), anyString(), anyString(), any(Role.class), anyBoolean(), any())).thenReturn("mock-kc-id");

        dealerA = dealerRepository.save(Dealer.builder().code("DLR_A").name("Dealer A").build());
        dealerB = dealerRepository.save(Dealer.builder().code("DLR_B").name("Dealer B").build());

        adminUser = userRepository.save(User.builder()
                .username("admin_perm")
                .email("admin_perm@test.local")
                .keycloakUserId("kc-admin-perm")
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        agentUserA = userRepository.save(User.builder()
                .username("agent_a")
                .email("agent_a@test.local")
                .keycloakUserId("kc-agent-a")
                .dealer(dealerA)
                .role(Role.AGENT)
                .enabled(true)
                .build());

        agentUserB = userRepository.save(User.builder()
                .username("agent_b")
                .email("agent_b@test.local")
                .keycloakUserId("kc-agent-b")
                .dealer(dealerB)
                .role(Role.AGENT)
                .enabled(true)
                .build());

        userA = userRepository.save(User.builder()
                .username("user_a")
                .email("user_a@test.local")
                .keycloakUserId("kc-user-a")
                .dealer(dealerA)
                .role(Role.USER)
                .enabled(true)
                .build());

        userB = userRepository.save(User.builder()
                .username("user_b")
                .email("user_b@test.local")
                .keycloakUserId("kc-user-b")
                .dealer(dealerB)
                .role(Role.USER)
                .enabled(true)
                .build());

        userC = userRepository.save(User.builder()
                .username("user_c")
                .email("user_c@test.local")
                .keycloakUserId("kc-user-c")
                .dealer(dealerA)
                .role(Role.USER)
                .enabled(true)
                .build());

        // Categories
        catExterior = partLibraryCategoryRepository.save(new PartLibraryCategory("EXTERIOR", "Ngoại thất", "ACTIVE"));
        catInterior = partLibraryCategoryRepository.save(new PartLibraryCategory("INTERIOR", "Nội thất", "ACTIVE"));

        // Assign permission: userA -> catExterior, userB -> catInterior, userC -> none
        userPartCategoryPermissionRepository.save(new UserPartCategoryPermission(userA, catExterior, "admin_perm"));
        userPartCategoryPermissionRepository.save(new UserPartCategoryPermission(userB, catInterior, "admin_perm"));

        // Physical files in storage
        String pathExt = fileStorageService.storeFile(VALID_SVG.getBytes(StandardCharsets.UTF_8), "exterior.svg");
        String pathInt = fileStorageService.storeFile(VALID_SVG.getBytes(StandardCharsets.UTF_8), "interior.svg");
        String pathUserA = fileStorageService.storeFile(VALID_SVG.getBytes(StandardCharsets.UTF_8), "saved_user_a.svg");
        String pathUserB = fileStorageService.storeFile(VALID_SVG.getBytes(StandardCharsets.UTF_8), "saved_user_b.svg");

        // Admin library SvgFiles
        SvgFile extFile = SvgFile.builder()
                .originalFilename("exterior.svg")
                .storedFilename("exterior.svg")
                .filePath(pathExt)
                .fileSize((long) VALID_SVG.length())
                .contentType("image/svg+xml")
                .status("ACTIVE")
                .uploadedBy(adminUser)
                .build();
        extFile.setFileKey("ext-svg-key");
        extFile.setDisplayName("Capo Ngoại Thất");
        extFile.setPartLibraryCategory(catExterior);
        svgExterior = svgFileRepository.save(extFile);

        SvgFile intFile = SvgFile.builder()
                .originalFilename("interior.svg")
                .storedFilename("interior.svg")
                .filePath(pathInt)
                .fileSize((long) VALID_SVG.length())
                .contentType("image/svg+xml")
                .status("ACTIVE")
                .uploadedBy(adminUser)
                .build();
        intFile.setFileKey("int-svg-key");
        intFile.setDisplayName("Màn Hình Nội Thất");
        intFile.setPartLibraryCategory(catInterior);
        svgInterior = svgFileRepository.save(intFile);

        // User saved personal files
        savedFileUserA = userSvgFileRepository.save(UserSvgFile.builder()
                .fileName("UserA_Saved.svg")
                .originalFileName("UserA_Saved.svg")
                .storedFileName("saved_user_a.svg")
                .filePath(pathUserA)
                .fileSize((long) VALID_SVG.length())
                .mimeType("image/svg+xml")
                .checksum("sha256:hash_a")
                .status("ACTIVE")
                .user(userA)
                .dealer(dealerA)
                .createdAt(LocalDateTime.now())
                .build());

        savedFileUserB = userSvgFileRepository.save(UserSvgFile.builder()
                .fileName("UserB_Saved.svg")
                .originalFileName("UserB_Saved.svg")
                .storedFileName("saved_user_b.svg")
                .filePath(pathUserB)
                .fileSize((long) VALID_SVG.length())
                .mimeType("image/svg+xml")
                .checksum("sha256:hash_b")
                .status("ACTIVE")
                .user(userB)
                .dealer(dealerB)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // =========================================================================
    // 1. Phân quyền danh mục kho mẫu & part (Create / Update / Get user)
    // =========================================================================

    @Test
    @DisplayName("Admin creates user with category permissions successfully")
    void admin_createUserWithPartCategoryPermissions() throws Exception {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("new_perm_user");
        req.setEmail("new_perm_user@test.local");
        req.setPassword("Password@123");
        req.setRole(Role.USER);
        req.setDealerId(dealerA.getId());
        req.setEnabled(true);
        req.setPartCategoryIds(List.of(catExterior.getId(), catInterior.getId()));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(asAdmin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("new_perm_user")))
                .andExpect(jsonPath("$.partCategoryIds", containsInAnyOrder(catExterior.getId().intValue(), catInterior.getId().intValue())));

        User created = userRepository.findByUsername("new_perm_user").orElseThrow();
        List<UserPartCategoryPermission> perms = userPartCategoryPermissionRepository.findByUserId(created.getId());
        assertThat(perms).hasSize(2);
    }

    @Test
    @DisplayName("Admin updates user category permissions (add/remove) and revokes")
    void admin_updateUserPartCategoryPermissions() throws Exception {
        // userA initially has catExterior. Now change to only catInterior.
        UpdateUserRequest updateReq = new UpdateUserRequest();
        updateReq.setEmail(userA.getEmail());
        updateReq.setRole(Role.USER);
        updateReq.setDealerId(dealerA.getId());
        updateReq.setEnabled(true);
        updateReq.setPartCategoryIds(List.of(catInterior.getId()));

        mockMvc.perform(put("/api/users/" + userA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq))
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partCategoryIds", contains(catInterior.getId().intValue())));

        List<UserPartCategoryPermission> permsAfter = userPartCategoryPermissionRepository.findByUserId(userA.getId());
        assertThat(permsAfter).hasSize(1);
        assertThat(permsAfter.get(0).getPartLibraryCategory().getId()).isEqualTo(catInterior.getId());

        // Revoke all permissions by passing empty list
        updateReq.setPartCategoryIds(List.of());
        mockMvc.perform(put("/api/users/" + userA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq))
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partCategoryIds", hasSize(0)));

        assertThat(userPartCategoryPermissionRepository.findByUserId(userA.getId())).isEmpty();
    }

    // =========================================================================
    // 2. Tự động phân phối file SVG theo danh mục (V1 /api/internal/user-files)
    // =========================================================================

    @Test
    @DisplayName("USER with category permission sees ADMIN_LIBRARY files merged into saved files list")
    void user_seesAdminLibraryFilesMatchingPermissions_V1() throws Exception {
        // userA has permission for catExterior.
        // Expect: 1 personal saved file (savedFileUserA) + 1 ADMIN_LIBRARY file (svgExterior).
        // Total: 2. svgInterior must NOT appear.
        mockMvc.perform(get("/api/internal/user-files").with(asUserA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[*].fileName", containsInAnyOrder("UserA_Saved.svg", "Capo Ngoại Thất")))
                .andExpect(jsonPath("$.content[?(@.fileName == 'Capo Ngoại Thất')].fileSource", contains("ADMIN_LIBRARY")))
                .andExpect(jsonPath("$.content[?(@.fileName == 'UserA_Saved.svg')].fileSource", contains("USER_SAVED")));

        // userB has permission for catInterior.
        // Expect: 1 personal saved file (savedFileUserB) + 1 ADMIN_LIBRARY file (svgInterior).
        mockMvc.perform(get("/api/internal/user-files").with(asUserB()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[*].fileName", containsInAnyOrder("UserB_Saved.svg", "Màn Hình Nội Thất")))
                .andExpect(jsonPath("$.content[?(@.fileName == 'Màn Hình Nội Thất')].fileSource", contains("ADMIN_LIBRARY")));

        // userC has NO permissions.
        // Expect: 0 personal saved files + 0 ADMIN_LIBRARY files.
        mockMvc.perform(get("/api/internal/user-files").with(asUserC()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    @DisplayName("USER can download ADMIN_LIBRARY file if permitted, but 403 if not permitted")
    void user_downloadAdminLibraryFile_permissionCheck() throws Exception {
        long exteriorOffsetId = ADMIN_LIBRARY_ID_OFFSET + svgExterior.getId();
        long interiorOffsetId = ADMIN_LIBRARY_ID_OFFSET + svgInterior.getId();

        // userA has permission for exterior -> download OK (200)
        mockMvc.perform(get("/api/internal/user-files/" + exteriorOffsetId + "/download").with(asUserA()))
                .andExpect(status().isOk());

        // userA does NOT have permission for interior -> 403 Forbidden
        mockMvc.perform(get("/api/internal/user-files/" + interiorOffsetId + "/download").with(asUserA()))
                .andExpect(status().isForbidden());

        // userA cannot access interior via /api/internal/svg-files/{id} either -> 403 Forbidden
        mockMvc.perform(get("/api/internal/svg-files/" + svgInterior.getId()).with(asUserA()))
                .andExpect(status().isForbidden());

        // userA CAN access exterior via /api/internal/svg-files/{id} -> 200 OK
        mockMvc.perform(get("/api/internal/svg-files/" + svgExterior.getId()).with(asUserA()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("USER cannot modify or delete ADMIN_LIBRARY files -> 403 Forbidden")
    void user_cannotMutateAdminLibraryFiles() throws Exception {
        long exteriorOffsetId = ADMIN_LIBRARY_ID_OFFSET + svgExterior.getId();

        mockMvc.perform(delete("/api/internal/user-files/" + exteriorOffsetId).with(asUserA()))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 3. Tích hợp API V2 (/api/internal/v2/user-files)
    // =========================================================================

    @Test
    @DisplayName("USER V2 list includes ADMIN_LIBRARY files according to permissions")
    void user_seesAdminLibraryFiles_V2() throws Exception {
        mockMvc.perform(get("/api/internal/v2/user-files").with(asUserA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].fileSource", is("ADMIN_LIBRARY")))
                .andExpect(jsonPath("$.content[0].fileName", is("Capo Ngoại Thất")));

        long exteriorOffsetId = ADMIN_LIBRARY_ID_OFFSET + svgExterior.getId();
        mockMvc.perform(get("/api/internal/v2/user-files/" + exteriorOffsetId).with(asUserA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileSource", is("ADMIN_LIBRARY")));

        long interiorOffsetId = ADMIN_LIBRARY_ID_OFFSET + svgInterior.getId();
        mockMvc.perform(get("/api/internal/v2/user-files/" + interiorOffsetId).with(asUserA()))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 4. AGENT xem “Bản đã lưu” của USER thuộc đại lý mình quản lý (Read-Only)
    // =========================================================================

    @Test
    @DisplayName("AGENT A only sees user saved files belonging to Dealer A")
    void agent_seesOnlyFilesOfOwnDealerUsers() throws Exception {
        // Agent A belongs to Dealer A. userA is in Dealer A (has savedFileUserA).
        // userB is in Dealer B (has savedFileUserB).
        mockMvc.perform(get("/api/agent/user-files").with(asAgentA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].fileName", is("UserA_Saved.svg")))
                .andExpect(jsonPath("$.content[0].dealer.name", is("Dealer A")));

        // Agent B belongs to Dealer B. Should only see savedFileUserB.
        mockMvc.perform(get("/api/agent/user-files").with(asAgentB()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].fileName", is("UserB_Saved.svg")))
                .andExpect(jsonPath("$.content[0].dealer.name", is("Dealer B")));
    }

    @Test
    @DisplayName("AGENT cannot call Admin user-files endpoint -> 403 Forbidden")
    void agent_cannotCallAdminUserFiles() throws Exception {
        mockMvc.perform(get("/api/admin/user-files").with(asAgentA()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER cannot call Agent user-files endpoint -> 403 Forbidden")
    void user_cannotCallAgentUserFiles() throws Exception {
        mockMvc.perform(get("/api/agent/user-files").with(asUserA()))
                .andExpect(status().isForbidden());
    }
}
