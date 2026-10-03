package com.example.svgmanager.integration;

import com.example.svgmanager.entity.CutJob;
import com.example.svgmanager.entity.CutOutcome;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.UserDevice;
import com.example.svgmanager.entity.UserSvgFile;
import com.example.svgmanager.entity.WorkDesign;
import com.example.svgmanager.repository.CutJobRepository;
import com.example.svgmanager.repository.UserDeviceRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.repository.UserSvgFileRepository;
import com.example.svgmanager.repository.WorkDesignRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AC kiểm thử trên PostgreSQL thật cho F-38 · NGO-428 · NGO-445:
 * - Flyway migration V14 chạy trên PostgreSQL
 * - Ghi rồi đọc thấy (POST -> GET /api/v1/cuts)
 * - RECUT khi cắt lại cùng designId
 * - designId của user khác bị bỏ liên kết
 * - body có trường hình học (pathData, svg, geometry) không được lưu
 * - cùng Idempotency-Key không ghi đôi
 * - máy bị gỡ → 401
 * - cutAt nhận ISO-8601 offset (+07:00), Z (UTC) quy về giờ VN, và không múi giờ
 * - designId nhận id user_svg_files của chính user (ưu tiên), trả lại trên GET /api/v1/cuts
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://mock-keycloak/realms/cutting/protocol/openid-connect/certs",
        "app.file.storage-path=target/test-svg-storage-cutpostgres",
        "app.keycloak.sync-enabled=false",
        "app.device.enforce=false"
})
@AutoConfigureMockMvc
@Transactional
class CutHistoryPostgresIntegrationTest {

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
    private CutJobRepository cutJobRepository;

    @Autowired
    private WorkDesignRepository workDesignRepository;

    @Autowired
    private UserSvgFileRepository userSvgFileRepository;

    private User userA;
    private User userB;
    private UserDevice deviceA;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(User.builder().username("pg_cut_user_a").email("pg_cut_a@t.vn")
                .keycloakUserId("kc-pg-cut-a").role(Role.USER).enabled(true).build());
        userB = userRepository.save(User.builder().username("pg_cut_user_b").email("pg_cut_b@t.vn")
                .keycloakUserId("kc-pg-cut-b").role(Role.USER).enabled(true).build());

        LocalDateTime now = LocalDateTime.now();
        deviceA = userDeviceRepository.save(UserDevice.register(userA, "pg-dev-a", "Máy Xưởng PG", "windows", "10.0.0.1", "sid-pg-A", now));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtAs(String keycloakSub, String username, String sid) {
        return jwt().jwt(j -> j.subject(keycloakSub)
                        .claim("preferred_username", username)
                        .claim("sid", sid))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    @DisplayName("Postgres: ghi rồi đọc thấy ngay trên GET /api/v1/cuts")
    void postgres_RecordAndRead() throws Exception {
        String json = """
                {
                    "cutAt": "2026-10-03T11:00:00",
                    "partLabel": "Đèn pha trái",
                    "vehicleLabel": "Mazda CX-5",
                    "filmUsage": "1,0 m",
                    "filmUsageMeters": 1.0,
                    "duration": "1′ 30″"
                }
                """;

        mockMvc.perform(post("/api/v1/cuts")
                        .with(jwtAs("kc-pg-cut-a", "pg_cut_user_a", "sid-pg-A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.partLabel", is("Đèn pha trái")))
                .andExpect(jsonPath("$.outcome", is("completed")));

        mockMvc.perform(get("/api/v1/cuts")
                        .with(jwtAs("kc-pg-cut-a", "pg_cut_user_a", "sid-pg-A"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs", hasSize(1)))
                .andExpect(jsonPath("$.jobs[0].partLabel", is("Đèn pha trái")))
                .andExpect(jsonPath("$.stats.jobCount", is(1)))
                .andExpect(jsonPath("$.stats.filmUsed", is("1 m")));
    }

    @Test
    @DisplayName("Postgres: RECUT khi cắt lại cùng designId")
    void postgres_RecutWhenRepeated() throws Exception {
        WorkDesign wd = new WorkDesign();
        wd.setDesignKey("wd-pg-1");
        wd.setOwner(userA);
        wd.setName("Bản cắt PG");
        wd.setHasBeenCut(false);
        wd.setCreatedAt(LocalDateTime.now());
        wd.setUpdatedAt(LocalDateTime.now());
        workDesignRepository.save(wd);

        String json = """
                {
                    "cutAt": "2026-10-03T11:00:00",
                    "partLabel": "Capo",
                    "vehicleLabel": "VF8",
                    "filmUsage": "1,5 m",
                    "designId": "wd-pg-1",
                    "designVersion": 1
                }
                """;

        // Lần 1: COMPLETED, work_designs.has_been_cut = true
        mockMvc.perform(post("/api/v1/cuts")
                        .with(jwtAs("kc-pg-cut-a", "pg_cut_user_a", "sid-pg-A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome", is("completed")));

        WorkDesign reloaded = workDesignRepository.findByDesignKeyAndOwnerId("wd-pg-1", userA.getId()).orElseThrow();
        assertThat(reloaded.isHasBeenCut()).isTrue();

        // Lần 2: RECUT
        mockMvc.perform(post("/api/v1/cuts")
                        .with(jwtAs("kc-pg-cut-a", "pg_cut_user_a", "sid-pg-A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome", is("recut")));
    }

    @Test
    @DisplayName("Postgres: designId của user khác bị bỏ liên kết (design_id = null)")
    void postgres_OtherUserDesignId_Unlinked() throws Exception {
        WorkDesign wd = new WorkDesign();
        wd.setDesignKey("wd-pg-other");
        wd.setOwner(userB);
        wd.setName("Bản của B");
        wd.setHasBeenCut(false);
        wd.setCreatedAt(LocalDateTime.now());
        wd.setUpdatedAt(LocalDateTime.now());
        workDesignRepository.save(wd);

        String json = """
                {
                    "cutAt": "2026-10-03T11:00:00",
                    "partLabel": "Cản trước",
                    "vehicleLabel": "VF9",
                    "filmUsage": "1,8 m",
                    "designId": "wd-pg-other",
                    "designVersion": 1
                }
                """;

        mockMvc.perform(post("/api/v1/cuts")
                        .with(jwtAs("kc-pg-cut-a", "pg_cut_user_a", "sid-pg-A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome", is("completed")));

        List<CutJob> jobs = cutJobRepository.findByUserDeviceIdOrderByCutAtDesc(deviceA.getId());
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getDesignId()).isNull();

        WorkDesign bDesign = workDesignRepository.findByDesignKeyAndOwnerId("wd-pg-other", userB.getId()).orElseThrow();
        assertThat(bDesign.isHasBeenCut()).isFalse();
    }

    @Test
    @DisplayName("Postgres: body có trường hình học lạ không được lưu")
    void postgres_GeometryFieldsIgnored() throws Exception {
        String jsonWithGeo = """
                {
                    "cutAt": "2026-10-03T11:00:00",
                    "partLabel": "Gương chiếu hậu",
                    "vehicleLabel": "Mazda 3",
                    "filmUsage": "0,3 m",
                    "pathData": "M 0 0 L 50 50 Z",
                    "svg": "<svg>...</svg>",
                    "geometry": {"point": [0,0]}
                }
                """;

        MvcResult res = mockMvc.perform(post("/api/v1/cuts")
                        .with(jwtAs("kc-pg-cut-a", "pg_cut_user_a", "sid-pg-A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithGeo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.partLabel", is("Gương chiếu hậu")))
                .andReturn();

        String body = res.getResponse().getContentAsString();
        for (String banned : new String[]{"geometry", "pathdata", "<svg>"}) {
            assertThat(body.toLowerCase()).doesNotContain(banned);
        }
    }

    @Test
    @DisplayName("Postgres: cùng Idempotency-Key không ghi đôi")
    void postgres_IdempotencyKey() throws Exception {
        String json = """
                {
                    "cutAt": "2026-10-03T11:00:00",
                    "partLabel": "Nẹp nóc",
                    "vehicleLabel": "Seltos",
                    "filmUsage": "0,7 m"
                }
                """;

        String idemKey = "pg-key-cl42-999";

        MvcResult r1 = mockMvc.perform(post("/api/v1/cuts")
                        .with(jwtAs("kc-pg-cut-a", "pg_cut_user_a", "sid-pg-A"))
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult r2 = mockMvc.perform(post("/api/v1/cuts")
                        .with(jwtAs("kc-pg-cut-a", "pg_cut_user_a", "sid-pg-A"))
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(r1.getResponse().getContentAsString()).isEqualTo(r2.getResponse().getContentAsString());
        assertThat(cutJobRepository.findByUserDeviceIdOrderByCutAtDesc(deviceA.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Postgres: máy bị gỡ hoặc phiên không còn hiệu lực → 401")
    void postgres_RevokedDevice_Returns401() throws Exception {
        String json = """
                {
                    "cutAt": "2026-10-03T11:00:00",
                    "partLabel": "Chắn bùn",
                    "vehicleLabel": "Innova",
                    "filmUsage": "0,4 m"
                }
                """;

        mockMvc.perform(post("/api/v1/cuts")
                        .with(jwtAs("kc-pg-cut-a", "pg_cut_user_a", "sid-khong-ton-tai"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("SESSION_REVOKED")));
    }

    @Test
    @DisplayName("Postgres: cutAt nhận mọi dạng ISO-8601 (+07:00, Z quy về giờ VN, không múi giờ)")
    void postgres_CutAt_IsoTimeZones() throws Exception {
        // 1. Có offset +07:00: 2026-10-04T01:10:00+07:00 -> đúng 2026-10-04T01:10:00
        String jsonOffset = """
                {
                    "cutAt": "2026-10-04T01:10:00+07:00",
                    "partLabel": "Đèn pha",
                    "vehicleLabel": "VF8",
                    "filmUsage": "0,8 m"
                }
                """;

        mockMvc.perform(post("/api/v1/cuts")
                        .with(jwtAs("kc-pg-cut-a", "pg_cut_user_a", "sid-pg-A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonOffset))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.at", is("2026-10-04T01:10:00")));

        // 2. Có Z (UTC): 2026-10-03T18:10:00Z -> quy sang Asia/Ho_Chi_Minh (+7) là 2026-10-04T01:10:00
        String jsonUtc = """
                {
                    "cutAt": "2026-10-03T18:10:00Z",
                    "partLabel": "Đèn gầm",
                    "vehicleLabel": "VF8",
                    "filmUsage": "0,4 m"
                }
                """;

        mockMvc.perform(post("/api/v1/cuts")
                        .with(jwtAs("kc-pg-cut-a", "pg_cut_user_a", "sid-pg-A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonUtc))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.at", is("2026-10-04T01:10:00")));

        // 3. Không múi giờ: 2026-10-04T02:00:00 -> giữ nguyên 2026-10-04T02:00:00
        String jsonNoZone = """
                {
                    "cutAt": "2026-10-04T02:00:00",
                    "partLabel": "Nẹp cửa",
                    "vehicleLabel": "VF8",
                    "filmUsage": "0,5 m"
                }
                """;

        mockMvc.perform(post("/api/v1/cuts")
                        .with(jwtAs("kc-pg-cut-a", "pg_cut_user_a", "sid-pg-A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonNoZone))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.at", is("2026-10-04T02:00:00")));
    }

    @Test
    @DisplayName("Postgres: designId nhận id user_svg_files của chính user; user khác bị bỏ liên kết; GET trả đúng designId")
    void postgres_UserSvgFile_DesignId() throws Exception {
        // Tạo file SVG đã lưu của userA
        UserSvgFile fileA = UserSvgFile.builder()
                .fileName("Ban-cat-user-A.svg")
                .originalFileName("Ban-cat-user-A.svg")
                .storedFileName("stored-a.svg")
                .filePath("target/test-storage/stored-a.svg")
                .fileSize(1024L)
                .checksum("chk-a")
                .user(userA)
                .status("ACTIVE")
                .build();
        fileA = userSvgFileRepository.save(fileA);
        String designIdA = String.valueOf(fileA.getId());

        // Tạo file SVG đã lưu của userB
        UserSvgFile fileB = UserSvgFile.builder()
                .fileName("Ban-cat-user-B.svg")
                .originalFileName("Ban-cat-user-B.svg")
                .storedFileName("stored-b.svg")
                .filePath("target/test-storage/stored-b.svg")
                .fileSize(2048L)
                .checksum("chk-b")
                .user(userB)
                .status("ACTIVE")
                .build();
        fileB = userSvgFileRepository.save(fileB);
        String designIdB = String.valueOf(fileB.getId());

        // 1. Gửi với designId của chính userA -> nhận liên kết và GET /api/v1/cuts trả về đúng designId
        String jsonA = """
                {
                    "cutAt": "2026-10-04T01:30:00+07:00",
                    "partLabel": "Cản sau",
                    "vehicleLabel": "Mazda CX-5",
                    "filmUsage": "1,2 m",
                    "designId": "%s",
                    "designVersion": 1
                }
                """.formatted(designIdA);

        mockMvc.perform(post("/api/v1/cuts")
                        .with(jwtAs("kc-pg-cut-a", "pg_cut_user_a", "sid-pg-A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.designId", is(designIdA)))
                .andExpect(jsonPath("$.designVersion", is(1)));

        // Kiểm tra GET /api/v1/cuts thấy designId trả về đúng để client "Mở lại" và có designAvailable = true
        mockMvc.perform(get("/api/v1/cuts")
                        .with(jwtAs("kc-pg-cut-a", "pg_cut_user_a", "sid-pg-A"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].designId", is(designIdA)))
                .andExpect(jsonPath("$.jobs[0].designVersion", is(1)))
                .andExpect(jsonPath("$.jobs[0].designAvailable", is(true)));

        // Xoá mềm bản lưu fileA
        fileA.setStatus("DELETED");
        userSvgFileRepository.save(fileA);

        // GET /api/v1/cuts vẫn trả dòng lịch sử, nhưng designAvailable = false (để app ẩn nút "Mở lại")
        mockMvc.perform(get("/api/v1/cuts")
                        .with(jwtAs("kc-pg-cut-a", "pg_cut_user_a", "sid-pg-A"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].designId", is(designIdA)))
                .andExpect(jsonPath("$.jobs[0].designAvailable", is(false)));

        // 2. Gửi với designId của userB -> bỏ liên kết (designId = null, designVersion = null)
        String jsonB = """
                {
                    "cutAt": "2026-10-04T01:35:00+07:00",
                    "partLabel": "Nắp bình xăng",
                    "vehicleLabel": "Mazda CX-5",
                    "filmUsage": "0,2 m",
                    "designId": "%s",
                    "designVersion": 1
                }
                """.formatted(designIdB);

        mockMvc.perform(post("/api/v1/cuts")
                        .with(jwtAs("kc-pg-cut-a", "pg_cut_user_a", "sid-pg-A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonB))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.designId").doesNotExist());

        List<CutJob> jobs = cutJobRepository.findByUserDeviceIdOrderByCutAtDesc(deviceA.getId());
        CutJob latestJob = jobs.get(0); // cutAt 01:35:00 là mới nhất
        assertThat(latestJob.getDesignId()).isNull();
        assertThat(latestJob.getDesignVersion()).isNull();
    }
}
