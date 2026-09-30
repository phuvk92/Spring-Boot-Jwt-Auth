package com.example.svgmanager.integration;

import com.example.svgmanager.entity.Dealer;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.UserDevice;
import com.example.svgmanager.repository.DealerRepository;
import com.example.svgmanager.repository.UserDeviceRepository;
import com.example.svgmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-07 / KX-04 — GET /api/v1/profile: hồ sơ của đúng user theo {@code sub} trong token,
 * trường chưa có dữ liệu trả null/[] tường minh chứ không lỗi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    private Dealer dealer;
    private User worker;        // thợ thuộc đại lý, có máy đăng ký
    private User admin;         // không thuộc đại lý, không máy

    @BeforeEach
    void setUp() {
        dealer = dealerRepository.save(Dealer.builder()
                .code("DL-P01").name("Dealer Profile").plan("Chuyên nghiệp").status("ACTIVE")
                .build());

        worker = userRepository.save(User.builder()
                .username("pf_worker").email("pf_worker@t.vn").keycloakUserId("kc-pf-worker")
                .fullName("Thợ Profile").phone("0901111222").role(Role.USER).dealer(dealer)
                .enabled(true).build());
        admin = userRepository.save(User.builder()
                .username("pf_admin").email("pf_admin@t.vn").keycloakUserId("kc-pf-admin")
                .role(Role.ADMIN).enabled(true).build());

        userDeviceRepository.save(UserDevice.register(worker, "dev-aaa", "XUONG-01", "Windows",
                "10.0.0.1", "sid-worker-1", LocalDateTime.now()));
        UserDevice old = UserDevice.register(worker, "dev-bbb", "XUONG-02", "Windows",
                "10.0.0.2", null, LocalDateTime.now().minusDays(3));
        old.setStatus(com.example.svgmanager.entity.DeviceStatus.REVOKED);
        old.setRevokedAt(LocalDateTime.now().minusDays(1));
        userDeviceRepository.save(old);
    }

    @Test
    @DisplayName("Trả đủ khối user/dealer/plan/devices của đúng user theo token; quota null chờ A6a")
    void profile_returnsOwnProfile_withExplicitNulls() throws Exception {
        mockMvc.perform(get("/api/v1/profile")
                        .with(jwt().jwt(j -> j.subject("kc-pf-worker")
                                        .claim("preferred_username", "pf_worker")
                                        .claim("sid", "sid-worker-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("pf_worker"))
                .andExpect(jsonPath("$.user.displayName").value("Thợ Profile"))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.email").value("pf_worker@t.vn"))
                .andExpect(jsonPath("$.user.phone").value("0901111222"))
                .andExpect(jsonPath("$.dealer.code").value("DL-P01"))
                .andExpect(jsonPath("$.dealer.name").value("Dealer Profile"))
                .andExpect(jsonPath("$.plan.name").value("Chuyên nghiệp"))
                .andExpect(jsonPath("$.plan.seatsUsed").value(nullValue()))
                .andExpect(jsonPath("$.quota").value(nullValue()))
                // devices khớp khuôn /api/internal/devices: gồm cả REVOKED, cờ current theo sid
                .andExpect(jsonPath("$.devices", hasSize(2)))
                .andExpect(jsonPath("$.devices[?(@.deviceId=='dev-aaa')].isCurrent").value(hasItem(true)))
                .andExpect(jsonPath("$.devices[?(@.deviceId=='dev-bbb')].status").value(hasItem("REVOKED")));
    }

    @Test
    @DisplayName("Tài khoản không thuộc đại lý: dealer/plan null tường minh, devices rỗng — không 500")
    void profile_noDealer_returnsNullBlocks() throws Exception {
        mockMvc.perform(get("/api/v1/profile")
                        .with(jwt().jwt(j -> j.subject("kc-pf-admin")
                                        .claim("preferred_username", "pf_admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("pf_admin"))
                .andExpect(jsonPath("$.user.displayName").value("pf_admin"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.dealer").value(nullValue()))
                .andExpect(jsonPath("$.plan").value(nullValue()))
                .andExpect(jsonPath("$.quota").value(nullValue()))
                .andExpect(jsonPath("$.devices", hasSize(0)));
    }

    @Test
    @DisplayName("Không gửi userId — hồ sơ luôn theo sub của token, không xem được người khác")
    void profile_ignoresCallerSuppliedIdentity() throws Exception {
        mockMvc.perform(get("/api/v1/profile")
                        .param("userId", String.valueOf(worker.getId()))
                        .with(jwt().jwt(j -> j.subject("kc-pf-admin")
                                        .claim("preferred_username", "pf_admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("pf_admin"));
    }

    @Test
    @DisplayName("Thiếu token → 401")
    void profile_noToken_is401() throws Exception {
        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isUnauthorized());
    }
}
