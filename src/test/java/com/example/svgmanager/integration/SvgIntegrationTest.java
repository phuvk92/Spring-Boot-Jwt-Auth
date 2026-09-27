package com.example.svgmanager.integration;

import com.example.svgmanager.dto.request.AssignDealersRequest;
import com.example.svgmanager.dto.request.AssignVehicleConfigurationsRequest;
import com.example.svgmanager.dto.request.SvgFileDealerPermissionRequest;
import com.example.svgmanager.entity.*;
import com.example.svgmanager.repository.*;
import com.example.svgmanager.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SvgIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CarBrandRepository carBrandRepository;

    @Autowired
    private CarModelRepository carModelRepository;

    @Autowired
    private VehicleConfigurationRepository vehicleConfigurationRepository;

    @Autowired
    private SvgFileRepository svgFileRepository;

    @Autowired
    private SvgFileDealerPermissionRepository svgDealerPermissionRepository;

    @Autowired
    private SvgFileVehicleConfigurationRepository svgVehicleConfigRepository;

    @MockBean
    private FileStorageService fileStorageService;

    private User adminUser;
    private User userInDealerA;
    private User userInDealerB;
    private User userInDealerC;
    private User userWithoutDealer;

    private Dealer dealerA;
    private Dealer dealerB;
    private Dealer dealerC;

    private Category categoryExterior;
    private VehicleConfiguration config1;
    private SvgFile sampleSvg;

    @BeforeEach
    void setUp() {
        when(fileStorageService.storeFile(any(), any())).thenReturn("/mock/storage/sample.svg");
        when(fileStorageService.loadFileAsResource(any())).thenReturn(
                new ByteArrayResource("<svg viewBox=\"0 0 100 100\"><circle cx=\"50\" cy=\"50\" r=\"40\"/></svg>".getBytes(StandardCharsets.UTF_8))
        );

        // 1. Create Dealers
        dealerA = dealerRepository.save(Dealer.builder().code("DLA").name("Dealer A").status("ACTIVE").build());
        dealerB = dealerRepository.save(Dealer.builder().code("DLB").name("Dealer B").status("ACTIVE").build());
        dealerC = dealerRepository.save(Dealer.builder().code("DLC").name("Dealer C").status("ACTIVE").build());

        // 2. Create Users
        adminUser = userRepository.save(User.builder()
                .username("admin_svg_test")
                .keycloakUserId("kc-admin-svg")
                .email("admin_svg@example.com")
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        userInDealerA = userRepository.save(User.builder()
                .username("user_dla")
                .keycloakUserId("kc-user-dla")
                .email("user_dla@example.com")
                .role(Role.USER)
                .dealer(dealerA)
                .enabled(true)
                .build());

        userInDealerB = userRepository.save(User.builder()
                .username("user_dlb")
                .keycloakUserId("kc-user-dlb")
                .email("user_dlb@example.com")
                .role(Role.USER)
                .dealer(dealerB)
                .enabled(true)
                .build());

        userInDealerC = userRepository.save(User.builder()
                .username("user_dlc")
                .keycloakUserId("kc-user-dlc")
                .email("user_dlc@example.com")
                .role(Role.USER)
                .dealer(dealerC)
                .enabled(true)
                .build());

        userWithoutDealer = userRepository.save(User.builder()
                .username("user_nodealer")
                .keycloakUserId("kc-user-nodealer")
                .email("user_nodealer@example.com")
                .role(Role.USER)
                .dealer(null)
                .enabled(true)
                .build());

        // 3. Category, Brand, Model, Config
        categoryExterior = categoryRepository.save(new Category(
                null, "Ngoại thất", "PPF Exterior", "category", null, null, 1, LocalDateTime.now()
        ));

        CarBrand brand = carBrandRepository.save(CarBrand.builder().name("BMW").code("BMW").build());
        CarModel model = carModelRepository.save(CarModel.builder().brand(brand).name("X5").code("X5").build());

        config1 = vehicleConfigurationRepository.save(VehicleConfiguration.builder()
                .category(categoryExterior)
                .productGroup(ProductGroup.PPF_EXTERIOR)
                .brand(brand)
                .model(model)
                .yearFrom(2020)
                .yearTo(2024)
                .generationCode("G05")
                .build());

        // 4. Create sample SVG
        sampleSvg = svgFileRepository.save(SvgFile.builder()
                .originalFilename("bmw_x5_hood.svg")
                .storedFilename("uuid-bmw-x5.svg")
                .filePath("/mock/storage/sample.svg")
                .fileSize(1024L)
                .contentType("image/svg+xml")
                .checksum("fakechecksum123")
                .status("ACTIVE")
                .uploadedBy(adminUser)
                .build());

        // Assign to config1
        SvgFileVehicleConfiguration svc = new SvgFileVehicleConfiguration(sampleSvg, config1);
        sampleSvg.getVehicleConfigurations().add(svc);
        svgVehicleConfigRepository.save(svc);

        // Dealer A: View = true, Download = true
        SvgFileDealerPermission dpA = new SvgFileDealerPermission(sampleSvg, dealerA, true, true);
        sampleSvg.getDealerPermissions().add(dpA);
        svgDealerPermissionRepository.save(dpA);

        // Dealer B: View = true, Download = false
        SvgFileDealerPermission dpB = new SvgFileDealerPermission(sampleSvg, dealerB, true, false);
        sampleSvg.getDealerPermissions().add(dpB);
        svgDealerPermissionRepository.save(dpB);

        entityManager.flush();
        entityManager.clear();
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asAdmin() {
        return jwt().jwt(j -> j.subject("kc-admin-svg").claim("preferred_username", "admin_svg_test"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUserDealerA() {
        return jwt().jwt(j -> j.subject("kc-user-dla").claim("preferred_username", "user_dla"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUserDealerB() {
        return jwt().jwt(j -> j.subject("kc-user-dlb").claim("preferred_username", "user_dlb"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUserDealerC() {
        return jwt().jwt(j -> j.subject("kc-user-dlc").claim("preferred_username", "user_dlc"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUserNoDealer() {
        return jwt().jwt(j -> j.subject("kc-user-nodealer").claim("preferred_username", "user_nodealer"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    @DisplayName("Admin can batch upload up to 10 SVG files with vehicle config and dealer permissions")
    void adminBatchUpload_Success() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "part1.svg", "image/svg+xml",
                "<svg><rect width=\"10\" height=\"10\"/></svg>".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile file2 = new MockMultipartFile("files", "part2.svg", "image/svg+xml",
                "<svg><circle cx=\"5\" cy=\"5\" r=\"4\"/></svg>".getBytes(StandardCharsets.UTF_8));

        String dealerPermsJson = objectMapper.writeValueAsString(List.of(
                new SvgFileDealerPermissionRequest(dealerA.getId(), true, true)
        ));

        mockMvc.perform(multipart("/api/svg/batch")
                        .file(file1)
                        .file(file2)
                        .param("vehicleConfigurationIds", String.valueOf(config1.getId()))
                        .param("dealerPermissions", dealerPermsJson)
                        .with(asAdmin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalUploaded", is(2)))
                .andExpect(jsonPath("$.files", hasSize(2)));
    }

    @Test
    @DisplayName("Batch upload rejects if files exceed 10")
    void adminBatchUpload_Exceed10_BadRequest() throws Exception {
        var builder = multipart("/api/svg/batch");
        for (int i = 0; i < 11; i++) {
            builder.file(new MockMultipartFile("files", "file" + i + ".svg", "image/svg+xml", "<svg/>".getBytes(StandardCharsets.UTF_8)));
        }

        mockMvc.perform(builder.with(asAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("tối đa 10 file")));
    }

    @Test
    @DisplayName("User cannot upload SVG files")
    void userCannotUpload_Forbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "part1.svg", "image/svg+xml", "<svg/>".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/svg/batch")
                        .file(file)
                        .with(asUserDealerA()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin sees all SVG files and can filter by dealerId")
    void adminList_SeesAll() throws Exception {
        mockMvc.perform(get("/api/svg")
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].originalFilename", is("bmw_x5_hood.svg")))
                .andExpect(jsonPath("$.content[0].dealerPermissionCount", is(2)));
    }

    @Test
    @DisplayName("User Dealer A (View=true) sees the SVG in list")
    void userDealerA_SeesFile() throws Exception {
        mockMvc.perform(get("/api/svg")
                        .with(asUserDealerA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].canView", is(true)))
                .andExpect(jsonPath("$.content[0].canDownload", is(true)));
    }

    @Test
    @DisplayName("User Dealer B (View=true, Download=false) sees the file but canDownload is false")
    void userDealerB_SeesFile_CannotDownloadInList() throws Exception {
        mockMvc.perform(get("/api/svg")
                        .with(asUserDealerB()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].canView", is(true)))
                .andExpect(jsonPath("$.content[0].canDownload", is(false)));
    }

    @Test
    @DisplayName("User Dealer C (no permission) does NOT see the file in list")
    void userDealerC_DoesNotSeeFile() throws Exception {
        mockMvc.perform(get("/api/svg")
                        .with(asUserDealerC()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("User without dealer receives empty list")
    void userNoDealer_EmptyList() throws Exception {
        mockMvc.perform(get("/api/svg")
                        .with(asUserNoDealer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("User Dealer A downloads file successfully")
    void userDealerA_Download_Success() throws Exception {
        mockMvc.perform(get("/api/svg/" + sampleSvg.getId() + "/download")
                        .with(asUserDealerA()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("bmw_x5_hood.svg")));
    }

    @Test
    @DisplayName("User Dealer B download returns 403 Forbidden")
    void userDealerB_Download_Forbidden() throws Exception {
        mockMvc.perform(get("/api/svg/" + sampleSvg.getId() + "/download")
                        .with(asUserDealerB()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("DOWNLOAD_PERMISSION_DENIED")));
    }

    @Test
    @DisplayName("User Dealer C download returns 404 Not Found (no leak)")
    void userDealerC_Download_NotFound() throws Exception {
        mockMvc.perform(get("/api/svg/" + sampleSvg.getId() + "/download")
                        .with(asUserDealerC()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Admin can update dealer permissions")
    void adminUpdateDealerPermissions_Success() throws Exception {
        AssignDealersRequest req = new AssignDealersRequest(List.of(
                new SvgFileDealerPermissionRequest(dealerC.getId(), true, true)
        ));

        mockMvc.perform(put("/api/svg/" + sampleSvg.getId() + "/dealers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].dealerCode", is("DLC")))
                .andExpect(jsonPath("$[0].canDownload", is(true)));
    }

    @Test
    @DisplayName("Admin can update vehicle configuration assignments")
    void adminUpdateVehicleConfigurations_Success() throws Exception {
        AssignVehicleConfigurationsRequest req = new AssignVehicleConfigurationsRequest(List.of(config1.getId()));

        mockMvc.perform(put("/api/svg/" + sampleSvg.getId() + "/vehicle-configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].generationCode", is("G05")));
    }
}
