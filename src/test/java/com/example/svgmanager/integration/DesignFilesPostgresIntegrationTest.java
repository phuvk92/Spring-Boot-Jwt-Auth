package com.example.svgmanager.integration;

import com.example.svgmanager.entity.FileCategory;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.SvgFileVehicleNode;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.VehicleNode;
import com.example.svgmanager.repository.FileCategoryRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.SvgFileVehicleNodeRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.repository.VehicleNodeRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test tích hợp cho /api/v1/files (app thợ) chạy trên PostgreSQL thật — NGO-354 AC.
 * Cây xe seed từ V15:
 *   Toyota (BRAND id=1) › Camry (SERIES id=2) › Camry 2.5Q (MODEL id=3) › Bản lắp ráp VN (SUBTYPE id=4)
 *   VinFast (BRAND id=9) › VF 8 (SERIES id=10) › VF 8 Plus (MODEL id=11)
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://mock-keycloak/realms/cutting/protocol/openid-connect/certs",
        "app.file.storage-path=target/test-svg-storage-designfiles",
        "app.keycloak.sync-enabled=false",
        "app.device.enforce=false"
})
@AutoConfigureMockMvc
@Transactional
class DesignFilesPostgresIntegrationTest {

    static PostgreSQLContainer<?> postgres;

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        String url = System.getenv("IT_DB_URL");
        String user = System.getenv("IT_DB_USER");
        String pass = System.getenv("IT_DB_PASSWORD");
        if (url == null) {
            postgres = new PostgreSQLContainer<>("postgres:16-alpine");
            postgres.start();
            url = postgres.getJdbcUrl();
            user = postgres.getUsername();
            pass = postgres.getPassword();
        }
        final String u = url;
        final String usr = user != null ? user : "postgres";
        final String pwd = pass != null ? pass : "postgres";
        registry.add("spring.datasource.url", () -> u);
        registry.add("spring.datasource.username", () -> usr);
        registry.add("spring.datasource.password", () -> pwd);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @AfterAll
    static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SvgFileRepository svgFileRepository;

    @Autowired
    private SvgFileVehicleNodeRepository linkRepository;

    @Autowired
    private VehicleNodeRepository vehicleNodeRepository;

    @Autowired
    private FileCategoryRepository fileCategoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final long BRAND_TOYOTA = 1;
    private static final long MODEL_CAMRY_25Q = 3;
    private static final long SUBTYPE_CAMRY_VN = 4;
    private static final long BRAND_VINFAST = 9;
    private static final long MODEL_VF8_PLUS = 11;
    private static final long CATEGORY_NGOAI_THAT = 1;

    private User uploader;
    private FileCategory ngoaiThat;

    @BeforeEach
    void setUp() {
        uploader = userRepository.findByUsername("it-test-uploader")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("it-test-uploader")
                        .email("it-uploader@test.local")
                        .fullName("IT Test Uploader")
                        .keycloakUserId("kc-it-uploader")
                        .password(passwordEncoder.encode("secret"))
                        .role(Role.ADMIN)
                        .enabled(true)
                        .deleted(false)
                        .build()));

        ngoaiThat = fileCategoryRepository.findById(CATEGORY_NGOAI_THAT).orElseThrow();
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUser() {
        return jwt().jwt(j -> j.subject("kc-user").claim("preferred_username", "worker"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private SvgFile createFile(String key, String displayName, String filename,
                               FileCategory category, Integer year, String status) {
        SvgFile file = SvgFile.builder()
                .originalFilename(filename)
                .storedFilename("stored-" + key + ".svg")
                .filePath("/tmp/" + key + ".svg")
                .fileSize(2048L)
                .contentType("image/svg+xml")
                .status(status)
                .uploadedBy(uploader)
                .build();
        file.setFileKey(key);
        file.setDisplayName(displayName);
        file.setFilmUsage("2,50 m");
        file.setNote("Note for " + key);
        file.setFileCategory(category);
        file.setModelYear(year);
        return svgFileRepository.save(file);
    }

    private void link(SvgFile file, long nodeId) {
        VehicleNode node = vehicleNodeRepository.findById(nodeId).orElseThrow();
        SvgFileVehicleNode link = linkRepository.save(new SvgFileVehicleNode(file, node));
        file.getVehicleNodes().add(link);
    }

    // ---------- AC 1: Không tham số → mọi file ACTIVE; file DELETED không hiện ----------

    @Test
    @DisplayName("AC: không tham số → mọi file ACTIVE, file DELETED không hiện")
    void getFiles_noParams_returnsAllActiveFilesAndHidesDeleted() throws Exception {
        SvgFile f1 = createFile("f-act-1", "Camry Ngoại Thất", "camry.svg", ngoaiThat, 2024, "ACTIVE");
        link(f1, MODEL_CAMRY_25Q);

        SvgFile f2 = createFile("f-act-2", "VF8 Ngoại Thất", "vf8.svg", ngoaiThat, 2023, "ACTIVE");
        link(f2, MODEL_VF8_PLUS);

        createFile("f-unlinked-1", "File Chưa Gắn", "unlinked.svg", ngoaiThat, 2022, "ACTIVE");

        SvgFile fDel = createFile("f-deleted-1", "File Đã Xoá", "deleted.svg", ngoaiThat, 2024, "DELETED");
        link(fDel, MODEL_CAMRY_25Q);

        mockMvc.perform(get("/api/v1/files").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItems("f-act-1", "f-act-2", "f-unlinked-1")))
                .andExpect(jsonPath("$.content[?(@.id == 'f-deleted-1')]").doesNotExist());
    }

    // ---------- AC 2: Chỉ brandId → đủ file của các model con ----------

    @Test
    @DisplayName("AC: chỉ brandId → đủ file của các model và subtype con thuộc hãng")
    void getFiles_onlyBrandId_returnsAllDescendantFiles() throws Exception {
        // File 1: gắn model Camry 2.5Q thuộc Toyota
        SvgFile fModel = createFile("f-toyota-model", "Toyota Model File", "t_model.svg", ngoaiThat, 2024, "ACTIVE");
        link(fModel, MODEL_CAMRY_25Q);

        // File 2: gắn subtype Bản lắp ráp VN thuộc Camry 2.5Q thuộc Toyota
        SvgFile fSub = createFile("f-toyota-sub", "Toyota Subtype File", "t_sub.svg", ngoaiThat, 2024, "ACTIVE");
        link(fSub, SUBTYPE_CAMRY_VN);

        // File 3: gắn model VF 8 Plus thuộc VinFast (khác hãng)
        SvgFile fVinFast = createFile("f-vinfast-model", "VinFast File", "vf.svg", ngoaiThat, 2024, "ACTIVE");
        link(fVinFast, MODEL_VF8_PLUS);

        // File 4: không gắn xe
        createFile("f-unlinked-other", "No Car File", "nocar.svg", ngoaiThat, 2024, "ACTIVE");

        mockMvc.perform(get("/api/v1/files")
                        .param("brandId", String.valueOf(BRAND_TOYOTA))
                        .with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItems("f-toyota-model", "f-toyota-sub")))
                .andExpect(jsonPath("$.content[?(@.id == 'f-vinfast-model')]").doesNotExist())
                .andExpect(jsonPath("$.content[?(@.id == 'f-unlinked-other')]").doesNotExist());
    }

    // ---------- AC 3: q khớp tên hiển thị hoặc tên file gốc, không phân biệt hoa thường ----------

    @Test
    @DisplayName("AC: q khớp tên hiển thị hoặc tên file gốc, không phân biệt hoa thường")
    void getFiles_q_matchesDisplayNameOrOriginalFilenameCaseInsensitive() throws Exception {
        createFile("f-q-1", "Dán Cản Trước Thể Thao", "can_truoc_01.svg", ngoaiThat, 2024, "ACTIVE");
        createFile("f-q-2", "Bộ Tem Thân Xe", "sport_stripes.svg", ngoaiThat, 2024, "ACTIVE");
        createFile("f-q-3", "Nẹp Chân Kính", "kinh_side.svg", ngoaiThat, 2024, "ACTIVE");

        // Khớp displayName (chữ hoa chữ thường)
        mockMvc.perform(get("/api/v1/files").param("q", "cản trước").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is("f-q-1")));

        // Khớp originalFilename không dấu / tiếng Anh
        mockMvc.perform(get("/api/v1/files").param("q", "SPORT").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is("f-q-2")));
    }

    // ---------- AC 4: year khớp cả file năm NULL ----------

    @Test
    @DisplayName("AC: year khớp cả file đúng năm và file có model_year IS NULL (Q3)")
    void getFiles_year_matchesTargetYearAndNullYearFiles() throws Exception {
        createFile("f-year-2024", "File 2024", "y2024.svg", ngoaiThat, 2024, "ACTIVE");
        createFile("f-year-null", "File Mọi Năm", "ynull.svg", ngoaiThat, null, "ACTIVE");
        createFile("f-year-2020", "File 2020", "y2020.svg", ngoaiThat, 2020, "ACTIVE");

        mockMvc.perform(get("/api/v1/files").param("year", "2024").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItems("f-year-2024", "f-year-null")))
                .andExpect(jsonPath("$.content[?(@.id == 'f-year-2020')]").doesNotExist());
    }

    // ---------- AC 5: Phân trang size=1 ra totalPages đúng ----------

    @Test
    @DisplayName("AC: phân trang size=1 ra totalPages đúng và các trường first/last")
    void getFiles_pagination_sizeOne_returnsCorrectTotalPages() throws Exception {
        createFile("f-pg-1", "File 1", "p1.svg", ngoaiThat, 2024, "ACTIVE");
        createFile("f-pg-2", "File 2", "p2.svg", ngoaiThat, 2024, "ACTIVE");
        createFile("f-pg-3", "File 3", "p3.svg", ngoaiThat, 2024, "ACTIVE");

        mockMvc.perform(get("/api/v1/files")
                        .param("q", "File ")
                        .param("page", "0")
                        .param("size", "1")
                        .with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(1)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(3)))
                .andExpect(jsonPath("$.first", is(true)))
                .andExpect(jsonPath("$.last", is(false)))
                .andExpect(jsonPath("$.content", hasSize(1)));

        // Trang cuối: page=2
        mockMvc.perform(get("/api/v1/files")
                        .param("q", "File ")
                        .param("page", "2")
                        .param("size", "1")
                        .with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(2)))
                .andExpect(jsonPath("$.first", is(false)))
                .andExpect(jsonPath("$.last", is(true)))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    // ---------- AC 6: vehiclePath đúng với file gắn model, subtype; null với file chưa gắn ----------

    @Test
    @DisplayName("AC: vehiclePath đúng với file gắn model và subtype; null với file chưa gắn")
    void getFiles_vehiclePath_correctForModelSubtypeAndNullForUnlinked() throws Exception {
        SvgFile fModel = createFile("f-vp-model", "Model VP File", "vp_m.svg", ngoaiThat, 2024, "ACTIVE");
        link(fModel, MODEL_CAMRY_25Q);

        SvgFile fSub = createFile("f-vp-sub", "Subtype VP File", "vp_s.svg", ngoaiThat, 2024, "ACTIVE");
        link(fSub, SUBTYPE_CAMRY_VN);

        createFile("f-vp-unlinked", "Unlinked VP File", "vp_u.svg", ngoaiThat, 2024, "ACTIVE");

        mockMvc.perform(get("/api/v1/files").param("q", "vp").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[?(@.id == 'f-vp-model')].vehiclePath",
                        contains("Toyota › Camry › Camry 2.5Q")))
                .andExpect(jsonPath("$.content[?(@.id == 'f-vp-sub')].vehiclePath",
                        contains("Toyota › Camry › Camry 2.5Q › Bản lắp ráp VN")))
                .andExpect(jsonPath("$.content[?(@.id == 'f-vp-unlinked')].vehiclePath",
                        contains((String) null)));
    }

    // ---------- Requirement 3: /catalog/year cho phép thiếu modelId ----------

    @Test
    @DisplayName("catalog/year: cho phép thiếu modelId (lọc theo brandId hoặc categoryId)")
    void catalogYear_missingModelId_returnsYearsFromPresentFilters() throws Exception {
        SvgFile f1 = createFile("f-cy-1", "CY1", "cy1.svg", ngoaiThat, 2024, "ACTIVE");
        link(f1, MODEL_CAMRY_25Q);

        SvgFile f2 = createFile("f-cy-2", "CY2", "cy2.svg", ngoaiThat, 2021, "ACTIVE");
        link(f2, SUBTYPE_CAMRY_VN);

        SvgFile f3 = createFile("f-cy-3", "CY3", "cy3.svg", ngoaiThat, 2018, "ACTIVE");
        link(f3, MODEL_VF8_PLUS); // thuộc VinFast

        // Lọc theo brandId Toyota: chỉ ra 2024 và 2021
        mockMvc.perform(get("/api/v1/catalog/year")
                        .param("brandId", String.valueOf(BRAND_TOYOTA))
                        .with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].value", is("2024")))
                .andExpect(jsonPath("$[1].value", is("2021")));

        // Không truyền modelId hay brandId: ra đủ cả 2024, 2021, 2018 giảm dần
        mockMvc.perform(get("/api/v1/catalog/year")
                        .param("categoryId", String.valueOf(CATEGORY_NGOAI_THAT))
                        .with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].value", is("2024")))
                .andExpect(jsonPath("$[1].value", is("2021")))
                .andExpect(jsonPath("$[2].value", is("2018")));
    }
}
