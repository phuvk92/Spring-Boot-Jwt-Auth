package com.example.svgmanager.integration;

import com.example.svgmanager.entity.*;
import com.example.svgmanager.repository.*;
import com.example.svgmanager.service.FileStorageService;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserSavedFilesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private FileCategoryRepository fileCategoryRepository;

    @Autowired
    private VehicleNodeRepository vehicleNodeRepository;

    @Autowired
    private UserSvgFileRepository userSvgFileRepository;

    @Autowired
    private FileStorageService fileStorageService;

    private User adminUser;
    private User user1;
    private User user2;
    private User agentUser;
    private Dealer dealerA;
    private Dealer dealerB;
    private FileCategory categoryPpf;
    private FileCategory categoryWindow;
    private VehicleNode brandToyota;
    private VehicleNode seriesCamry;
    private VehicleNode modelCamry25Q;
    private UserSvgFile fileUser1;
    private UserSvgFile fileUser2;

    private static final String VALID_SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\"><rect width=\"100\" height=\"100\" fill=\"red\"/></svg>";

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asAdmin() {
        return jwt().jwt(j -> j.subject("kc-admin-test").claim("preferred_username", "admin_saved_test"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUser1() {
        return jwt().jwt(j -> j.subject("kc-user1-test").claim("preferred_username", "user1_saved_test"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUser2() {
        return jwt().jwt(j -> j.subject("kc-user2-test").claim("preferred_username", "user2_saved_test"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asAgent() {
        return jwt().jwt(j -> j.subject("kc-agent-test").claim("preferred_username", "agent_saved_test"))
                .authorities(new SimpleGrantedAuthority("ROLE_AGENT"));
    }

    @BeforeEach
    void setUp() {
        userSvgFileRepository.deleteAll();

        // 1. Dealers
        dealerA = dealerRepository.save(Dealer.builder().code("DLA").name("Dealer Alpha").build());
        dealerB = dealerRepository.save(Dealer.builder().code("DLB").name("Dealer Beta").build());

        // 2. Users
        adminUser = userRepository.save(User.builder()
                .username("admin_saved_test")
                .email("admin_saved_test@test.local")
                .keycloakUserId("kc-admin-test")
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        user1 = userRepository.save(User.builder()
                .username("user1_saved_test")
                .email("user1@test.local")
                .keycloakUserId("kc-user1-test")
                .fullName("Nguyen Van A")
                .dealer(dealerA)
                .role(Role.USER)
                .enabled(true)
                .build());

        user2 = userRepository.save(User.builder()
                .username("user2_saved_test")
                .email("user2@test.local")
                .keycloakUserId("kc-user2-test")
                .fullName("Tran Van B")
                .dealer(dealerB)
                .role(Role.USER)
                .enabled(true)
                .build());

        agentUser = userRepository.save(User.builder()
                .username("agent_saved_test")
                .email("agent@test.local")
                .keycloakUserId("kc-agent-test")
                .role(Role.AGENT)
                .enabled(true)
                .build());

        // 3. Categories
        categoryPpf = fileCategoryRepository.save(new FileCategory("PPF Ngoại thất", 1));
        categoryWindow = fileCategoryRepository.save(new FileCategory("Window Film", 2));

        // 4. Vehicle tree
        brandToyota = vehicleNodeRepository.save(new VehicleNode(null, VehicleNodeLevel.BRAND, "Toyota", 1));
        seriesCamry = vehicleNodeRepository.save(new VehicleNode(brandToyota, VehicleNodeLevel.SERIES, "Camry", 1));
        modelCamry25Q = vehicleNodeRepository.save(new VehicleNode(seriesCamry, VehicleNodeLevel.MODEL, "Camry 2.5Q", 1));

        // 5. Store physical sample file
        String path1 = fileStorageService.storeFile(VALID_SVG.getBytes(StandardCharsets.UTF_8), "test1.svg");
        String path2 = fileStorageService.storeFile(VALID_SVG.getBytes(StandardCharsets.UTF_8), "test2.svg");

        // 6. User 1 saved file
        fileUser1 = userSvgFileRepository.save(UserSvgFile.builder()
                .fileName("BMW_X5_G05_DOOR.svg")
                .originalFileName("BMW_X5_G05_DOOR.svg")
                .storedFileName("test1.svg")
                .filePath(path1)
                .fileSize(1258291L)
                .mimeType("image/svg+xml")
                .checksum("sha256:mock_hash_1")
                .category(categoryPpf)
                .vehicleNode(modelCamry25Q)
                .productGroup("PPF_EXTERIOR")
                .productGroupName("PPF Exterior")
                .brandName("BMW")
                .modelName("X5")
                .yearFrom(2019)
                .yearTo(2023)
                .generationCode("G05")
                .filmWidth(1520.0)
                .filmWidthUnit("MM")
                .rollLength(3500.0)
                .rollLengthUnit("MM")
                .axisX(3500.0)
                .axisY(1520.0)
                .description("Mẫu cắt cửa xe BMW X5 G05")
                .status("ACTIVE")
                .user(user1)
                .dealer(dealerA)
                .createdAt(LocalDateTime.of(2026, 10, 1, 10, 0))
                .build());

        // 7. User 2 saved file
        fileUser2 = userSvgFileRepository.save(UserSvgFile.builder()
                .fileName("Toyota_Camry_Window.svg")
                .originalFileName("Toyota_Camry_Window.svg")
                .storedFileName("test2.svg")
                .filePath(path2)
                .fileSize(845210L)
                .mimeType("image/svg+xml")
                .checksum("sha256:mock_hash_2")
                .category(categoryWindow)
                .vehicleNode(modelCamry25Q)
                .productGroup("WINDOW_FILM")
                .productGroupName("Window Film")
                .brandName("Toyota")
                .modelName("Camry")
                .yearFrom(2022)
                .yearTo(2025)
                .filmWidth(1000.0)
                .filmWidthUnit("MM")
                .rollLength(2000.0)
                .rollLengthUnit("MM")
                .axisX(2000.0)
                .axisY(1000.0)
                .description("Phim cách nhiệt cửa kính Camry")
                .status("ACTIVE")
                .user(user2)
                .dealer(dealerB)
                .createdAt(LocalDateTime.of(2026, 10, 2, 14, 0))
                .build());
    }

    // ==========================================
    // 1. ADMIN LIST ALL & MULTI-USER / MULTI-DEALER
    // ==========================================

    @Test
    @DisplayName("ADMIN list all user saved files from multiple dealers and users")
    void admin_canListAllUserFiles_acrossDealersAndUsers() throws Exception {
        mockMvc.perform(get("/api/admin/user-files").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].fileName", containsInAnyOrder("BMW_X5_G05_DOOR.svg", "Toyota_Camry_Window.svg")))
                .andExpect(jsonPath("$.content[*].dealer.name", containsInAnyOrder("Dealer Alpha", "Dealer Beta")))
                .andExpect(jsonPath("$.content[*].createdBy.username", containsInAnyOrder("user1_saved_test", "user2_saved_test")));
    }

    // ==========================================
    // 2. AUTHORIZATION TESTS
    // ==========================================

    @Test
    @DisplayName("USER cannot call admin list -> 403 Forbidden")
    void user_cannotCallAdminList_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/user-files").with(asUser1()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AGENT cannot call admin list -> 403 Forbidden")
    void agent_cannotCallAdminList_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/user-files").with(asAgent()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated cannot call admin list -> 401 Unauthorized")
    void unauthenticated_cannotCallAdminList_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/user-files"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // 3. ADMIN GET DETAIL & DOWNLOAD
    // ==========================================

    @Test
    @DisplayName("ADMIN get detail returns complete metadata matching spec")
    void admin_getDetail_success() throws Exception {
        mockMvc.perform(get("/api/admin/user-files/" + fileUser1.getId()).with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(fileUser1.getId().intValue())))
                .andExpect(jsonPath("$.fileName", is("BMW_X5_G05_DOOR.svg")))
                .andExpect(jsonPath("$.originalFileName", is("BMW_X5_G05_DOOR.svg")))
                .andExpect(jsonPath("$.category.id", is(categoryPpf.getId().intValue())))
                .andExpect(jsonPath("$.category.name", is("PPF Ngoại thất")))
                .andExpect(jsonPath("$.vehicleConfiguration.brandName", is("BMW")))
                .andExpect(jsonPath("$.vehicleConfiguration.modelName", is("X5")))
                .andExpect(jsonPath("$.vehicleConfiguration.yearFrom", is(2019)))
                .andExpect(jsonPath("$.vehicleConfiguration.yearTo", is(2023)))
                .andExpect(jsonPath("$.vehicleConfiguration.generationCode", is("G05")))
                .andExpect(jsonPath("$.cutSize.filmWidth", is(1520.0)))
                .andExpect(jsonPath("$.cutSize.rollLength", is(3500.0)))
                .andExpect(jsonPath("$.cutSize.axisX", is(3500.0)))
                .andExpect(jsonPath("$.cutSize.axisY", is(1520.0)))
                .andExpect(jsonPath("$.description", is("Mẫu cắt cửa xe BMW X5 G05")))
                .andExpect(jsonPath("$.createdBy.username", is("user1_saved_test")))
                .andExpect(jsonPath("$.createdBy.displayName", is("Nguyen Van A")))
                .andExpect(jsonPath("$.dealer.name", is("Dealer Alpha")))
                .andExpect(jsonPath("$.fileSize", is(1258291)))
                .andExpect(jsonPath("$.mimeType", is("image/svg+xml")))
                .andExpect(jsonPath("$.checksum", is("sha256:mock_hash_1")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                // Ensure storageKey and physicalPath are NOT exposed
                .andExpect(jsonPath("$.storageKey").doesNotExist())
                .andExpect(jsonPath("$.physicalPath").doesNotExist())
                .andExpect(jsonPath("$.filePath").doesNotExist());
    }

    @Test
    @DisplayName("ADMIN download file returns SVG attachment")
    void admin_download_success() throws Exception {
        mockMvc.perform(get("/api/admin/user-files/" + fileUser1.getId() + "/download").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("image/svg+xml")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("BMW_X5_G05_DOOR.svg")))
                .andExpect(content().string(containsString("<svg")));
    }

    // ==========================================
    // 4. PAGINATION & FILTERS
    // ==========================================

    @Test
    @DisplayName("Pagination works")
    void pagination_works() throws Exception {
        mockMvc.perform(get("/api/admin/user-files")
                        .param("page", "0")
                        .param("size", "1")
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(1)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("Search by keyword (filename)")
    void search_byFilename() throws Exception {
        mockMvc.perform(get("/api/admin/user-files")
                        .param("keyword", "BMW")
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].fileName", is("BMW_X5_G05_DOOR.svg")));
    }

    @Test
    @DisplayName("Filter by category")
    void filter_byCategory() throws Exception {
        mockMvc.perform(get("/api/admin/user-files")
                        .param("categoryId", categoryWindow.getId().toString())
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].fileName", is("Toyota_Camry_Window.svg")));
    }

    @Test
    @DisplayName("Filter by dealer")
    void filter_byDealer() throws Exception {
        mockMvc.perform(get("/api/admin/user-files")
                        .param("dealerId", dealerB.getId().toString())
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].dealer.name", is("Dealer Beta")));
    }

    @Test
    @DisplayName("Filter by user")
    void filter_byUser() throws Exception {
        mockMvc.perform(get("/api/admin/user-files")
                        .param("userId", user1.getId().toString())
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].createdBy.username", is("user1_saved_test")));
    }

    @Test
    @DisplayName("Filter by vehicle configuration (vehicleNodeId)")
    void filter_byVehicleNode() throws Exception {
        mockMvc.perform(get("/api/admin/user-files")
                        .param("vehicleNodeId", modelCamry25Q.getId().toString())
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    @DisplayName("Filter by date range")
    void filter_byDateRange() throws Exception {
        // Since @CreatedDate sets createdAt to now, query around now
        LocalDateTime now = LocalDateTime.now();
        mockMvc.perform(get("/api/admin/user-files")
                        .param("createdFrom", now.minusHours(1).toString())
                        .param("createdTo", now.plusHours(1).toString())
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)));

        // Range in the future -> 0
        mockMvc.perform(get("/api/admin/user-files")
                        .param("createdFrom", now.plusDays(10).toString())
                        .param("createdTo", now.plusDays(20).toString())
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    // ==========================================
    // 5. INTERNAL USER API (SAVE & OWN SCOPE)
    // ==========================================

    @Test
    @DisplayName("USER can upload file via POST /api/internal/user-files (Multipart)")
    void user_canUploadFile_multipart() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "custom_door.svg",
                "image/svg+xml",
                VALID_SVG.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/internal/user-files")
                        .file(file)
                        .param("description", "Cắt decal xe")
                        .param("filmWidth", "1500")
                        .param("rollLength", "3000")
                        .with(asUser1()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.fileName", is("custom_door.svg")))
                .andExpect(jsonPath("$.createdBy.username", is("user1_saved_test")))
                .andExpect(jsonPath("$.dealer.name", is("Dealer Alpha")));
    }

    @Test
    @DisplayName("USER only sees own files via GET /api/internal/user-files")
    void user_onlySeesOwnFiles() throws Exception {
        mockMvc.perform(get("/api/internal/user-files").with(asUser1()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].fileName", is("BMW_X5_G05_DOOR.svg")));

        mockMvc.perform(get("/api/internal/user-files").with(asUser2()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].fileName", is("Toyota_Camry_Window.svg")));
    }

    @Test
    @DisplayName("USER cannot download another user file via internal API -> 404")
    void user_cannotDownloadOtherUserFile_returns404() throws Exception {
        mockMvc.perform(get("/api/internal/user-files/" + fileUser2.getId() + "/download").with(asUser1()))
                .andExpect(status().isNotFound());
    }

    @Autowired
    private SvgFileRepository svgFileRepository;

    @Test
    @DisplayName("USER can update existing file via PUT /api/internal/user-files/{id} (Multipart)")
    void user_canUpdateFile_multipart() throws Exception {
        // Prepare source svg file in catalog
        SvgFile sourceSvg = SvgFile.builder()
                .originalFilename("Audi_Q6_2024.svg")
                .status("ACTIVE")
                .uploadedBy(adminUser)
                .build();
        sourceSvg.setFileKey("audi-q6-2024-full");
        sourceSvg.setDisplayName("Audi Q6 2024 - Full Body");
        svgFileRepository.save(sourceSvg);

        String updatedSvgContent = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"200\" height=\"200\"><circle cx=\"100\" cy=\"100\" r=\"50\" fill=\"blue\"/></svg>";
        MockMultipartFile newFile = new MockMultipartFile(
                "file",
                "audi_modified.svg",
                "image/svg+xml",
                updatedSvgContent.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/internal/user-files/" + fileUser1.getId())
                        .file(newFile)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .param("fileName", "Audi_Q6_Custom.svg")
                        .param("sourceFileKey", "audi-q6-2024-full")
                        .param("description", "Bản đã xếp lại nắp capo")
                        .param("filmWidth", "1520")
                        .param("rollLength", "4000")
                        .with(asUser1()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(fileUser1.getId().intValue())))
                .andExpect(jsonPath("$.fileName", is("Audi_Q6_Custom.svg")))
                .andExpect(jsonPath("$.originalFileName", is("audi_modified.svg")))
                .andExpect(jsonPath("$.sourceFileKey", is("audi-q6-2024-full")))
                .andExpect(jsonPath("$.sourceFileName", is("Audi Q6 2024 - Full Body")))
                .andExpect(jsonPath("$.description", is("Bản đã xếp lại nắp capo")))
                .andExpect(jsonPath("$.createdBy.username", is("user1_saved_test")));

        // Verify GET returns updated data and same ID
        mockMvc.perform(get("/api/internal/user-files/" + fileUser1.getId()).with(asUser1()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(fileUser1.getId().intValue())))
                .andExpect(jsonPath("$.fileName", is("Audi_Q6_Custom.svg")))
                .andExpect(jsonPath("$.sourceFileKey", is("audi-q6-2024-full")))
                .andExpect(jsonPath("$.sourceFileName", is("Audi Q6 2024 - Full Body")));
    }

    @Test
    @DisplayName("USER cannot update another user's file via PUT /api/internal/user-files/{id} -> 404")
    void user_cannotUpdateOtherUserFile_returns404() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hack.svg",
                "image/svg+xml",
                VALID_SVG.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/internal/user-files/" + fileUser2.getId())
                        .file(file)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .param("fileName", "Hacked.svg")
                        .with(asUser1()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/internal/user-files with sourceFileKey saves and returns sourceFileKey and sourceFileName")
    void user_saveWithSourceFileKey_returnsSourceFileDetails() throws Exception {
        SvgFile sourceSvg = SvgFile.builder()
                .originalFilename("BMW_Door_Template.svg")
                .status("ACTIVE")
                .uploadedBy(adminUser)
                .build();
        sourceSvg.setFileKey("bmw-door-template-key");
        sourceSvg.setDisplayName("BMW G05 Door Template");
        svgFileRepository.save(sourceSvg);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "my_custom.svg",
                "image/svg+xml",
                VALID_SVG.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/internal/user-files")
                        .file(file)
                        .param("fileName", "My_BMW.svg")
                        .param("sourceFileKey", "bmw-door-template-key")
                        .with(asUser1()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceFileKey", is("bmw-door-template-key")))
                .andExpect(jsonPath("$.sourceFileName", is("BMW G05 Door Template")));
    }
}
