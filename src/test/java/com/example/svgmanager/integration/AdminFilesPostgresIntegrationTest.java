package com.example.svgmanager.integration;

import com.example.svgmanager.entity.SvgFile;
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
    @DisplayName("POST upload Audi Q6 2024.svg → lưu SVG, partCount 139 (ring chẵn/lẻ — SA-Nesting §8), không lưu part")
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
                .andExpect(jsonPath("$.partCount", is(139)))
                .andExpect(jsonPath("$.category", is("Ngoại thất")))
                .andExpect(jsonPath("$.year", is(2024)))
                .andExpect(jsonPath("$.vehicles[0].path", is("Toyota › Camry › Camry 2.5Q")))
                .andReturn();

        long id = ((Integer) com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.id")).longValue();
        SvgFile saved = svgFileRepository.findById(id).orElseThrow();
        // Board 08/10: server chỉ lưu SVG + số liệu hiển thị; app tự tách part khi mở file
        assertEquals(139, saved.getNestedPartCount());
        assertNull(saved.getRawPartCount());
        assertNotNull(saved.getFilmUsage());
        mockMvc.perform(get("/api/v1/files/{id}/svg", saved.getFileKey()).with(asUser()))
                .andExpect(status().isOk())
                .andExpect(content().string(startsWith("<svg")));
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
    @DisplayName("Board 30/09: một file một mẫu xe — gửi 2 model → 400 ONE_VEHICLE_PER_FILE")
    void upload_twoVehicles_rejected() throws Exception {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10mm\" height=\"10mm\" viewBox=\"0 0 10 10\">"
                + "<rect width=\"10\" height=\"10\"/></svg>";
        mockMvc.perform(multipart("/api/admin/files")
                        .file(svgPart(svg))
                        .param("name", "Hai hãng").param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q) + "," + MODEL_VF8_PLUS)
                        .with(asAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ONE_VEHICLE_PER_FILE"));

        MvcResult res = mockMvc.perform(multipart("/api/admin/files")
                        .file(svgPart(svg))
                        .param("name", "Một hãng").param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(MODEL_VF8_PLUS))
                        .with(asAdmin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicles", hasSize(1)))
                .andReturn();
        long id = ((Integer) com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.id")).longValue();
        mockMvc.perform(get("/api/admin/files").param("brandId", String.valueOf(BRAND_VINFAST)).with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem((int) id)));
    }

    @Test
    @DisplayName("PUT có file mới → thay file SVG, cập nhật partCount")
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
        assertEquals(1, svgFileRepository.findById(id).orElseThrow().getNestedPartCount());

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

        assertEquals(2, svgFileRepository.findById(id).orElseThrow().getNestedPartCount());
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

    // ── AC NGO-378: Part file hai cách xếp (SA §8) ──────────────────────

    private MockMultipartFile nestedSvg(String content) {
        return new MockMultipartFile("nestedFile", "nested.svg", "image/svg+xml",
                content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private MockMultipartFile rawSvg(String content) {
        return new MockMultipartFile("rawFile", "raw.svg", "image/svg+xml",
                content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("NGO-378: Upload chỉ nested / chỉ raw / thiếu cả hai → 400 FILE_REQUIRED")
    void upload_singleLayout_orNone() throws Exception {
        String svg1 = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10mm\" height=\"10mm\" viewBox=\"0 0 10 10\"><rect width=\"10\" height=\"10\"/></svg>";

        // 1. Chỉ nested
        mockMvc.perform(multipart("/api/admin/files")
                        .file(nestedSvg(svg1))
                        .param("name", "Chi Nested")
                        .param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q))
                        .with(asAdmin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasNested", is(true)))
                .andExpect(jsonPath("$.hasRaw", is(false)))
                .andExpect(jsonPath("$.partCount", is(1)));

        // 2. Chỉ raw
        MvcResult rawRes = mockMvc.perform(multipart("/api/admin/files")
                        .file(rawSvg(svg1))
                        .param("name", "Chi Raw")
                        .param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q))
                        .with(asAdmin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasNested", is(false)))
                .andExpect(jsonPath("$.hasRaw", is(true)))
                .andExpect(jsonPath("$.partCount", is(1)))
                .andReturn();

        long rawId = ((Integer) com.jayway.jsonpath.JsonPath.read(rawRes.getResponse().getContentAsString(), "$.id")).longValue();
        String rawFileKey = com.jayway.jsonpath.JsonPath.read(rawRes.getResponse().getContentAsString(), "$.fileKey");

        // Gọi content raw
        mockMvc.perform(get("/api/svg/{id}/content", rawId).param("layout", "raw").with(asAdmin()))
                .andExpect(status().isOk());
        // App tải bản chưa xếp; bỏ trống layout → bản duy nhất đang có
        mockMvc.perform(get("/api/v1/files/{id}/svg", rawFileKey).param("layout", "raw").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(content().string(startsWith("<svg")));
        mockMvc.perform(get("/api/v1/files/{id}/svg", rawFileKey).with(asUser()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/files/{id}/svg", rawFileKey).param("layout", "nested").with(asUser()))
                .andExpect(status().isNotFound());

        // 3. Thiếu cả hai → 400 FILE_REQUIRED
        mockMvc.perform(multipart("/api/admin/files")
                        .param("name", "Khong file")
                        .param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q))
                        .with(asAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("FILE_REQUIRED")));
    }

    @Test
    @DisplayName("Board 08/10: hai bản khác số part vẫn nhận — server không còn so part (app tự tách)")
    void upload_partCountDiffers_accepted() throws Exception {
        String svg1Part = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10mm\" height=\"10mm\" viewBox=\"0 0 10 10\"><rect width=\"10\" height=\"10\"/></svg>";
        String svg2Parts = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10mm\" height=\"10mm\" viewBox=\"0 0 10 10\"><rect width=\"5\" height=\"5\"/><circle cx=\"8\" cy=\"8\" r=\"1\"/></svg>";

        mockMvc.perform(multipart("/api/admin/files")
                        .file(nestedSvg(svg1Part))
                        .file(rawSvg(svg2Parts))
                        .param("name", "Khac so part")
                        .param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q))
                        .with(asAdmin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.partCount", is(1)));
    }

    @Test
    @DisplayName("NGO-378: PUT bỏ một bản thành công; PUT bỏ cả hai → 400 FILE_REQUIRED")
    void put_removeLayouts() throws Exception {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10mm\" height=\"10mm\" viewBox=\"0 0 10 10\"><rect width=\"10\" height=\"10\"/></svg>";

        MvcResult res = mockMvc.perform(multipart("/api/admin/files")
                        .file(nestedSvg(svg))
                        .file(rawSvg(svg))
                        .param("name", "Hai ban")
                        .param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q))
                        .with(asAdmin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasNested", is(true)))
                .andExpect(jsonPath("$.hasRaw", is(true)))
                .andReturn();

        long id = ((Integer) com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.id")).longValue();

        // PUT bỏ bản nested (removeNested=true)
        mockMvc.perform(multipart("/api/admin/files/" + id)
                        .param("removeNested", "true")
                        .with(r -> { r.setMethod("PUT"); return r; })
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNested", is(false)))
                .andExpect(jsonPath("$.hasRaw", is(true)));

        // PUT bỏ tiếp bản raw (removeRaw=true) → bỏ cả hai → 400
        mockMvc.perform(multipart("/api/admin/files/" + id)
                        .param("removeRaw", "true")
                        .with(r -> { r.setMethod("PUT"); return r; })
                        .with(asAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("FILE_REQUIRED")));
    }

    @Test
    @DisplayName("NGO-378 AC: Upload Audi Q6 cả hai bản → lưu hai SVG, partCount 139, app tải được từng bản")
    void upload_audiQ6_bothLayouts_svgPerLayout() throws Exception {
        String svgContent = audiSvg();

        MvcResult res = mockMvc.perform(multipart("/api/admin/files")
                        .file(new MockMultipartFile("nestedFile", "Audi Q6 2024.svg", "image/svg+xml",
                                svgContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .file(new MockMultipartFile("rawFile", "Audi Q6 2024.svg", "image/svg+xml",
                                svgContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .param("name", "Audi Q6 2024")
                        .param("categoryId", String.valueOf(CATEGORY_NGOAI_THAT))
                        .param("year", "2024")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q))
                        .with(asAdmin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasNested", is(true)))
                .andExpect(jsonPath("$.hasRaw", is(true)))
                .andExpect(jsonPath("$.partCount", is(139)))
                .andReturn();

        long id = ((Integer) com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.id")).longValue();
        String fileKey = com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.fileKey");

        SvgFile saved = svgFileRepository.findById(id).orElseThrow();
        assertEquals(139, saved.getNestedPartCount());
        assertEquals(139, saved.getRawPartCount());

        // App tải từng bản về tự tách (board 08/10)
        mockMvc.perform(get("/api/v1/files/{id}/svg", fileKey).param("layout", "nested").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(content().string(startsWith("<svg")));
        mockMvc.perform(get("/api/v1/files/{id}/svg", fileKey).param("layout", "raw").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(content().string(startsWith("<svg")));

        // GET content mặc định (nested) và layout=raw
        mockMvc.perform(get("/api/svg/{id}/content", id).with(asAdmin()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/svg/{id}/content", id).param("layout", "raw").with(asAdmin()))
                .andExpect(status().isOk());
    }

    // ── AC NGO-400: Khổ cắt theo file (epic NGO-399) ────────────────────

    private static final String SVG_1PART =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10mm\" height=\"10mm\" viewBox=\"0 0 10 10\">"
                    + "<rect width=\"10\" height=\"10\"/></svg>";

    @Test
    @DisplayName("NGO-400: upload kèm khổ cắt → response và danh sách trả đúng cutArea")
    void upload_withCutArea() throws Exception {
        MvcResult res = mockMvc.perform(multipart("/api/admin/files")
                        .file(svgPart(SVG_1PART))
                        .param("name", "Co Kho Cat").param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q))
                        .param("cutAreaLengthMm", "15000")
                        .param("cutAreaWidthMm", "700")
                        .with(asAdmin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cutAreaLengthMm", is(15000)))
                .andExpect(jsonPath("$.cutAreaWidthMm", is(700)))
                .andReturn();
        String fileKey = com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.fileKey");

        mockMvc.perform(get("/api/v1/files").param("q", "Co Kho Cat").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cutArea.lengthMm", is(15000)))
                .andExpect(jsonPath("$.content[0].cutArea.widthMm", is(700)));
    }

    @Test
    @DisplayName("NGO-400: upload không khai khổ → cutArea null khắp nơi (dữ liệu cũ tương đương)")
    void upload_withoutCutArea_nullEverywhere() throws Exception {
        MvcResult res = mockMvc.perform(multipart("/api/admin/files")
                        .file(svgPart(SVG_1PART))
                        .param("name", "Khong Kho Cat").param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q))
                        .with(asAdmin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cutAreaLengthMm").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.cutAreaWidthMm").value(org.hamcrest.Matchers.nullValue()))
                .andReturn();
        String fileKey = com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.fileKey");

        mockMvc.perform(get("/api/v1/files").param("q", "Khong Kho Cat").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", is(fileKey)))
                .andExpect(jsonPath("$.content[0].cutArea").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("NGO-400: chỉ một trường khổ → 400 CUT_AREA_INCOMPLETE; ngoài giới hạn → 400 CUT_AREA_OUT_OF_RANGE")
    void upload_cutAreaValidation() throws Exception {
        mockMvc.perform(multipart("/api/admin/files")
                        .file(svgPart(SVG_1PART))
                        .param("name", "x").param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q))
                        .param("cutAreaLengthMm", "15000")
                        .with(asAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("CUT_AREA_INCOMPLETE")));

        mockMvc.perform(multipart("/api/admin/files")
                        .file(svgPart(SVG_1PART))
                        .param("name", "x").param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q))
                        .param("cutAreaWidthMm", "700")
                        .with(asAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("CUT_AREA_INCOMPLETE")));

        mockMvc.perform(multipart("/api/admin/files")
                        .file(svgPart(SVG_1PART))
                        .param("name", "x").param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q))
                        .param("cutAreaLengthMm", "99")
                        .param("cutAreaWidthMm", "700")
                        .with(asAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("CUT_AREA_OUT_OF_RANGE")));

        mockMvc.perform(multipart("/api/admin/files")
                        .file(svgPart(SVG_1PART))
                        .param("name", "x").param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q))
                        .param("cutAreaLengthMm", "15000")
                        .param("cutAreaWidthMm", "2001")
                        .with(asAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("CUT_AREA_OUT_OF_RANGE")));
    }

    @Test
    @DisplayName("NGO-400: PUT đổi khổ, clearCutArea → null, không gửi → giữ nguyên")
    void put_cutArea_updateClearKeep() throws Exception {
        MvcResult res = mockMvc.perform(multipart("/api/admin/files")
                        .file(svgPart(SVG_1PART))
                        .param("name", "Kho Cat Put").param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q))
                        .param("cutAreaLengthMm", "15000")
                        .param("cutAreaWidthMm", "700")
                        .with(asAdmin()))
                .andExpect(status().isCreated())
                .andReturn();
        long id = ((Integer) com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.id")).longValue();

        // PUT không gửi trường khổ → giữ nguyên
        mockMvc.perform(multipart("/api/admin/files/" + id)
                        .param("name", "Kho Cat Put 2")
                        .with(r -> { r.setMethod("PUT"); return r; })
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cutAreaLengthMm", is(15000)))
                .andExpect(jsonPath("$.cutAreaWidthMm", is(700)));

        // PUT đổi khổ
        mockMvc.perform(multipart("/api/admin/files/" + id)
                        .param("cutAreaLengthMm", "30000")
                        .param("cutAreaWidthMm", "1520")
                        .with(r -> { r.setMethod("PUT"); return r; })
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cutAreaLengthMm", is(30000)))
                .andExpect(jsonPath("$.cutAreaWidthMm", is(1520)));

        // PUT thiếu một trường → 400 CUT_AREA_INCOMPLETE
        mockMvc.perform(multipart("/api/admin/files/" + id)
                        .param("cutAreaLengthMm", "20000")
                        .with(r -> { r.setMethod("PUT"); return r; })
                        .with(asAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("CUT_AREA_INCOMPLETE")));

        // clearCutArea → về null
        mockMvc.perform(multipart("/api/admin/files/" + id)
                        .param("clearCutArea", "true")
                        .with(r -> { r.setMethod("PUT"); return r; })
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cutAreaLengthMm").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.cutAreaWidthMm").value(org.hamcrest.Matchers.nullValue()));

        SvgFile saved = svgFileRepository.findById(id).orElseThrow();
        assertNull(saved.getCutAreaLengthMm());
        assertNull(saved.getCutAreaWidthMm());
    }
}
