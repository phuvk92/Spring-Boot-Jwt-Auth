package com.example.svgmanager.integration;

import com.example.svgmanager.entity.Dealer;
import com.example.svgmanager.entity.DeviceStatus;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.UserDevice;
import com.example.svgmanager.repository.DealerRepository;
import com.example.svgmanager.repository.UserDeviceRepository;
import com.example.svgmanager.repository.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test tích hợp cho API thiết bị toàn hệ thống (F-57 · NGO-422):
 * - GET /api/devices (phân trang, lọc q/dealerId/status, thứ tự lastSeenAt desc)
 * - GET /api/devices/stats (activeNow, registered, usersAtLimit, staleDevices)
 * - Phân quyền: ADMIN thấy tất cả, AGENT chỉ thấy thuộc đại lý mình, USER -> 403
 * Chạy trên PostgreSQL thật với IT_DB_URL.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://mock-keycloak/realms/cutting/protocol/openid-connect/certs",
        "app.file.storage-path=target/test-svg-storage-devices",
        "app.keycloak.sync-enabled=false",
        "app.device.enforce=false",
        "app.device.max-per-user=1"
})
@AutoConfigureMockMvc
@Transactional
class DevicesPostgresIntegrationTest {

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
    private DealerRepository dealerRepository;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    private User admin;
    private Dealer dealerA;
    private Dealer dealerB;
    private User agentA;
    private User agentB;
    private User workerA1;
    private User workerA2;
    private User workerB1;

    private UserDevice devA1_active_now;
    private UserDevice devA1_revoked;
    private UserDevice devA2_stale;
    private UserDevice devB1_active;

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtAdmin() {
        return jwt().jwt(builder -> builder
                .subject("kc-admin-ngo422")
                .claim("preferred_username", "admin_ngo422")
                .claim("email", "admin422@pcut.vn")
                .claim("realm_access", Map.of("roles", List.of("ADMIN")))
        ).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtAgentA() {
        return jwt().jwt(builder -> builder
                .subject("kc-agentA-ngo422")
                .claim("preferred_username", "agent_a_ngo422")
                .claim("email", "agent_a422@pcut.vn")
                .claim("realm_access", Map.of("roles", List.of("AGENT")))
        ).authorities(new SimpleGrantedAuthority("ROLE_AGENT"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtUser() {
        return jwt().jwt(builder -> builder
                .subject("kc-user-ngo422")
                .claim("preferred_username", "worker_a1_ngo422")
                .claim("email", "worker_a1@pcut.vn")
                .claim("realm_access", Map.of("roles", List.of("USER")))
        ).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @BeforeEach
    void setUp() {
        // Dọn dữ liệu test liên quan
        userDeviceRepository.deleteAll();

        admin = userRepository.save(User.builder()
                .username("admin_ngo422")
                .email("admin422@pcut.vn")
                .fullName("Quản trị hệ thống")
                .keycloakUserId("kc-admin-ngo422")
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        dealerA = dealerRepository.save(Dealer.builder()
                .code("DL-HN-01")
                .name("PPF Hà Nội Center")
                .status("ACTIVE")
                .build());

        dealerB = dealerRepository.save(Dealer.builder()
                .code("DL-SG-02")
                .name("Decal Ô Tô Sài Gòn")
                .status("ACTIVE")
                .build());

        agentA = userRepository.save(User.builder()
                .username("agent_a_ngo422")
                .email("agent_a422@pcut.vn")
                .fullName("Đại lý Hà Nội")
                .keycloakUserId("kc-agentA-ngo422")
                .role(Role.AGENT)
                .dealer(dealerA)
                .enabled(true)
                .build());

        agentB = userRepository.save(User.builder()
                .username("agent_b_ngo422")
                .email("agent_b422@pcut.vn")
                .fullName("Đại lý Sài Gòn")
                .keycloakUserId("kc-agentB-ngo422")
                .role(Role.AGENT)
                .dealer(dealerB)
                .enabled(true)
                .build());

        // workerA1 thuộc dealerA, max_devices = 1 (mặc định)
        User uA1 = User.builder()
                .username("worker_a1_ngo422")
                .email("worker_a1@pcut.vn")
                .fullName("Trần Minh Hoàng")
                .keycloakUserId("kc-user-ngo422")
                .role(Role.USER)
                .dealer(dealerA)
                .agent(agentA)
                .enabled(true)
                .build();
        uA1.setMaxDevices(1);
        workerA1 = userRepository.save(uA1);

        // workerA2 thuộc dealerA, max_devices = 2
        User uA2 = User.builder()
                .username("worker_a2_ngo422")
                .email("worker_a2@pcut.vn")
                .fullName("Phạm Anh Dũng")
                .keycloakUserId("kc-user-a2")
                .role(Role.USER)
                .dealer(dealerA)
                .agent(agentA)
                .enabled(true)
                .build();
        uA2.setMaxDevices(2);
        workerA2 = userRepository.save(uA2);

        // workerB1 thuộc dealerB
        User uB1 = User.builder()
                .username("worker_b1_ngo422")
                .email("worker_b1@pcut.vn")
                .fullName("Võ Quốc Khánh")
                .keycloakUserId("kc-user-b1")
                .role(Role.USER)
                .dealer(dealerB)
                .agent(agentB)
                .enabled(true)
                .build();
        uB1.setMaxDevices(1);
        workerB1 = userRepository.save(uB1);

        LocalDateTime now = LocalDateTime.now();

        // 1. devA1_active_now: ACTIVE, lastSeenAt 5 phút trước -> activeNow = true, stale = false
        devA1_active_now = UserDevice.register(workerA1, "dev-uuid-a1-now", "PC xưởng 1", "Windows", "113.161.44.2", "sid-a1-1", now.minusMinutes(5));
        devA1_active_now = userDeviceRepository.save(devA1_active_now);

        // 2. devA1_revoked: REVOKED, lastSeenAt 20 phút trước -> không active
        devA1_revoked = UserDevice.register(workerA1, "dev-uuid-a1-old", "Laptop mang về", "Windows", "113.161.44.3", "sid-a1-2", now.minusMinutes(20));
        devA1_revoked.setStatus(DeviceStatus.REVOKED);
        devA1_revoked.setRevokedAt(now.minusMinutes(10));
        devA1_revoked.setRevokedBy("admin_ngo422");
        devA1_revoked = userDeviceRepository.save(devA1_revoked);

        // 3. devA2_stale: ACTIVE, lastSeenAt 35 ngày trước -> activeNow = false, stale = true
        // workerA2 có maxDevices = 2, chỉ có 1 device active -> workerA2 chưa at limit (1/2)
        // workerA1 có maxDevices = 1, có 1 device active (devA1_active_now) -> workerA1 at limit (1/1)
        devA2_stale = UserDevice.register(workerA2, "dev-uuid-a2-stale", "PC xưởng 2", "Windows", "14.191.88.7", "sid-a2-1", now.minusDays(35));
        devA2_stale.setFirstSeenAt(now.minusDays(40));
        devA2_stale.setLastSeenAt(now.minusDays(35));
        devA2_stale = userDeviceRepository.save(devA2_stale);

        // 4. devB1_active: ACTIVE, thuộc dealerB, lastSeenAt 2 giờ trước -> activeNow = false, stale = false
        // workerB1 có maxDevices = 1, 1 device active -> workerB1 at limit (1/1)
        devB1_active = UserDevice.register(workerB1, "dev-uuid-b1-act", "PC xưởng SG", "Windows", "222.255.31.5", "sid-b1-1", now.minusHours(2));
        devB1_active = userDeviceRepository.save(devB1_active);
    }

    // ── GET /api/devices (Danh sách thiết bị) ───────────────────────────────

    @Test
    @DisplayName("ADMIN: lấy danh sách mặc định (status=ACTIVE) -> ra 3 máy active, sắp lastSeenAt giảm dần")
    void admin_GetDevices_DefaultActive() throws Exception {
        mockMvc.perform(get("/api/devices").with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content", hasSize(3)))
                // Thứ tự lastSeenAt giảm dần: devA1_active_now (5m trước) > devB1_active (2h trước) > devA2_stale (35d trước)
                .andExpect(jsonPath("$.content[0].deviceRegId").value(devA1_active_now.getId()))
                .andExpect(jsonPath("$.content[0].username").value("worker_a1_ngo422"))
                .andExpect(jsonPath("$.content[0].fullName").value("Trần Minh Hoàng"))
                .andExpect(jsonPath("$.content[0].dealerName").value("PPF Hà Nội Center"))
                .andExpect(jsonPath("$.content[0].deviceName").value("PC xưởng 1"))
                .andExpect(jsonPath("$.content[0].platform").value("Windows"))
                .andExpect(jsonPath("$.content[0].lastIp").value("113.161.44.2"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[1].deviceRegId").value(devB1_active.getId()))
                .andExpect(jsonPath("$.content[2].deviceRegId").value(devA2_stale.getId()));
    }

    @Test
    @DisplayName("ADMIN: lọc status=REVOKED -> ra 1 máy đã gỡ kèm revokedAt và revokedBy")
    void admin_GetDevices_StatusRevoked() throws Exception {
        mockMvc.perform(get("/api/devices")
                        .param("status", "REVOKED")
                        .with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].deviceRegId").value(devA1_revoked.getId()))
                .andExpect(jsonPath("$.content[0].status").value("REVOKED"))
                .andExpect(jsonPath("$.content[0].revokedBy").value("admin_ngo422"))
                .andExpect(jsonPath("$.content[0].revokedAt").isNotEmpty());
    }

    @Test
    @DisplayName("ADMIN: lọc status=ALL -> ra đủ 4 máy cả ACTIVE lẫn REVOKED")
    void admin_GetDevices_StatusAll() throws Exception {
        mockMvc.perform(get("/api/devices")
                        .param("status", "ALL")
                        .with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    @DisplayName("ADMIN: tìm kiếm q theo username, fullName, deviceName, IP")
    void admin_GetDevices_SearchFilter() throws Exception {
        // Tìm theo họ tên
        mockMvc.perform(get("/api/devices")
                        .param("q", "Minh Hoàng")
                        .with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("worker_a1_ngo422"));

        // Tìm theo IP
        mockMvc.perform(get("/api/devices")
                        .param("q", "222.255.31.5")
                        .with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("worker_b1_ngo422"));

        // Tìm theo tên máy
        mockMvc.perform(get("/api/devices")
                        .param("q", "PC xưởng 2")
                        .with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].deviceName").value("PC xưởng 2"));
    }

    @Test
    @DisplayName("AGENT: chỉ thấy máy thuộc đại lý mình; truyền dealerId khác vẫn bị ép về đại lý mình")
    void agent_GetDevices_ScopedToOwnDealer() throws Exception {
        // Agent A thuộc dealerA -> chỉ thấy 2 máy ACTIVE của workerA1 và workerA2
        mockMvc.perform(get("/api/devices").with(jwtAgentA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].dealerId", everyItem(is(dealerA.getId().intValue()))));

        // Agent A cố tình truyền dealerId của Dealer B -> vẫn bị ép về Dealer A, không thấy máy Dealer B
        mockMvc.perform(get("/api/devices")
                        .param("dealerId", String.valueOf(dealerB.getId()))
                        .with(jwtAgentA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].dealerId", everyItem(is(dealerA.getId().intValue()))));
    }

    @Test
    @DisplayName("USER: gọi GET /api/devices -> 403 Forbidden")
    void user_GetDevices_Forbidden() throws Exception {
        mockMvc.perform(get("/api/devices").with(jwtUser()))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/devices/stats (Bốn thẻ thống kê) ──────────────────────────

    @Test
    @DisplayName("ADMIN: thống kê toàn hệ thống đúng 4 số liệu")
    void admin_GetStats_AllSystem() throws Exception {
        // Toàn hệ thống:
        // - registered: 3 máy ACTIVE (devA1_active_now, devA2_stale, devB1_active)
        // - activeNow: 1 máy (devA1_active_now có lastSeenAt trong 15p)
        // - staleDevices: 1 máy (devA2_stale có lastSeenAt > 30 ngày trước)
        // - usersAtLimit: 2 user (workerA1: 1/1, workerB1: 1/1; workerA2: 1/2)
        mockMvc.perform(get("/api/devices/stats").with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeNow").value(1))
                .andExpect(jsonPath("$.registered").value(3))
                .andExpect(jsonPath("$.usersAtLimit").value(2))
                .andExpect(jsonPath("$.staleDevices").value(1));
    }

    @Test
    @DisplayName("AGENT: thống kê theo đại lý mình (dealerA)")
    void agent_GetStats_ScopedToDealer() throws Exception {
        // Dealer A:
        // - registered: 2 máy ACTIVE (devA1_active_now, devA2_stale)
        // - activeNow: 1 máy (devA1_active_now)
        // - staleDevices: 1 máy (devA2_stale)
        // - usersAtLimit: 1 user (workerA1: 1/1; workerA2: 1/2)
        mockMvc.perform(get("/api/devices/stats").with(jwtAgentA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeNow").value(1))
                .andExpect(jsonPath("$.registered").value(2))
                .andExpect(jsonPath("$.usersAtLimit").value(1))
                .andExpect(jsonPath("$.staleDevices").value(1));
    }

    @Test
    @DisplayName("USER: gọi GET /api/devices/stats -> 403 Forbidden")
    void user_GetStats_Forbidden() throws Exception {
        mockMvc.perform(get("/api/devices/stats").with(jwtUser()))
                .andExpect(status().isForbidden());
    }
}
