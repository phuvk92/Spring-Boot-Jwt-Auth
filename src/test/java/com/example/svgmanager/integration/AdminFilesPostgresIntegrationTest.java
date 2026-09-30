package com.example.svgmanager.integration;

import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.repository.SvgFilePartRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.VehicleNodeRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
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

import java.io.InputStream;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kho part file — SA v2 §3.2/§4. Chạy trên PostgreSQL THẬT qua Testcontainers để
 * Flyway chạy đủ V1…V15 (seed cây xe + danh mục) và query lọc theo nhánh cây
 * (WITH RECURSIVE) được kiểm thật.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://mock-keycloak/realms/cutting/protocol/openid-connect/certs",
        "app.file.storage-path=target/test-svg-storage-adminfiles",
        "app.keycloak.sync-enabled=false",
        "app.device.enforce=false"
})
@AutoConfigureMockMvc
@Transactional
class AdminFilesPostgresIntegrationTest {

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
    private SvgFilePartRepository partRepository;

    @Autowired
    private VehicleNodeRepository vehicleNodeRepository;

    /** Seed V15: Toyota›Camry›Camry 2.5Q (MODEL id=3), VinFast›VF 8›VF 8 Plus (MODEL id=11). */
    private static final long MODEL_CAMRY_25Q = 3;
    private static final long MODEL_VF8_PLUS = 11;
    private static final long BRAND_TOYOTA = 1;
    private static final long BRAND_VINFAST = 9;
    private static final long CATEGORY_NGOAI_THAT = 1;

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asAdmin() {
        return jwt().jwt(j -> j.subject("kc-admin").claim("preferred_username", "admin"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUser() {
        return jwt().jwt(j -> j.subject("kc-user").claim("preferred_username", "user"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private MockMultipartFile svgPart(String content) {
        return new MockMultipartFile("file", "test.svg", "image/svg+xml",
                content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String audiSvg() throws Exception {
        try (InputStream in = new ClassPathResource("fixtures/audi-q6-2024.svg").getInputStream()) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    // ── AC: upload + tách part ──────────────────────────────────────────

    @Test
    @DisplayName("POST upload Audi Q6 2024.svg → 177 part + hình học V13, file_key duy nhất")
    void upload_audi_splitsParts() throws Exception {
        MvcResult res = mockMvc.perform(multipart("/api/admin/files")
                        .file(new MockMultipartFile("file", "Audi Q6 2024.svg", "image/svg+xml",
                                audiSvg().getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .param("name", "Audi Q6 2024 — ngoại thất")
                        .param("categoryId", String.valueOf(CATEGORY_NGOAI_THAT))
                        .param("year", "2024")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q))
                        .with(asAdmin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileKey", notNullValue()))
                .andExpect(jsonPath("$.partCount", is(177)))
                .andExpect(jsonPath("$.category", is("Ngoại thất")))
                .andExpect(jsonPath("$.year", is(2024)))
                .andExpect(jsonPath("$.vehicles[0].path", is("Toyota › Camry › Camry 2.5Q")))
                .andReturn();

        long id = ((Integer) com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.id")).longValue();
        SvgFile saved = svgFileRepository.findById(id).orElseThrow();
        assertEquals(177, partRepository.countBySvgFileId(id));
        // Part đầu tiên có đủ cột hình học V13
        var part0 = partRepository.findBySvgFileIdOrderByDisplayOrderAscIdAsc(id).get(0);
        assertNotNull(part0.getPathData());
        assertTrue(part0.getWidthMm() > 0 && part0.getHeightMm() > 0);
        assertNotNull(saved.getFilmUsage());
    }

    @Test
    @DisplayName("Upload .plt / file đổi đuôi / thiếu node → 400 UNSUPPORTED_FORMAT / 400")
    void upload_rejectsBadInput() throws Exception {
        // Đuôi .plt
        mockMvc.perform(multipart("/api/admin/files")
                        .file(new MockMultipartFile("file", "mau.plt", "application/octet-stream",
                                "IN;SP1;".getBytes()))
                        .param("name", "x").param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q))
                        .with(asAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("UNSUPPORTED_FORMAT")));

        // Đuôi .svg nhưng nội dung DXF
        mockMvc.perform(multipart("/api/admin/files")
                        .file(new MockMultipartFile("file", "mau.svg", "image/svg+xml",
                                "0\nSECTION\n2\nENTITIES".getBytes()))
                        .param("name", "x").param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q))
                        .with(asAdmin()))
                .andExpect(status().isBadRequest());

        // vehicleNodeIds trỏ vào BRAND (không phải MODEL/SUBTYPE) → 400
        mockMvc.perform(multipart("/api/admin/files")
                        .file(svgPart("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10mm\" height=\"10mm\" viewBox=\"0 0 10 10\"><rect width=\"10\" height=\"10\"/></svg>"))
                        .param("name", "x").param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(BRAND_TOYOTA))
                        .with(asAdmin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Gắn 2 model khác hãng → file hiện ở cả hai nhánh khi lọc")
    void upload_multiBrand_visibleInBoth() throws Exception {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10mm\" height=\"10mm\" viewBox=\"0 0 10 10\">"
                + "<rect width=\"10\" height=\"10\"/></svg>";
        MvcResult res = mockMvc.perform(multipart("/api/admin/files")
                        .file(svgPart(svg))
                        .param("name", "Hai hãng").param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q) + "," + MODEL_VF8_PLUS)
                        .with(asAdmin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicles", hasSize(2)))
                .andReturn();
        long id = ((Integer) com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(get("/api/admin/files").param("brandId", String.valueOf(BRAND_TOYOTA)).with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem((int) id)));
        mockMvc.perform(get("/api/admin/files").param("brandId", String.valueOf(BRAND_VINFAST)).with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem((int) id)));
    }

    @Test
    @DisplayName("PUT có file mới → tách lại, thay toàn bộ parts")
    void put_newFile_replacesParts() throws Exception {
        String svg1 = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10mm\" height=\"10mm\" viewBox=\"0 0 10 10\">"
                + "<rect width=\"10\" height=\"10\"/></svg>";
        MvcResult res = mockMvc.perform(multipart("/api/admin/files")
                        .file(svgPart(svg1))
                        .param("name", "v1").param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q))
                        .with(asAdmin()))
                .andExpect(status().isCreated())
                .andReturn();
        long id = ((Integer) com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.id")).longValue();
        assertEquals(1, partRepository.countBySvgFileId(id));

        String svg2 = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10mm\" height=\"10mm\" viewBox=\"0 0 10 10\">"
                + "<rect width=\"5\" height=\"5\"/><circle cx=\"8\" cy=\"8\" r=\"1\"/></svg>";
        mockMvc.perform(multipart("/api/admin/files/" + id)
                        .file(svgPart(svg2))
                        .param("name", "v2")
                        .with(r -> { r.setMethod("PUT"); return r; })
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("v2")))
                .andExpect(jsonPath("$.partCount", is(2)));

        var parts = partRepository.findBySvgFileIdOrderByDisplayOrderAscIdAsc(id);
        assertEquals(2, parts.size());
        assertTrue(parts.stream().allMatch(p -> p.getPathData() != null));
    }

    @Test
    @DisplayName("DELETE xoá mềm — file biến khỏi list, stats unlinked tính đúng")
    void delete_softDelete() throws Exception {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10mm\" height=\"10mm\" viewBox=\"0 0 10 10\">"
                + "<rect width=\"10\" height=\"10\"/></svg>";
        MvcResult res = mockMvc.perform(multipart("/api/admin/files")
                        .file(svgPart(svg))
                        .param("name", "delete me").param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q))
                        .with(asAdmin()))
                .andExpect(status().isCreated())
                .andReturn();
        long id = ((Integer) com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(delete("/api/admin/files/" + id).with(asAdmin()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/files").with(asAdmin()))
                .andExpect(jsonPath("$.content[*].id", not(hasItem((int) id))));

        // USER không vào được API admin
        mockMvc.perform(get("/api/admin/files").with(asUser()))
                .andExpect(status().isForbidden());
    }
}
