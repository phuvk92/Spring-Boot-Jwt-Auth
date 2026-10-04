package com.example.svgmanager.integration;

import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.UserDevice;
import com.example.svgmanager.repository.UserDeviceRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.repository.WorkDesignRepository;
import com.example.svgmanager.repository.WorkDesignVersionRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test tích hợp cho F-36 Lưu bản làm việc trên PostgreSQL THẬT:
 * - Flyway migration V22 tạo cột payload TEXT và payload_size INT
 * - POST tạo -> PUT sửa 2 lần -> versions có 3 bản đúng thứ tự
 * - GET content mặc định ra bản 3, ?version=1 ra bản 1
 * - User khác PUT / GET content -> 404
 * - Quá 20 MB -> 413 DESIGN_TOO_LARGE
 * - PUT không đổi sourceTemplateId
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://mock-keycloak/realms/cutting/protocol/openid-connect/certs",
        "app.keycloak.sync-enabled=false",
        "app.device.enforce=false",
        "app.file.storage-path=target/test-svg-storage-saved-designs"
})
@AutoConfigureMockMvc
@Transactional
class SavedDesignsPostgresIntegrationTest {

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
    private UserRepository userRepository;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @Autowired
    private WorkDesignRepository workDesignRepository;

    @Autowired
    private WorkDesignVersionRepository versionRepository;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        userA = userRepository.save(User.builder().username("pg_sd_user_a").email("pg_a@pcut.vn")
                .keycloakUserId("kc-pg-sd-a").role(Role.USER).enabled(true).build());
        userB = userRepository.save(User.builder().username("pg_sd_user_b").email("pg_b@pcut.vn")
                .keycloakUserId("kc-pg-sd-b").role(Role.USER).enabled(true).build());

        userDeviceRepository.save(UserDevice.register(userA, "dev-pg-a", "Máy Xưởng A", "windows", "10.0.0.1", "sid-pg-a", now));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtAs(String keycloakSub, String username, String sid) {
        return jwt().jwt(j -> {
            j.subject(keycloakSub);
            j.claim("preferred_username", username);
            if (sid != null) {
                j.claim("sid", sid);
            }
        }).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    @DisplayName("Postgres AC: Tạo -> Sửa 2 lần -> 3 versions đúng thứ tự, content mặc định bản 3, version=1 ra bản 1, PUT không đổi sourceTemplateId")
    void createAndTwiceUpdateLifecycle() throws Exception {
        // 1. POST tạo bản làm việc lần đầu
        String createJson = """
                {
                    "name": "Bản làm việc VF8",
                    "category": "Ngoại thất",
                    "vehicleLabel": "VinFast VF8 · 2025",
                    "sourceTemplateId": "tpl-vf8-orig",
                    "payload": {
                        "cutArea": { "widthMm": 3000, "heightMm": 1500 },
                        "parts": [
                            { "partId": "p1", "name": "Capo", "yMm": 0, "heightMm": 1000 }
                        ]
                    }
                }
                """;

        var createResult = mockMvc.perform(post("/api/v1/designs")
                        .with(jwtAs("kc-pg-sd-a", "pg_sd_user_a", "sid-pg-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name", is("Bản làm việc VF8")))
                .andExpect(jsonPath("$.sourceTemplateId", is("tpl-vf8-orig")))
                .andExpect(jsonPath("$.versionCount", is(1)))
                .andExpect(jsonPath("$.lastSavedByDeviceName", is("Máy Xưởng A")))
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode rootNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseBody);
        String designId = rootNode.get("id").asText();

        // 2. PUT lần 1 (version 2)
        String update1Json = """
                {
                    "name": "Bản làm việc VF8 (sửa 1)",
                    "category": "Ngoại thất",
                    "vehicleLabel": "VinFast VF8 · 2025",
                    "sourceTemplateId": "tpl-other-ignored",
                    "payload": {
                        "cutArea": { "widthMm": 3000, "heightMm": 1500 },
                        "parts": [
                            { "partId": "p1", "name": "Capo", "yMm": 0, "heightMm": 1000 },
                            { "partId": "p2", "name": "Cản trước", "yMm": 1000, "heightMm": 800 }
                        ]
                    }
                }
                """;

        mockMvc.perform(put("/api/v1/designs/" + designId)
                        .with(jwtAs("kc-pg-sd-a", "pg_sd_user_a", "sid-pg-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update1Json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(designId)))
                .andExpect(jsonPath("$.name", is("Bản làm việc VF8 (sửa 1)")))
                .andExpect(jsonPath("$.sourceTemplateId", is("tpl-vf8-orig"))) // Không đổi
                .andExpect(jsonPath("$.partCount", is(2)))
                .andExpect(jsonPath("$.versionCount", is(2)));

        // 3. PUT lần 2 (version 3)
        String update2Json = """
                {
                    "name": "Bản làm việc VF8 (sửa 2)",
                    "category": "Ngoại thất",
                    "vehicleLabel": "VinFast VF8 · 2025",
                    "payload": {
                        "cutArea": { "widthMm": 3000, "heightMm": 1500 },
                        "parts": [
                            { "partId": "p1", "name": "Capo", "yMm": 0, "heightMm": 1000 },
                            { "partId": "p2", "name": "Cản trước", "yMm": 1000, "heightMm": 800 },
                            { "partId": "p3", "name": "Tai xe", "yMm": 1800, "heightMm": 600 }
                        ]
                    }
                }
                """;

        mockMvc.perform(put("/api/v1/designs/" + designId)
                        .with(jwtAs("kc-pg-sd-a", "pg_sd_user_a", "sid-pg-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update2Json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(designId)))
                .andExpect(jsonPath("$.name", is("Bản làm việc VF8 (sửa 2)")))
                .andExpect(jsonPath("$.partCount", is(3)))
                .andExpect(jsonPath("$.versionCount", is(3)));

        // 4. Kiểm tra danh sách versions: có đúng 3 bản, mới nhất trước
        mockMvc.perform(get("/api/v1/designs/" + designId + "/versions")
                        .with(jwtAs("kc-pg-sd-a", "pg_sd_user_a", "sid-pg-a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].number", is(3)))
                .andExpect(jsonPath("$[0].isCurrent", is(true)))
                .andExpect(jsonPath("$[1].number", is(2)))
                .andExpect(jsonPath("$[1].isCurrent", is(false)))
                .andExpect(jsonPath("$[2].number", is(1)))
                .andExpect(jsonPath("$[2].isCurrent", is(false)));

        // 5. GET content mặc định: ra bản 3 (có 3 part)
        mockMvc.perform(get("/api/v1/designs/" + designId + "/content")
                        .with(jwtAs("kc-pg-sd-a", "pg_sd_user_a", "sid-pg-a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(designId)))
                .andExpect(jsonPath("$.version", is(3)))
                .andExpect(jsonPath("$.payload.parts", hasSize(3)));

        // 6. GET content ?version=1: ra bản 1 (có 1 part)
        mockMvc.perform(get("/api/v1/designs/" + designId + "/content?version=1")
                        .with(jwtAs("kc-pg-sd-a", "pg_sd_user_a", "sid-pg-a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(designId)))
                .andExpect(jsonPath("$.version", is(1)))
                .andExpect(jsonPath("$.payload.parts", hasSize(1)));

        // 7. User khác PUT hoặc GET content -> 404 DESIGN_NOT_FOUND (không lộ)
        mockMvc.perform(put("/api/v1/designs/" + designId)
                        .with(jwtAs("kc-pg-sd-b", "pg_sd_user_b", "sid-pg-b"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update2Json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("DESIGN_NOT_FOUND")));

        mockMvc.perform(get("/api/v1/designs/" + designId + "/content")
                        .with(jwtAs("kc-pg-sd-b", "pg_sd_user_b", "sid-pg-b")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("DESIGN_NOT_FOUND")));
    }

    @Test
    @DisplayName("Postgres AC: Quá 20 MB -> 413 DESIGN_TOO_LARGE")
    void payloadTooLarge_Postgres() throws Exception {
        String large = "x".repeat(20 * 1024 * 1024 + 10);
        String json = "{\"name\":\"Bản siêu nặng\",\"payload\":\"" + large + "\"}";

        mockMvc.perform(post("/api/v1/designs")
                        .with(jwtAs("kc-pg-sd-a", "pg_sd_user_a", "sid-pg-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code", is("DESIGN_TOO_LARGE")));
    }
}
