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
        for (String banned : new String[]{"geometry", "path", "point", "outline", "svg", "shape", "design"}) {
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
}
