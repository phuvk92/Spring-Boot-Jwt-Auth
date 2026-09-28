package com.example.svgmanager.integration;

import com.example.svgmanager.entity.DeviceStatus;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.UserDevice;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.exception.UnauthorizedException;
import com.example.svgmanager.repository.UserDeviceRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.service.KeycloakUserService;
import com.example.svgmanager.service.UserDeviceService;
import com.example.svgmanager.service.UserDeviceService.DeviceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-57 — giới hạn 1 tài khoản 1 thiết bị. Thiết kế: Pcut-Client/technical/SA-GioiHanThietBi.md.
 * Bật kiểm máy (mặc định test tắt) và tắt cache phiên để thấy ngay hiệu lực của việc gỡ máy.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.device.enforce=true",
        "app.device.max-per-user=1",
        "app.device.session-cache-seconds=0"
})
@Transactional
class DeviceLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDeviceService userDeviceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @MockBean
    private KeycloakUserService keycloakUserService;

    private User admin;
    private User agent;
    private User worker;        // thợ thuộc agent
    private User otherWorker;   // thợ KHÔNG thuộc agent

    @BeforeEach
    void setUp() {
        admin = userRepository.save(User.builder().username("dev_admin").email("dev_admin@t.vn")
                .keycloakUserId("kc-dev-admin").role(Role.ADMIN).enabled(true).build());
        agent = userRepository.save(User.builder().username("dev_agent").email("dev_agent@t.vn")
                .keycloakUserId("kc-dev-agent").role(Role.AGENT).enabled(true).build());
        worker = userRepository.save(User.builder().username("dev_worker").email("dev_worker@t.vn")
                .keycloakUserId("kc-dev-worker").role(Role.USER).agent(agent).enabled(true).build());
        otherWorker = userRepository.save(User.builder().username("dev_other").email("dev_other@t.vn")
                .keycloakUserId("kc-dev-other").role(Role.USER).enabled(true).build());
    }

    // ── Đăng ký máy khi đăng nhập ─────────────────────────────────────────

    @Test
    @DisplayName("Máy đầu tiên được đăng ký, máy thứ hai bị từ chối SESSION_LIMIT")
    void secondDevice_IsRejected() {
        userDeviceService.bindOnLogin(worker, ctx("may-A"), "sid-A");

        assertThatThrownBy(() -> userDeviceService.bindOnLogin(worker, ctx("may-B"), "sid-B"))
                .isInstanceOf(ForbiddenException.class)
                .extracting(e -> ((ForbiddenException) e).getCode())
                .isEqualTo(ErrorCodes.SESSION_LIMIT);

        assertThat(userDeviceRepository.countByUserIdAndStatus(worker.getId(), DeviceStatus.ACTIVE)).isEqualTo(1);
        assertThat(userDeviceService.isSessionActive("sid-A")).isTrue();
        assertThat(userDeviceService.isSessionActive("sid-B")).isFalse();
    }

    @Test
    @DisplayName("Cùng máy đăng nhập lại: không tốn thêm chỗ, phiên cũ trên máy đó bị chấm dứt")
    void sameDevice_ReLogin_ReplacesSession() {
        userDeviceService.bindOnLogin(worker, ctx("may-A"), "sid-A1");
        userDeviceService.bindOnLogin(worker, ctx("may-A"), "sid-A2");

        assertThat(userDeviceRepository.countByUserIdAndStatus(worker.getId(), DeviceStatus.ACTIVE)).isEqualTo(1);
        assertThat(userDeviceService.isSessionActive("sid-A1")).isFalse();
        assertThat(userDeviceService.isSessionActive("sid-A2")).isTrue();
        verify(keycloakUserService).deleteSession("sid-A1");
    }

    @Test
    @DisplayName("maxDevices riêng của tài khoản cho phép thêm máy")
    void perUserMaxDevices_AllowsMore() {
        worker.setMaxDevices(2);
        userRepository.save(worker);

        userDeviceService.bindOnLogin(worker, ctx("may-A"), "sid-A");
        userDeviceService.bindOnLogin(worker, ctx("may-B"), "sid-B");

        assertThat(userDeviceRepository.countByUserIdAndStatus(worker.getId(), DeviceStatus.ACTIVE)).isEqualTo(2);
    }

    @Test
    @DisplayName("Mỗi tài khoản giới hạn riêng — tài khoản khác dùng được máy khác")
    void limitIsPerAccount() {
        userDeviceService.bindOnLogin(worker, ctx("may-A"), "sid-A");
        userDeviceService.bindOnLogin(otherWorker, ctx("may-B"), "sid-B");

        assertThat(userDeviceService.isSessionActive("sid-B")).isTrue();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Hai máy đăng nhập đồng thời: khoá hàng user chỉ cho một máy lọt")
    void concurrentLogin_OnlyOneDeviceWins() throws Exception {
        // Test này cần transaction riêng của từng luồng nên chạy ngoài transaction của lớp —
        // mọi dữ liệu (kể cả user của setUp) được commit thật và phải dọn tay ở finally.
        User racer = userRepository.saveAndFlush(User.builder().username("dev_race").email("dev_race@t.vn")
                .keycloakUserId("kc-dev-race").role(Role.USER).enabled(true).build());
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            List<Throwable> failures = new CopyOnWriteArrayList<>();
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                final int n = i;
                Thread t = new Thread(() -> {
                    ready.countDown();
                    try {
                        go.await(10, TimeUnit.SECONDS);
                        userDeviceService.bindOnLogin(racer, ctx("may-race-" + n), "sid-race-" + n);
                    } catch (Throwable th) {
                        failures.add(th);
                    }
                });
                threads.add(t);
                t.start();
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            for (Thread t : threads) {
                t.join(TimeUnit.SECONDS.toMillis(30));
                assertThat(t.isAlive()).as("luồng login bị treo").isFalse();
            }

            assertThat(failures).hasSize(1);
            assertThat(failures.get(0))
                    .isInstanceOf(ForbiddenException.class)
                    .extracting(e -> ((ForbiddenException) e).getCode())
                    .isEqualTo(ErrorCodes.SESSION_LIMIT);
            assertThat(userDeviceRepository.countByUserIdAndStatus(racer.getId(), DeviceStatus.ACTIVE)).isEqualTo(1);
        } finally {
            userDeviceRepository.findByUserIdOrderByStatusAscLastSeenAtDesc(racer.getId())
                    .forEach(userDeviceRepository::delete);
            userRepository.delete(racer);
            userRepository.deleteAll(List.of(admin, agent, worker, otherWorker));
        }
    }

    // ── Làm mới phiên ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Refresh từ máy khác với máy gắn phiên → SESSION_REVOKED")
    void refresh_FromOtherDevice_IsRevoked() {
        userDeviceService.bindOnLogin(worker, ctx("may-A"), "sid-A");

        userDeviceService.verifyOnRefresh("may-A", "sid-A");   // đúng máy — qua
        assertThatThrownBy(() -> userDeviceService.verifyOnRefresh("may-B", "sid-A"))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(e -> ((UnauthorizedException) e).getCode())
                .isEqualTo(ErrorCodes.SESSION_REVOKED);
        assertThatThrownBy(() -> userDeviceService.verifyOnRefresh("may-A", "sid-khong-co"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Đăng xuất: máy vẫn giữ chỗ, phiên không còn hiệu lực")
    void logout_KeepsSlot_EndsSession() {
        userDeviceService.bindOnLogin(worker, ctx("may-A"), "sid-A");
        userDeviceService.releaseSession("sid-A");

        assertThat(userDeviceService.isSessionActive("sid-A")).isFalse();
        assertThat(userDeviceRepository.countByUserIdAndStatus(worker.getId(), DeviceStatus.ACTIVE)).isEqualTo(1);
        assertThatThrownBy(() -> userDeviceService.bindOnLogin(worker, ctx("may-B"), "sid-B"))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── Filter /api/internal/** ───────────────────────────────────────────

    @Test
    @DisplayName("Thợ xem máy của mình; máy hiện tại được đánh dấu isCurrent")
    void internalDevices_ListsOwnDevices() throws Exception {
        userDeviceService.bindOnLogin(worker, new DeviceContext("may-A-0123456789abcdef", "XUONG-01", "Windows", "10.0.0.5"), "sid-A");

        mockMvc.perform(get("/api/internal/devices").with(asWorker("sid-A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("XUONG-01"))
                .andExpect(jsonPath("$[0].deviceId").value("may-A-0123456789abcdef"))
                .andExpect(jsonPath("$[0].isCurrent").value(true));
    }

    @Test
    @DisplayName("Token có sid không gắn máy nào → 401 SESSION_REVOKED")
    void internalRequest_UnknownSession_Rejected() throws Exception {
        mockMvc.perform(get("/api/internal/devices").with(asWorker("sid-la")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.SESSION_REVOKED));
    }

    @Test
    @DisplayName("Token không có sid (vd token của web) → bị chặn ở /api/internal/**")
    void internalRequest_NoSid_Rejected() throws Exception {
        mockMvc.perform(get("/api/internal/devices").with(jwt()
                        .jwt(j -> j.subject("kc-dev-worker").claim("preferred_username", "dev_worker"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.SESSION_REVOKED));
    }

    // ── Gỡ máy (ADMIN / AGENT) ────────────────────────────────────────────

    @Test
    @DisplayName("ADMIN gỡ máy: máy cũ bị 401 ngay request kế tiếp, máy mới đăng nhập được")
    void adminRevoke_EndsOldDevice_FreesSlot() throws Exception {
        userDeviceService.bindOnLogin(worker, ctx("may-A"), "sid-A");
        Long regId = userDeviceRepository.findByUserIdAndDeviceId(worker.getId(), "may-A").orElseThrow().getId();

        mockMvc.perform(get("/api/users/" + worker.getId() + "/devices").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        mockMvc.perform(delete("/api/users/" + worker.getId() + "/devices/" + regId).with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.revokedBy").value("dev_admin"));

        verify(keycloakUserService).deleteSession("sid-A");
        mockMvc.perform(get("/api/internal/devices").with(asWorker("sid-A")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.SESSION_REVOKED));

        userDeviceService.bindOnLogin(worker, ctx("may-B"), "sid-B");
        assertThat(userDeviceService.isSessionActive("sid-B")).isTrue();
    }

    @Test
    @DisplayName("Máy đã gỡ đăng nhập lại khi còn chỗ → dùng lại bản ghi cũ")
    void revokedDevice_CanComeBack_WhenSlotFree() {
        userDeviceService.bindOnLogin(worker, ctx("may-A"), "sid-A");
        UserDevice d = userDeviceRepository.findByUserIdAndDeviceId(worker.getId(), "may-A").orElseThrow();
        userDeviceService.revoke(worker, d.getId(), "dev_admin", "ADMIN");

        userDeviceService.bindOnLogin(worker, ctx("may-A"), "sid-A2");

        UserDevice back = userDeviceRepository.findByUserIdAndDeviceId(worker.getId(), "may-A").orElseThrow();
        assertThat(back.getId()).isEqualTo(d.getId());
        assertThat(back.getStatus()).isEqualTo(DeviceStatus.ACTIVE);
        assertThat(back.getRevokedAt()).isNull();
    }

    @Test
    @DisplayName("Gỡ máy đã gỡ: không làm gì, không gọi Keycloak lần nữa")
    void revoke_IsIdempotent() {
        userDeviceService.bindOnLogin(worker, ctx("may-A"), "sid-A");
        Long regId = userDeviceRepository.findByUserIdAndDeviceId(worker.getId(), "may-A").orElseThrow().getId();

        userDeviceService.revoke(worker, regId, "dev_admin", "ADMIN");
        userDeviceService.revoke(worker, regId, "dev_admin", "ADMIN");

        verify(keycloakUserService).deleteSession("sid-A");
    }

    @Test
    @DisplayName("AGENT gỡ được máy của thợ thuộc mình, không thấy thợ ngoài phạm vi")
    void agentRevoke_ScopedToOwnWorkers() throws Exception {
        userDeviceService.bindOnLogin(worker, ctx("may-A"), "sid-A");
        userDeviceService.bindOnLogin(otherWorker, ctx("may-X"), "sid-X");
        Long ownReg = userDeviceRepository.findByUserIdAndDeviceId(worker.getId(), "may-A").orElseThrow().getId();
        Long otherReg = userDeviceRepository.findByUserIdAndDeviceId(otherWorker.getId(), "may-X").orElseThrow().getId();

        mockMvc.perform(delete("/api/users/" + otherWorker.getId() + "/devices/" + otherReg).with(asAgent()))
                .andExpect(status().isNotFound());
        verify(keycloakUserService, never()).deleteSession("sid-X");

        mockMvc.perform(delete("/api/users/" + worker.getId() + "/devices/" + ownReg).with(asAgent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));
    }

    @Test
    @DisplayName("Không gỡ được máy của user khác qua id máy (id máy phải thuộc đúng user)")
    void revoke_DeviceOfAnotherUser_NotFound() throws Exception {
        userDeviceService.bindOnLogin(otherWorker, ctx("may-X"), "sid-X");
        Long otherReg = userDeviceRepository.findByUserIdAndDeviceId(otherWorker.getId(), "may-X").orElseThrow().getId();

        mockMvc.perform(delete("/api/users/" + worker.getId() + "/devices/" + otherReg).with(asAdmin()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Thợ không gọi được API quản trị thiết bị")
    void worker_CannotManageDevices() throws Exception {
        mockMvc.perform(get("/api/users/" + worker.getId() + "/devices").with(asWorker("sid-bat-ky")))
                .andExpect(status().isForbidden());
    }

    // ── Số máy tối đa ─────────────────────────────────────────────────────

    @Test
    @DisplayName("ADMIN đổi maxDevices; 0 = về mặc định")
    void admin_UpdatesMaxDevices() throws Exception {
        mockMvc.perform(put("/api/users/" + worker.getId()).with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dev_worker@t.vn\",\"maxDevices\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxDevices").value(3))
                .andExpect(jsonPath("$.effectiveMaxDevices").value(3));

        mockMvc.perform(put("/api/users/" + worker.getId()).with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dev_worker@t.vn\",\"maxDevices\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxDevices").doesNotExist())
                .andExpect(jsonPath("$.effectiveMaxDevices").value(1));
    }

    @Test
    @DisplayName("AGENT không đổi được maxDevices")
    void agent_CannotUpdateMaxDevices() throws Exception {
        mockMvc.perform(put("/api/users/" + worker.getId()).with(asAgent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dev_worker@t.vn\",\"maxDevices\":5}"))
                .andExpect(status().isForbidden());
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private static DeviceContext ctx(String deviceId) {
        return new DeviceContext(deviceId, null, null, null);
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asWorker(String sid) {
        return jwt().jwt(j -> j.subject("kc-dev-worker").claim("preferred_username", "dev_worker").claim("sid", sid))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asAdmin() {
        return jwt().jwt(j -> j.subject("kc-dev-admin").claim("preferred_username", "dev_admin"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asAgent() {
        return jwt().jwt(j -> j.subject("kc-dev-agent").claim("preferred_username", "dev_agent"))
                .authorities(new SimpleGrantedAuthority("ROLE_AGENT"));
    }
}
