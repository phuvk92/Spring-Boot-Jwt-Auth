package com.example.svgmanager.integration;

import com.example.svgmanager.dto.request.CreateVehicleConfigurationRequest;
import com.example.svgmanager.dto.request.UpdateVehicleConfigurationRequest;
import com.example.svgmanager.entity.*;
import com.example.svgmanager.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
class VehicleConfigurationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

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

    private User adminUser;
    private User agentUser;
    private User normalUser;

    private Category categoryExterior;
    private CarBrand brandToyota;
    private CarBrand brandHonda;
    private CarModel modelCamry;
    private CarModel modelCivic;
    private VehicleConfiguration sampleConfig;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.save(User.builder()
                .keycloakUserId("kc-admin-vc")
                .username("admin_vc_test")
                .email("admin_vc@example.com")
                .password("hash")
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        agentUser = userRepository.save(User.builder()
                .keycloakUserId("kc-agent-vc")
                .username("agent_vc_test")
                .email("agent_vc@example.com")
                .password("hash")
                .role(Role.AGENT)
                .enabled(true)
                .build());

        normalUser = userRepository.save(User.builder()
                .keycloakUserId("kc-user-vc")
                .username("user_vc_test")
                .email("user_vc@example.com")
                .password("hash")
                .role(Role.USER)
                .enabled(true)
                .build());

        categoryExterior = categoryRepository.save(new Category(
                null, "Ngoại thất", "PPF Exterior", "category", null, null, 1, LocalDateTime.now()
        ));

        brandToyota = carBrandRepository.save(CarBrand.builder()
                .code("TOYOTA_TEST")
                .name("Toyota")
                .status("ACTIVE")
                .displayOrder(1)
                .build());

        brandHonda = carBrandRepository.save(CarBrand.builder()
                .code("HONDA_TEST")
                .name("Honda")
                .status("ACTIVE")
                .displayOrder(2)
                .build());

        modelCamry = carModelRepository.save(CarModel.builder()
                .brand(brandToyota)
                .code("CAMRY_TEST")
                .name("Camry")
                .status("ACTIVE")
                .build());

        modelCivic = carModelRepository.save(CarModel.builder()
                .brand(brandHonda)
                .code("CIVIC_TEST")
                .name("Civic")
                .status("ACTIVE")
                .build());

        sampleConfig = vehicleConfigurationRepository.save(VehicleConfiguration.builder()
                .category(categoryExterior)
                .productGroup(ProductGroup.PPF_EXTERIOR)
                .brand(brandToyota)
                .model(modelCamry)
                .yearFrom(2019)
                .yearTo(2023)
                .generationCode("XV70")
                .status("ACTIVE")
                .deleted(false)
                .build());
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminJwt() {
        return jwt().jwt(builder -> builder
                .subject(adminUser.getKeycloakUserId())
                .claim("preferred_username", adminUser.getUsername())
                .claim("realm_access", Map.of("roles", List.of("ADMIN", "ROLE_ADMIN")))
        ).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor agentJwt() {
        return jwt().jwt(builder -> builder
                .subject(agentUser.getKeycloakUserId())
                .claim("preferred_username", agentUser.getUsername())
                .claim("realm_access", Map.of("roles", List.of("AGENT", "ROLE_AGENT")))
        ).authorities(new SimpleGrantedAuthority("ROLE_AGENT"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor userJwt() {
        return jwt().jwt(builder -> builder
                .subject(normalUser.getKeycloakUserId())
                .claim("preferred_username", normalUser.getUsername())
                .claim("realm_access", Map.of("roles", List.of("USER", "ROLE_USER")))
        ).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    @DisplayName("Admin can get vehicle configurations list")
    void getConfigurations_Admin_Success() throws Exception {
        mockMvc.perform(get("/api/vehicle-configurations")
                        .with(adminJwt())
                        .param("brandId", brandToyota.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].generationCode").value("XV70"));
    }

    @Test
    @DisplayName("Non-admin (AGENT) is forbidden from accessing vehicle configurations")
    void getConfigurations_Agent_Forbidden() throws Exception {
        mockMvc.perform(get("/api/vehicle-configurations")
                        .with(agentJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Non-admin (USER) is forbidden from accessing vehicle configurations")
    void getConfigurations_User_Forbidden() throws Exception {
        mockMvc.perform(get("/api/vehicle-configurations")
                        .with(userJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to vehicle configurations returns 401")
    void getConfigurations_Unauthenticated_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/vehicle-configurations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin can create new vehicle configuration")
    void createConfiguration_Admin_Success() throws Exception {
        CreateVehicleConfigurationRequest request = new CreateVehicleConfigurationRequest(
                categoryExterior.getId(),
                "PPF_EXTERIOR",
                brandToyota.getId(),
                modelCamry.getId(),
                2024,
                2026,
                "XV80",
                "ACTIVE"
        );

        mockMvc.perform(post("/api/vehicle-configurations")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.generationCode").value("XV80"))
                .andExpect(jsonPath("$.yearFrom").value(2024))
                .andExpect(jsonPath("$.yearTo").value(2026));
    }

    @Test
    @DisplayName("Creating duplicate vehicle configuration returns 409 Conflict")
    void createConfiguration_Duplicate_Conflict() throws Exception {
        CreateVehicleConfigurationRequest duplicateRequest = new CreateVehicleConfigurationRequest(
                categoryExterior.getId(),
                "PPF_EXTERIOR",
                brandToyota.getId(),
                modelCamry.getId(),
                2019,
                2023,
                "XV70",
                "ACTIVE"
        );

        mockMvc.perform(post("/api/vehicle-configurations")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("VEHICLE_CONFIGURATION_ALREADY_EXISTS")));
    }

    @Test
    @DisplayName("Creating with invalid year range returns 400 Bad Request")
    void createConfiguration_InvalidYearRange_BadRequest() throws Exception {
        CreateVehicleConfigurationRequest invalidRequest = new CreateVehicleConfigurationRequest(
                categoryExterior.getId(),
                "PPF_EXTERIOR",
                brandToyota.getId(),
                modelCamry.getId(),
                2026,
                2020,
                "XV80",
                "ACTIVE"
        );

        mockMvc.perform(post("/api/vehicle-configurations")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("không được lớn hơn")));
    }

    @Test
    @DisplayName("Creating with model not belonging to brand returns 400 Bad Request")
    void createConfiguration_ModelBrandMismatch_BadRequest() throws Exception {
        // Civic belongs to Honda, not Toyota
        CreateVehicleConfigurationRequest mismatchRequest = new CreateVehicleConfigurationRequest(
                categoryExterior.getId(),
                "PPF_EXTERIOR",
                brandToyota.getId(),
                modelCivic.getId(),
                2020,
                2024,
                "FC",
                "ACTIVE"
        );

        mockMvc.perform(post("/api/vehicle-configurations")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mismatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("không thuộc Hãng xe")));
    }

    @Test
    @DisplayName("Admin can update vehicle configuration")
    void updateConfiguration_Admin_Success() throws Exception {
        UpdateVehicleConfigurationRequest request = new UpdateVehicleConfigurationRequest(
                categoryExterior.getId(),
                "PPF_EXTERIOR",
                brandToyota.getId(),
                modelCamry.getId(),
                2019,
                2024,
                "XV70-FACELIFT",
                "ACTIVE"
        );

        mockMvc.perform(put("/api/vehicle-configurations/" + sampleConfig.getId())
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generationCode").value("XV70-FACELIFT"))
                .andExpect(jsonPath("$.yearTo").value(2024));
    }

    @Test
    @DisplayName("Admin can soft-delete unused vehicle configuration")
    void deleteConfiguration_Admin_Success() throws Exception {
        mockMvc.perform(delete("/api/vehicle-configurations/" + sampleConfig.getId())
                        .with(adminJwt()))
                .andExpect(status().isNoContent());

        // Subsequent get should return 404
        mockMvc.perform(get("/api/vehicle-configurations/" + sampleConfig.getId())
                        .with(adminJwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deleting configuration in use by SVG returns 409 Conflict")
    void deleteConfiguration_InUse_Conflict() throws Exception {
        // Link SVG to configuration
        svgFileRepository.save(SvgFile.builder()
                .originalFilename("test.svg")
                .storedFilename("test_" + System.currentTimeMillis() + ".svg")
                .filePath("/tmp/test.svg")
                .fileSize(100L)
                .contentType("image/svg+xml")
                .category(categoryExterior)
                .vehicleConfiguration(sampleConfig)
                .uploadedBy(adminUser)
                .build());

        mockMvc.perform(delete("/api/vehicle-configurations/" + sampleConfig.getId())
                        .with(adminJwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("VEHICLE_CONFIGURATION_IN_USE")));
    }

    @Test
    @DisplayName("Any authenticated user can list Car Brands and Models")
    void getBrandsAndModels_Authenticated_Success() throws Exception {
        mockMvc.perform(get("/api/car-brands")
                        .with(agentJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));

        mockMvc.perform(get("/api/car-models")
                        .with(userJwt())
                        .param("brandId", brandToyota.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name").value("Camry"));
    }
}
