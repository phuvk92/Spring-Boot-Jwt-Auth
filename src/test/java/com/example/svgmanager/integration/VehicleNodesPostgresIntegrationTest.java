package com.example.svgmanager.integration;

import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.SvgFileVehicleNode;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.VehicleNode;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.SvgFileVehicleNodeRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.repository.VehicleNodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import jakarta.persistence.EntityManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API quản trị cây xe 4 cấp — chạy trên PostgreSQL THẬT qua Testcontainers để
 * Flyway chạy đủ V1…V15 (test H2 tắt Flyway không bắt được lỗi migration).
 * ddl-auto=validate để bắt lệch giữa entity và schema do V15 tạo.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://mock-keycloak/realms/cutting/protocol/openid-connect/certs",
        "app.file.storage-path=target/test-svg-storage-pg",
        "app.keycloak.sync-enabled=false",
        "app.device.enforce=false"
})
@AutoConfigureMockMvc
@Transactional
class VehicleNodesPostgresIntegrationTest {

    // Mặc định tự dựng container Postgres; có IT_DB_URL thì dùng DB ngoài
    // (máy dev có Docker Desktop socket không tương thích docker-java).
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

    @org.junit.jupiter.api.AfterAll
    static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleNodeRepository vehicleNodeRepository;

    @Autowired
    private SvgFileRepository svgFileRepository;

    @Autowired
    private SvgFileVehicleNodeRepository linkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asAdmin() {
        return jwt().jwt(j -> j.subject("kc-admin").claim("preferred_username", "admin"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUser() {
        return jwt().jwt(j -> j.subject("kc-user").claim("preferred_username", "user"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    @DisplayName("V15 chạy thật: seed 7 hãng gốc (6 hãng META0 + Abarth) và 4 danh mục file")
    void migration_seedData_present() throws Exception {
        mockMvc.perform(get("/api/vehicle-nodes").param("size", "50").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(7)))
                .andExpect(jsonPath("$.content[*].name",
                        hasItems("Toyota", "VinFast", "Ford", "Hyundai", "Mazda", "Kia", "Abarth")))
                // Toyota seed kèm cây con đủ 4 cấp
                .andExpect(jsonPath("$.content[0].name", is("Toyota")))
                .andExpect(jsonPath("$.content[0].children[0].name", is("Camry")))
                .andExpect(jsonPath("$.content[0].children[0].children[0].name", is("Camry 2.5Q")))
                .andExpect(jsonPath("$.content[0].children[0].children[0].children[0].level", is("SUBTYPE")));
    }

    @Test
    @DisplayName("Thêm đủ 4 cấp: hãng → dòng → model → phiên bản")
    void create_fullFourLevels() throws Exception {
        String brand = mockMvc.perform(post("/api/vehicle-nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Porsche\"}")
                        .with(asAdmin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.level", is("BRAND")))
                .andReturn().getResponse().getContentAsString();
        long brandId = ((Integer) com.jayway.jsonpath.JsonPath.read(brand, "$.id")).longValue();

        String series = postNode(brandId, "911")
                .andExpect(jsonPath("$.level", is("SERIES")))
                .andReturn().getResponse().getContentAsString();
        long seriesId = ((Integer) com.jayway.jsonpath.JsonPath.read(series, "$.id")).longValue();

        String model = postNode(seriesId, "911 Carrera")
                .andExpect(jsonPath("$.level", is("MODEL")))
                .andReturn().getResponse().getContentAsString();
        long modelId = ((Integer) com.jayway.jsonpath.JsonPath.read(model, "$.id")).longValue();

        postNode(modelId, "Bản nhập Đức")
                .andExpect(jsonPath("$.level", is("SUBTYPE")));
    }

    @Test
    @DisplayName("SUBTYPE là cấp cuối — tạo con dưới SUBTYPE → 400")
    void create_childUnderSubtype_returns400() throws Exception {
        VehicleNode subtype = vehicleNodeRepository.findAll().stream()
                .filter(n -> n.getLevel().name().equals("SUBTYPE"))
                .findFirst().orElseThrow();

        postNodeRaw(subtype.getId(), "Không được")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Trùng tên cùng cha → 409 NODE_NAME_TAKEN; tên trùng ở cha khác vẫn được")
    void create_duplicateName_returns409() throws Exception {
        VehicleNode toyota = rootByName("Toyota");
        // Seed đã có 'Camry' dưới Toyota
        postNodeRaw(toyota.getId(), "camry")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("NODE_NAME_TAKEN")));

        // Cùng tên 'Camry' nhưng dưới hãng khác thì hợp lệ
        VehicleNode kia = rootByName("Kia");
        postNode(kia.getId(), "Camry")
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Tìm 'camry' → trả hãng Toyota với cây tỉa chỉ còn nhánh Camry")
    void search_camry_returnsToyotaBranchOnly() throws Exception {
        mockMvc.perform(get("/api/vehicle-nodes").param("q", "camry").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Toyota")))
                // Chỉ còn nhánh Camry, Vios bị tỉa
                .andExpect(jsonPath("$.content[0].children", hasSize(1)))
                .andExpect(jsonPath("$.content[0].children[0].name", is("Camry")))
                .andExpect(jsonPath("$.content[0].children[0].children[*].name",
                        hasItems("Camry 2.5Q", "Camry 2.0G")));
    }

    @Test
    @DisplayName("Đổi tên node (PUT) — chỉ đổi tên, không đổi cấp/cha")
    void rename_node_ok() throws Exception {
        VehicleNode vios = vehicleNodeRepository.findAll().stream()
                .filter(n -> n.getName().equals("Vios"))
                .findFirst().orElseThrow();

        mockMvc.perform(put("/api/vehicle-nodes/" + vios.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Vios 2025\"}")
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Vios 2025")))
                .andExpect(jsonPath("$.level", is("SERIES")));

        // Trùng tên cùng cha khi đổi tên → 409
        mockMvc.perform(put("/api/vehicle-nodes/" + vios.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Camry\"}")
                        .with(asAdmin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("NODE_NAME_TAKEN")));
    }

    @Test
    @DisplayName("Xoá SERIES: mất cả nhánh, file gắn vào nhánh còn nguyên nhưng không còn liên kết (Q4)")
    void delete_series_cascadesButKeepsFiles() throws Exception {
        VehicleNode camry = vehicleNodeRepository.findAll().stream()
                .filter(n -> n.getName().equals("Camry"))
                .findFirst().orElseThrow();
        VehicleNode model = camry.getChildren().stream()
                .filter(n -> n.getName().equals("Camry 2.5Q"))
                .findFirst().orElseThrow();
        VehicleNode subtype = model.getChildren().get(0);

        // File gắn vào SUBTYPE trong nhánh sắp xoá
        User uploader = userRepository.save(User.builder()
                .username("content-pg")
                .email("content-pg@test.local")
                .fullName("Content")
                .keycloakUserId("kc-content-pg")
                .password(passwordEncoder.encode("x"))
                .role(Role.ADMIN)
                .enabled(true)
                .deleted(false)
                .build());
        SvgFile file = svgFileRepository.save(SvgFile.builder()
                .originalFilename("camry-capo.svg")
                .storedFilename("uuid-camry-capo.svg")
                .filePath("/tmp/camry-capo.svg")
                .fileSize(512L)
                .contentType("image/svg+xml")
                .status("ACTIVE")
                .uploadedBy(uploader)
                .build());
        linkRepository.save(new SvgFileVehicleNode(file, subtype));

        // impact trước khi xoá: 2 MODEL + 2 SUBTYPE con, 1 file mất liên kết
        mockMvc.perform(get("/api/vehicle-nodes/" + camry.getId() + "/impact").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes", is(4)))
                .andExpect(jsonPath("$.files", is(1)));

        mockMvc.perform(delete("/api/vehicle-nodes/" + camry.getId()).with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedNodes", is(5)))
                .andExpect(jsonPath("$.unlinkedFiles", is(1)));

        // Cả nhánh mất — Toyota chỉ còn Vios (clear context: cascade FK xoá ở DB, không qua entity)
        entityManager.flush();
        entityManager.clear();
        assertTrue(vehicleNodeRepository.findById(camry.getId()).isEmpty());
        assertTrue(vehicleNodeRepository.findById(model.getId()).isEmpty());
        // File còn nguyên nhưng hết liên kết
        assertTrue(svgFileRepository.findById(file.getId()).isPresent());
        assertTrue(linkRepository.findBySvgFileId(file.getId()).isEmpty());
    }

    @Test
    @DisplayName("Không phải ADMIN → 403; không có phiên → 401")
    void vehicleNodes_requiresAdmin() throws Exception {
        mockMvc.perform(get("/api/vehicle-nodes").with(asUser()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/vehicle-nodes"))
                .andExpect(status().isUnauthorized());
    }

    private VehicleNode rootByName(String name) {
        return vehicleNodeRepository.findByParentIsNullOrderByDisplayOrderAscIdAsc().stream()
                .filter(n -> n.getName().equals(name))
                .findFirst().orElseThrow();
    }

    private org.springframework.test.web.servlet.ResultActions postNode(Long parentId, String name) throws Exception {
        return postNodeRaw(parentId, name).andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions postNodeRaw(Long parentId, String name) throws Exception {
        String body = parentId == null
                ? "{\"name\":\"" + name + "\"}"
                : "{\"parentId\":" + parentId + ",\"name\":\"" + name + "\"}";
        return mockMvc.perform(post("/api/vehicle-nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(asAdmin()));
    }
}
