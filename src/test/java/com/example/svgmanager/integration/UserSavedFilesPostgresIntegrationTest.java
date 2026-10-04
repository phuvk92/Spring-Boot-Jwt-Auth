package com.example.svgmanager.integration;

import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.repository.UserSvgFileRepository;
import org.junit.jupiter.api.AfterAll;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kiểm thử tích hợp chạy trên PostgreSQL thật:
 * - Flyway migration V24 (source_file_key column + index)
 * - POST /api/internal/user-files tạo bản lưu có sourceFileKey
 * - PUT /api/internal/user-files/{id} lưu đè cùng id, cập nhật file_size, checksum, fileName, updated_at
 * - PUT của user khác -> 404
 * - GET /api/internal/user-files/{id} trả về sourceFileKey + sourceFileName
 * - GET /api/internal/user-files (list) không tăng dòng sau PUT
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://mock-keycloak/realms/cutting/protocol/openid-connect/certs",
        "app.file.storage-path=target/test-svg-storage-userfiles-postgres",
        "app.keycloak.sync-enabled=false",
        "app.device.enforce=false"
})
@AutoConfigureMockMvc
@Transactional
class UserSavedFilesPostgresIntegrationTest {

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
    private SvgFileRepository svgFileRepository;

    @Autowired
    private UserSvgFileRepository userSvgFileRepository;

    @Autowired
    private com.example.svgmanager.service.FileStorageService fileStorageService;

    private User userA;
    private User userB;
    private SvgFile catalogSvg;

    private static final String SVG_CONTENT_V1 = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\"><rect width=\"100\" height=\"100\" fill=\"red\"/></svg>";
    private static final String SVG_CONTENT_V2 = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"200\" height=\"200\"><circle cx=\"100\" cy=\"100\" r=\"50\" fill=\"blue\"/></svg>";

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUserA() {
        return jwt().jwt(j -> j.subject("kc-usera-pg-test").claim("preferred_username", "usera_pg_test"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUserB() {
        return jwt().jwt(j -> j.subject("kc-userb-pg-test").claim("preferred_username", "userb_pg_test"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @BeforeEach
    void setUp() {
        userA = userRepository.save(User.builder()
                .username("usera_pg_test")
                .email("usera_pg@test.local")
                .keycloakUserId("kc-usera-pg-test")
                .role(Role.USER)
                .enabled(true)
                .build());

        userB = userRepository.save(User.builder()
                .username("userb_pg_test")
                .email("userb_pg@test.local")
                .keycloakUserId("kc-userb-pg-test")
                .role(Role.USER)
                .enabled(true)
                .build());

        SvgFile file = SvgFile.builder()
                .originalFilename("Audi_Q6_2024.svg")
                .status("ACTIVE")
                .uploadedBy(userA)
                .build();
        file.setFileKey("audi-q6-2024-full");
        file.setDisplayName("Audi Q6 2024 Full Body");
        catalogSvg = svgFileRepository.save(file);
    }

    @Test
    @DisplayName("AC: POST -> PUT cùng id -> GET thấy nội dung + tên mới, id không đổi, list không thêm dòng, sourceFileKey đúng")
    void post_then_put_same_id_postgres() throws Exception {
        // 1. POST bản lưu mới
        MockMultipartFile fileV1 = new MockMultipartFile(
                "file",
                "audi_v1.svg",
                "image/svg+xml",
                SVG_CONTENT_V1.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult postResult = mockMvc.perform(multipart("/api/internal/user-files")
                        .file(fileV1)
                        .param("fileName", "Audi_Saved_V1.svg")
                        .param("sourceFileKey", "audi-q6-2024-full")
                        .param("description", "Phiên bản 1")
                        .param("filmWidth", "1500")
                        .param("rollLength", "3000")
                        .with(asUserA()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.fileName", is("Audi_Saved_V1.svg")))
                .andExpect(jsonPath("$.sourceFileKey", is("audi-q6-2024-full")))
                .andExpect(jsonPath("$.sourceFileName", is("Audi Q6 2024 Full Body")))
                .andReturn();

        String responseBody = postResult.getResponse().getContentAsString();
        // Trích id
        com.fasterxml.jackson.databind.JsonNode rootNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseBody);
        long savedId = rootNode.get("id").asLong();

        // Kiểm tra danh sách của User A có đúng 1 bản
        mockMvc.perform(get("/api/internal/user-files").with(asUserA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)));

        // 2. PUT lưu đè vào cùng id
        MockMultipartFile fileV2 = new MockMultipartFile(
                "file",
                "audi_v2.svg",
                "image/svg+xml",
                SVG_CONTENT_V2.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/internal/user-files/" + savedId)
                        .file(fileV2)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .param("fileName", "Audi_Saved_V2.svg")
                        .param("sourceFileKey", "audi-q6-2024-full")
                        .param("description", "Phiên bản 2 cập nhật")
                        .param("filmWidth", "1520")
                        .param("rollLength", "3500")
                        .with(asUserA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) savedId)))
                .andExpect(jsonPath("$.fileName", is("Audi_Saved_V2.svg")))
                .andExpect(jsonPath("$.sourceFileKey", is("audi-q6-2024-full")))
                .andExpect(jsonPath("$.sourceFileName", is("Audi Q6 2024 Full Body")))
                .andExpect(jsonPath("$.description", is("Phiên bản 2 cập nhật")));

        // 3. GET kiểm tra: id không đổi, tên mới, nội dung download là V2
        mockMvc.perform(get("/api/internal/user-files/" + savedId).with(asUserA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) savedId)))
                .andExpect(jsonPath("$.fileName", is("Audi_Saved_V2.svg")))
                .andExpect(jsonPath("$.sourceFileKey", is("audi-q6-2024-full")))
                .andExpect(jsonPath("$.sourceFileName", is("Audi Q6 2024 Full Body")));

        MvcResult downloadResult = mockMvc.perform(get("/api/internal/user-files/" + savedId + "/download").with(asUserA()))
                .andExpect(status().isOk())
                .andReturn();
        String downloadedSvg = downloadResult.getResponse().getContentAsString();
        assertThat(downloadedSvg).contains("circle");

        // 4. Danh sách của User A vẫn giữ nguyên 1 bản (không thêm dòng)
        mockMvc.perform(get("/api/internal/user-files").with(asUserA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].id", is((int) savedId)))
                .andExpect(jsonPath("$.content[0].fileName", is("Audi_Saved_V2.svg")));

        // 5. User B không thể PUT đè lên bản của User A -> 404
        mockMvc.perform(multipart("/api/internal/user-files/" + savedId)
                        .file(fileV2)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .param("fileName", "Hacked.svg")
                        .with(asUserB()))
                .andExpect(status().isNotFound());

        // 6. User B không thể DELETE bản của User A -> 404
        mockMvc.perform(delete("/api/internal/user-files/" + savedId).with(asUserB()))
                .andExpect(status().isNotFound());

        // 7. User A DELETE bản của mình -> 204
        mockMvc.perform(delete("/api/internal/user-files/" + savedId).with(asUserA()))
                .andExpect(status().isNoContent());

        // 8. Sau khi xoá: danh sách của User A không còn
        mockMvc.perform(get("/api/internal/user-files").with(asUserA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));

        // 9. GET /{id} -> 404
        mockMvc.perform(get("/api/internal/user-files/" + savedId).with(asUserA()))
                .andExpect(status().isNotFound());

        // 10. GET /{id}/download -> 404
        mockMvc.perform(get("/api/internal/user-files/" + savedId + "/download").with(asUserA()))
                .andExpect(status().isNotFound());

        // 11. PUT lên bản đã xoá -> 404
        mockMvc.perform(multipart("/api/internal/user-files/" + savedId)
                        .file(fileV2)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .param("fileName", "Revive.svg")
                        .with(asUserA()))
                .andExpect(status().isNotFound());

        // 12. Xoá lại lần 2 -> 404
        mockMvc.perform(delete("/api/internal/user-files/" + savedId).with(asUserA()))
                .andExpect(status().isNotFound());

        // 13. File vật lý trên đĩa vẫn giữ nguyên (xoá mềm)
        var entityInDb = userSvgFileRepository.findById(savedId).orElseThrow();
        assertThat(entityInDb.getStatus()).isEqualTo("DELETED");
        org.springframework.core.io.Resource physicalResource = fileStorageService.loadFileAsResource(entityInDb.getFilePath());
        assertThat(physicalResource.exists()).isTrue();
    }
}
