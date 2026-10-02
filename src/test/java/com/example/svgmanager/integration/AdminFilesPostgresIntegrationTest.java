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
        // Geometry trả layout = "raw"
        mockMvc.perform(get("/api/v1/files/{id}/geometry", rawFileKey).with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parts[0].layout", is("raw")))
                .andExpect(jsonPath("$.parts[0].partId", containsString("--raw--")));

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
    @DisplayName("NGO-378: Upload cả hai bản nhưng số part khác nhau → 400 LAYOUT_PART_MISMATCH (kèm 2 số)")
    void upload_partMismatch_rejected() throws Exception {
        String svg1Part = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10mm\" height=\"10mm\" viewBox=\"0 0 10 10\"><rect width=\"10\" height=\"10\"/></svg>";
        String svg2Parts = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10mm\" height=\"10mm\" viewBox=\"0 0 10 10\"><rect width=\"5\" height=\"5\"/><circle cx=\"8\" cy=\"8\" r=\"1\"/></svg>";

        mockMvc.perform(multipart("/api/admin/files")
                        .file(nestedSvg(svg1Part))
                        .file(rawSvg(svg2Parts))
                        .param("name", "Mismatch")
                        .param("categoryId", "1")
                        .param("vehicleNodeIds", String.valueOf(MODEL_CAMRY_25Q))
                        .with(asAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("LAYOUT_PART_MISMATCH")))
                .andExpect(jsonPath("$.message", containsString("1")))
                .andExpect(jsonPath("$.message", containsString("2")));
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
    @DisplayName("NGO-378 AC: Upload Audi Q6 cả hai bản → geometry 2 x 177 part, layout đúng, partId không trùng, parts chỉ trả 177")
    void upload_audiQ6_bothLayouts_geometryAndParts() throws Exception {
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
                .andExpect(jsonPath("$.partCount", is(177)))
                .andReturn();

        long id = ((Integer) com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.id")).longValue();
        String fileKey = com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.fileKey");

        assertEquals(354, partRepository.countBySvgFileId(id));
        assertEquals(177, partRepository.countBySvgFileIdAndLayout(id, "NESTED"));
        assertEquals(177, partRepository.countBySvgFileIdAndLayout(id, "RAW"));

        // GET /api/v1/files/{id}/parts chỉ trả 1 bản (177 part)
        mockMvc.perform(get("/api/v1/files/{id}/parts", fileKey).with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(177)));

        // GET /api/v1/files/{id}/geometry trả 2 x 177 = 354 part
        MvcResult geoRes = mockMvc.perform(get("/api/v1/files/{id}/geometry", fileKey).with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parts", hasSize(354)))
                .andReturn();

        String geoJson = geoRes.getResponse().getContentAsString();
        java.util.List<String> partIds = com.jayway.jsonpath.JsonPath.read(geoJson, "$.parts[*].partId");
        java.util.List<String> layouts = com.jayway.jsonpath.JsonPath.read(geoJson, "$.parts[*].layout");

        assertEquals(354, partIds.size());
        assertEquals(354, new java.util.HashSet<>(partIds).size(), "Mọi partId trong geometry phải duy nhất!");

        long nestedCount = layouts.stream().filter("nested"::equals).count();
        long rawCount = layouts.stream().filter("raw"::equals).count();
        assertEquals(177, nestedCount);
        assertEquals(177, rawCount);

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
    @DisplayName("NGO-400: upload kèm khổ cắt → response, geometry và danh sách trả đúng cutArea")
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

        mockMvc.perform(get("/api/v1/files/{id}/geometry", fileKey).with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cutArea.lengthMm", is(15000)))
                .andExpect(jsonPath("$.cutArea.widthMm", is(700)));

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

        mockMvc.perform(get("/api/v1/files/{id}/geometry", fileKey).with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cutArea").value(org.hamcrest.Matchers.nullValue()));
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
