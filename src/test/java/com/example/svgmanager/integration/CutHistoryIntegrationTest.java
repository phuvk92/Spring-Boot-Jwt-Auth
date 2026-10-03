package com.example.svgmanager.integration;

import com.example.svgmanager.entity.CutJob;
import com.example.svgmanager.entity.CutOutcome;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.UserDevice;
import com.example.svgmanager.repository.CutJobRepository;
import com.example.svgmanager.repository.UserDeviceRepository;
import com.example.svgmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-38 · KX-03 — GET /api/v1/cuts: scope theo máy trong token (sid), DTO chỉ số liệu,
 * thiếu dữ liệu nguồn → null chứ không ném.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CutHistoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @Autowired
    private CutJobRepository cutJobRepository;

    @Autowired
    private com.example.svgmanager.repository.WorkDesignRepository workDesignRepository;

    @Autowired
    private com.example.svgmanager.repository.UserSvgFileRepository userSvgFileRepository;

    private User userA;
    private User userB;
    private UserDevice deviceA;
    private UserDevice deviceA2;
    private UserDevice deviceB;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(User.builder().username("cut_user_a").email("cut_a@t.vn")
                .keycloakUserId("kc-cut-a").role(Role.USER).enabled(true).build());
        userB = userRepository.save(User.builder().username("cut_user_b").email("cut_b@t.vn")
                .keycloakUserId("kc-cut-b").role(Role.USER).enabled(true).build());

        LocalDateTime now = LocalDateTime.now();
        deviceA = userDeviceRepository.save(UserDevice.register(userA, "dev-a", "Máy A", "windows", "10.0.0.1", "sid-A", now));
        // Máy thứ hai của CÙNG user A — scope là máy, không phải tài khoản.
        deviceA2 = userDeviceRepository.save(UserDevice.register(userA, "dev-a2", "Máy A2", "windows", "10.0.0.2", "sid-A2", now));
        deviceB = userDeviceRepository.save(UserDevice.register(userB, "dev-b", "Máy B", "windows", "10.0.0.3", "sid-B", now));
    }

    private CutJob job(UserDevice device, LocalDateTime at, String vehicle, CutOutcome outcome, BigDecimal meters) {
        CutJob j = new CutJob();
        j.setUserDevice(device);
        j.setCutAt(at);
        j.setPartLabel("Nắp capo");
        j.setVehicleLabel(vehicle);
        j.setFilmUsage("0,9 m");
        j.setFilmUsageMeters(meters);
        j.setDuration("2′ 18″");
        j.setOutcome(outcome);
        return cutJobRepository.save(j);
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtAs(String keycloakSub, String username, String sid) {
        return jwt().jwt(j -> j.subject(keycloakSub)
                        .claim("preferred_username", username)
                        .claim("sid", sid))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    @DisplayName("Không có token → 401")
    void noToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/cuts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Scope theo device trong token: chỉ thấy job của máy mình, không lộ máy khác dù cùng tài khoản")
    void scopedByDeviceInToken() throws Exception {
        LocalDateTime t = LocalDateTime.of(2026, 8, 16, 10, 30);
        job(deviceA, t, "Mazda CX-5", CutOutcome.COMPLETED, new BigDecimal("0.9"));
        job(deviceA, t.plusHours(1), "Mazda CX-5", CutOutcome.RECUT, new BigDecimal("1.1"));
        job(deviceA2, t, "VinFast VF3", CutOutcome.COMPLETED, BigDecimal.ONE); // cùng user A, máy khác
        job(deviceB, t, "Toyota Vios", CutOutcome.MISALIGNED, BigDecimal.ONE); // user khác

        mockMvc.perform(get("/api/v1/cuts").with(jwtAs("kc-cut-a", "cut_user_a", "sid-A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs", hasSize(2)))
                .andExpect(jsonPath("$.jobs[0].deviceName", is("Máy A")))
                .andExpect(jsonPath("$.stats.jobCount", is(2)))
                .andExpect(jsonPath("$.stats.recutCount", is(1)))
                .andExpect(jsonPath("$.stats.vehicleCount", is(1)))
                .andExpect(jsonPath("$.stats.filmUsed", is("2 m")))
                .andExpect(jsonPath("$.stats.period", is("16/08 – 16/08/2026")));
    }

    @Test
    @DisplayName("DTO chỉ có số liệu — không trường hình học/path/point nào (F-38)")
    void dtoHasNoGeometryFields() throws Exception {
        job(deviceA, LocalDateTime.now(), "Mazda CX-5", CutOutcome.COMPLETED, null);

        MvcResult res = mockMvc.perform(get("/api/v1/cuts").with(jwtAs("kc-cut-a", "cut_user_a", "sid-A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].outcome", is("completed")))
                .andReturn();

        String body = res.getResponse().getContentAsString();
        for (String banned : new String[]{"geometry", "path", "point", "outline", "svg", "shape"}) {
            assertThat(body.toLowerCase()).as("response không được chứa '%s'", banned).doesNotContain(banned);
        }
    }

    @Test
    @DisplayName("Thiếu dữ liệu nguồn (nhãn, số đo) → trường null, không 500")
    void missingSourceData_NullsNotError() throws Exception {
        CutJob bare = new CutJob();
        bare.setUserDevice(deviceA);
        bare.setCutAt(LocalDateTime.of(2026, 9, 1, 8, 0));
        bare.setOutcome(CutOutcome.MISALIGNED);
        cutJobRepository.save(bare);

        mockMvc.perform(get("/api/v1/cuts").with(jwtAs("kc-cut-a", "cut_user_a", "sid-A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].partLabel", nullValue()))
                .andExpect(jsonPath("$.jobs[0].vehicleLabel", nullValue()))
                .andExpect(jsonPath("$.jobs[0].filmUsage", nullValue()))
                .andExpect(jsonPath("$.stats.filmUsed", nullValue()))
                .andExpect(jsonPath("$.stats.vehicleCount", is(0)))
                .andExpect(jsonPath("$.jobs[0].outcome", is("misaligned")));
    }

    @Test
    @DisplayName("Token có sid nhưng không gắn máy nào → lịch sử rỗng, không ném")
    void unknownSession_EmptyHistory() throws Exception {
        job(deviceB, LocalDateTime.now(), "Toyota Vios", CutOutcome.COMPLETED, null);

        mockMvc.perform(get("/api/v1/cuts").with(jwtAs("kc-cut-a", "cut_user_a", "sid-lạ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs", hasSize(0)))
                .andExpect(jsonPath("$.stats.jobCount", is(0)))
                .andExpect(jsonPath("$.stats.period", nullValue()));
    }

    @Test
    @DisplayName("Cùng tháng gộp nhãn đầu khoảng ngày")
    void period_SameMonth() throws Exception {
        job(deviceA, LocalDateTime.of(2026, 8, 10, 9, 0), "Xe 1", CutOutcome.COMPLETED, null);
        job(deviceA, LocalDateTime.of(2026, 8, 16, 9, 0), "Xe 2", CutOutcome.COMPLETED, null);

        mockMvc.perform(get("/api/v1/cuts").with(jwtAs("kc-cut-a", "cut_user_a", "sid-A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.period", is("10/08 – 16/08/2026")))
                .andExpect(jsonPath("$.stats.vehicleCount", is(2)));
    }

    // ── POST /api/v1/cuts (F-38 · NGO-428) ───────────────────────────────────

    @Test
    @DisplayName("Ghi một lượt cắt thành công, trả 201 và dòng vừa ghi, GET /api/v1/cuts thấy ngay dòng mới")
    void recordCut_Success() throws Exception {
        String json = """
                {
                    "cutAt": "2026-10-03T10:15:30",
                    "partLabel": "Đèn trái + phải",
                    "vehicleLabel": "Mazda CX-5",
                    "filmUsage": "0,9 m",
                    "filmUsageMeters": 0.900,
                    "duration": "2′ 18″"
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/cuts")
                        .with(jwtAs("kc-cut-a", "cut_user_a", "sid-A"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.at", is("2026-10-03T10:15:30")))
                .andExpect(jsonPath("$.deviceName", is("Máy A")))
                .andExpect(jsonPath("$.partLabel", is("Đèn trái + phải")))
                .andExpect(jsonPath("$.vehicleLabel", is("Mazda CX-5")))
                .andExpect(jsonPath("$.filmUsage", is("0,9 m")))
                .andExpect(jsonPath("$.duration", is("2′ 18″")))
                .andExpect(jsonPath("$.outcome", is("completed")));

        // GET /api/v1/cuts thấy ngay dòng mới
        mockMvc.perform(get("/api/v1/cuts").with(jwtAs("kc-cut-a", "cut_user_a", "sid-A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs", hasSize(1)))
                .andExpect(jsonPath("$.jobs[0].vehicleLabel", is("Mazda CX-5")))
                .andExpect(jsonPath("$.stats.jobCount", is(1)))
                .andExpect(jsonPath("$.stats.filmUsed", is("0,9 m")));
    }

    @Test
    @DisplayName("RECUT khi cắt lại cùng designId trên cùng máy; lần đầu là COMPLETED")
    void recordCut_RecutOutcome() throws Exception {
        com.example.svgmanager.entity.WorkDesign wd = new com.example.svgmanager.entity.WorkDesign();
        wd.setDesignKey("wd-101");
        wd.setOwner(userA);
        wd.setName("Bản cắt mẫu");
        wd.setHasBeenCut(false);
        wd.setCreatedAt(LocalDateTime.now());
        wd.setUpdatedAt(LocalDateTime.now());
        workDesignRepository.save(wd);

        String json = """
                {
                    "cutAt": "2026-10-03T10:00:00",
                    "partLabel": "Nắp capo",
                    "vehicleLabel": "VF8",
                    "filmUsage": "1,5 m",
                    "filmUsageMeters": 1.5,
                    "duration": "3′ 00″",
                    "designId": "wd-101",
                    "designVersion": 1
                }
                """;

        // Lần cắt đầu tiên -> outcome = completed, work_designs.has_been_cut chuyển thành true
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/cuts")
                        .with(jwtAs("kc-cut-a", "cut_user_a", "sid-A"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome", is("completed")));

        com.example.svgmanager.entity.WorkDesign reloaded = workDesignRepository.findByDesignKeyAndOwnerId("wd-101", userA.getId()).orElseThrow();
        assertThat(reloaded.isHasBeenCut()).isTrue();

        // Lần cắt thứ hai trên cùng máy với cùng designId -> outcome = recut
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/cuts")
                        .with(jwtAs("kc-cut-a", "cut_user_a", "sid-A"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome", is("recut")));
    }

    @Test
    @DisplayName("designId của user khác hoặc không tồn tại bị bỏ liên kết (design_id = null), không 403")
    void recordCut_OtherUserDesignId_Unlinked() throws Exception {
        // userB sở hữu wd-202
        com.example.svgmanager.entity.WorkDesign wd = new com.example.svgmanager.entity.WorkDesign();
        wd.setDesignKey("wd-202");
        wd.setOwner(userB);
        wd.setName("Bản cắt của B");
        wd.setHasBeenCut(false);
        wd.setCreatedAt(LocalDateTime.now());
        wd.setUpdatedAt(LocalDateTime.now());
        workDesignRepository.save(wd);

        String json = """
                {
                    "cutAt": "2026-10-03T10:00:00",
                    "partLabel": "Cản trước",
                    "vehicleLabel": "Civic",
                    "filmUsage": "1,2 m",
                    "designId": "wd-202",
                    "designVersion": 1
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/cuts")
                        .with(jwtAs("kc-cut-a", "cut_user_a", "sid-A"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome", is("completed")));

        // Kiểm tra trong DB: cutJob được lưu với designId = null
        List<CutJob> jobs = cutJobRepository.findByUserDeviceIdOrderByCutAtDesc(deviceA.getId());
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getDesignId()).isNull();
        assertThat(jobs.get(0).getDesignVersion()).isNull();

        // work_design của B không bị ảnh hưởng hasBeenCut
        com.example.svgmanager.entity.WorkDesign bDesign = workDesignRepository.findByDesignKeyAndOwnerId("wd-202", userB.getId()).orElseThrow();
        assertThat(bDesign.isHasBeenCut()).isFalse();
    }

    @Test
    @DisplayName("Body có trường hình học lạ (pathData, svg, geometry) bị bỏ qua, không được lưu")
    void recordCut_GeometryFieldsIgnored() throws Exception {
        String jsonWithGeometry = """
                {
                    "cutAt": "2026-10-03T10:00:00",
                    "partLabel": "Tai xe",
                    "vehicleLabel": "Mazda 3",
                    "filmUsage": "0,5 m",
                    "pathData": "M 0 0 L 100 100 Z",
                    "svg": "<svg><path d='...'/></svg>",
                    "geometry": {"type": "polygon"}
                }
                """;

        MvcResult res = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/cuts")
                        .with(jwtAs("kc-cut-a", "cut_user_a", "sid-A"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(jsonWithGeometry))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.partLabel", is("Tai xe")))
                .andReturn();

        String body = res.getResponse().getContentAsString();
        for (String banned : new String[]{"geometry", "pathdata", "polygon", "<svg>"}) {
            assertThat(body.toLowerCase()).doesNotContain(banned);
        }
    }

    @Test
    @DisplayName("Cùng Idempotency-Key trong 24 giờ không ghi đôi, trả lại đúng bản ghi cũ")
    void recordCut_IdempotencyKey() throws Exception {
        String json = """
                {
                    "cutAt": "2026-10-03T10:00:00",
                    "partLabel": "Nẹp cửa",
                    "vehicleLabel": "CRV",
                    "filmUsage": "0,8 m",
                    "filmUsageMeters": 0.8
                }
                """;

        String idemKey = "cl-42-key-12345";

        MvcResult first = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/cuts")
                        .with(jwtAs("kc-cut-a", "cut_user_a", "sid-A"))
                        .header("Idempotency-Key", idemKey)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult second = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/cuts")
                        .with(jwtAs("kc-cut-a", "cut_user_a", "sid-A"))
                        .header("Idempotency-Key", idemKey)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(first.getResponse().getContentAsString()).isEqualTo(second.getResponse().getContentAsString());

        // DB chỉ có đúng 1 bản ghi
        List<CutJob> jobs = cutJobRepository.findByUserDeviceIdOrderByCutAtDesc(deviceA.getId());
        assertThat(jobs).hasSize(1);
    }

    @Test
    @DisplayName("Máy bị gỡ hoặc phiên không còn hiệu lực → 401 SESSION_REVOKED")
    void recordCut_RevokedSession_Returns401() throws Exception {
        String json = """
                {
                    "cutAt": "2026-10-03T10:00:00",
                    "partLabel": "Nắp xăng",
                    "vehicleLabel": "Camry",
                    "filmUsage": "0,2 m"
                }
                """;

        // sid-la không thuộc máy nào
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/cuts")
                        .with(jwtAs("kc-cut-a", "cut_user_a", "sid-la"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("SESSION_REVOKED")));
    }

    @Test
    @DisplayName("cutAt nhận ISO-8601 offset (+07:00), Z quy về giờ VN, và không múi giờ")
    void recordCut_CutAt_IsoTimeZones() throws Exception {
        String jsonOffset = """
                {
                    "cutAt": "2026-10-04T01:10:00+07:00",
                    "partLabel": "Đèn pha",
                    "vehicleLabel": "VF8",
                    "filmUsage": "0,8 m"
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/cuts")
                        .with(jwtAs("kc-cut-a", "cut_user_a", "sid-A"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(jsonOffset))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.at", is("2026-10-04T01:10:00")));

        String jsonUtc = """
                {
                    "cutAt": "2026-10-03T18:10:00Z",
                    "partLabel": "Đèn gầm",
                    "vehicleLabel": "VF8",
                    "filmUsage": "0,4 m"
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/cuts")
                        .with(jwtAs("kc-cut-a", "cut_user_a", "sid-A"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(jsonUtc))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.at", is("2026-10-04T01:10:00")));

        String jsonNoZone = """
                {
                    "cutAt": "2026-10-04T02:00:00",
                    "partLabel": "Nẹp cửa",
                    "vehicleLabel": "VF8",
                    "filmUsage": "0,5 m"
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/cuts")
                        .with(jwtAs("kc-cut-a", "cut_user_a", "sid-A"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(jsonNoZone))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.at", is("2026-10-04T02:00:00")));
    }

    @Test
    @DisplayName("designId nhận id user_svg_files của chính user; GET /api/v1/cuts trả đúng designId")
    void recordCut_UserSvgFile_DesignId() throws Exception {
        com.example.svgmanager.entity.UserSvgFile fileA = com.example.svgmanager.entity.UserSvgFile.builder()
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

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/cuts")
                        .with(jwtAs("kc-cut-a", "cut_user_a", "sid-A"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(jsonA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.designId", is(designIdA)))
                .andExpect(jsonPath("$.designVersion", is(1)));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/cuts")
                        .with(jwtAs("kc-cut-a", "cut_user_a", "sid-A"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].designId", is(designIdA)))
                .andExpect(jsonPath("$.jobs[0].designVersion", is(1)))
                .andExpect(jsonPath("$.jobs[0].designAvailable", is(true)));

        // Xoá mềm file bản lưu
        fileA.setStatus("DELETED");
        userSvgFileRepository.save(fileA);

        // GET /api/v1/cuts vẫn trả dòng lịch sử, nhưng cờ designAvailable = false
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/cuts")
                        .with(jwtAs("kc-cut-a", "cut_user_a", "sid-A"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].designId", is(designIdA)))
                .andExpect(jsonPath("$.jobs[0].designAvailable", is(false)));
    }
}

