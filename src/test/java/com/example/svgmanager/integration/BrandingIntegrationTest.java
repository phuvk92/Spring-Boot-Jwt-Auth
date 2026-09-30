package com.example.svgmanager.integration;

import com.example.svgmanager.entity.Dealer;
import com.example.svgmanager.entity.DealerBranding;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.repository.DealerBrandingRepository;
import com.example.svgmanager.repository.DealerRepository;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BR-10/BR-13/BR-30/BR-32 — GET /api/v1/branding. Bật kiểm máy để chứng minh
 * phiên bị gỡ vẫn nhận 401 SESSION_REVOKED như mọi endpoint app-cắt khác.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.device.enforce=true",
        "app.device.session-cache-seconds=0"
})
@Transactional
class BrandingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private DealerBrandingRepository dealerBrandingRepository;

    @Autowired
    private UserDeviceService userDeviceService;

    @MockBean
    private KeycloakUserService keycloakUserService;

    private static final byte[] LOGO_PNG = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

    private User workerConfigured;   // thợ của đại lý đã cấu hình thương hiệu
    private User workerBareDealer;   // thợ của đại lý chưa cấu hình (BR-32)
    private User workerNoDealer;     // user độc lập (BR-30)

    @BeforeEach
    void setUp() {
        Dealer dealerConfigured = dealerRepository.save(Dealer.builder()
                .code("DL-BR").name("SG Decal").status("ACTIVE").build());
        Dealer dealerBare = dealerRepository.save(Dealer.builder()
                .code("DL-BARE").name("Dealer Chua Cau Hinh").status("ACTIVE").build());

        DealerBranding b = new DealerBranding();
        b.setDealer(dealerConfigured);
        b.setDisplayName("SG Decal");
        b.setSlogan("Dan phim chuan tung milimet");
        b.setHotline("0901234567");
        b.setPrimaryColor("#7C3AED");
        b.setSecondaryColor("#2E7D5B");
        b.setTheme("dark");
        b.setAllowWorkerThemeToggle(true);
        b.setShowDealerNameNextToLogo(false);
        b.setProtectScreenCapture(false);
        b.setLogoPng(LOGO_PNG);
        dealerBrandingRepository.save(b);

        workerConfigured = userRepository.save(User.builder()
                .username("br_worker_a").email("br_a@t.vn").keycloakUserId("kc-br-a")
                .role(Role.USER).dealer(dealerConfigured).enabled(true).build());
        workerBareDealer = userRepository.save(User.builder()
                .username("br_worker_b").email("br_b@t.vn").keycloakUserId("kc-br-b")
                .role(Role.USER).dealer(dealerBare).enabled(true).build());
        workerNoDealer = userRepository.save(User.builder()
                .username("br_worker_c").email("br_c@t.vn").keycloakUserId("kc-br-c")
                .role(Role.USER).dealer(null).enabled(true).build());
    }

    @Test
    @DisplayName("Thợ của đại lý đã cấu hình → 200 trả đủ trường theo schema Branding")
    void configuredDealer_ReturnsFullBranding() throws Exception {
        userDeviceService.bindOnLogin(workerConfigured, ctx("may-1"), "sid-br-1");

        mockMvc.perform(get("/api/v1/branding").with(asUser("kc-br-a", "br_worker_a", "sid-br-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("SG Decal"))
                .andExpect(jsonPath("$.slogan").value("Dan phim chuan tung milimet"))
                .andExpect(jsonPath("$.hotline").value("0901234567"))
                .andExpect(jsonPath("$.primaryColor").value("#7C3AED"))
                .andExpect(jsonPath("$.secondaryColor").value("#2E7D5B"))
                .andExpect(jsonPath("$.theme").value("dark"))
                .andExpect(jsonPath("$.allowWorkerThemeToggle").value(true))
                .andExpect(jsonPath("$.showDealerNameNextToLogo").value(false))
                .andExpect(jsonPath("$.protectScreenCapture").value(false))
                .andExpect(jsonPath("$.logoPng").value(Base64.getEncoder().encodeToString(LOGO_PNG)));
    }

    @Test
    @DisplayName("Đại lý chưa cấu hình → 200 bản mặc định Pcut, logoPng null (BR-32)")
    void bareDealer_ReturnsPcutDefault() throws Exception {
        userDeviceService.bindOnLogin(workerBareDealer, ctx("may-2"), "sid-br-2");

        mockMvc.perform(get("/api/v1/branding").with(asUser("kc-br-b", "br_worker_b", "sid-br-2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Pcut"))
                .andExpect(jsonPath("$.primaryColor").value("#2563C9"))
                .andExpect(jsonPath("$.theme").value("system"))
                .andExpect(jsonPath("$.protectScreenCapture").value(true))
                .andExpect(jsonPath("$.logoPng").value(nullValue()));
    }

    @Test
    @DisplayName("User độc lập không thuộc đại lý → 200 bản mặc định Pcut (BR-30)")
    void noDealer_ReturnsPcutDefault() throws Exception {
        userDeviceService.bindOnLogin(workerNoDealer, ctx("may-3"), "sid-br-3");

        mockMvc.perform(get("/api/v1/branding").with(asUser("kc-br-c", "br_worker_c", "sid-br-3")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Pcut"))
                .andExpect(jsonPath("$.theme").value("system"))
                .andExpect(jsonPath("$.logoPng").value(nullValue()));
    }

    @Test
    @DisplayName("Trường cấu hình NULL/sai trong DB → rơi về mặc định, không lỗi (BR-13)")
    void partialConfig_FallsBackPerField() throws Exception {
        Dealer dealer = dealerRepository.save(Dealer.builder()
                .code("DL-PART").name("Dealer Partial").status("ACTIVE").build());
        DealerBranding b = new DealerBranding();
        b.setDealer(dealer);
        b.setDisplayName("Chi Co Ten");
        // theme null, primaryColor null, công tắc null → phải ra mặc định
        dealerBrandingRepository.save(b);
        User worker = userRepository.save(User.builder()
                .username("br_worker_d").email("br_d@t.vn").keycloakUserId("kc-br-d")
                .role(Role.USER).dealer(dealer).enabled(true).build());
        userDeviceService.bindOnLogin(worker, ctx("may-4"), "sid-br-4");

        mockMvc.perform(get("/api/v1/branding").with(asUser("kc-br-d", "br_worker_d", "sid-br-4")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Chi Co Ten"))
                .andExpect(jsonPath("$.primaryColor").value("#2563C9"))
                .andExpect(jsonPath("$.theme").value("system"))
                .andExpect(jsonPath("$.allowWorkerThemeToggle").value(false))
                .andExpect(jsonPath("$.showDealerNameNextToLogo").value(true))
                .andExpect(jsonPath("$.protectScreenCapture").value(true))
                .andExpect(jsonPath("$.logoPng").value(nullValue()));
    }

    @Test
    @DisplayName("Không có token → 401")
    void noToken_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/branding"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token có sid không gắn máy ACTIVE (máy bị gỡ) → 401 SESSION_REVOKED")
    void revokedDevice_SessionRevoked() throws Exception {
        mockMvc.perform(get("/api/v1/branding").with(asUser("kc-br-a", "br_worker_a", "sid-linh-tinh")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.SESSION_REVOKED));
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private static DeviceContext ctx(String deviceId) {
        return new DeviceContext(deviceId, null, null, null);
    }

    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor
    asUser(String kcId, String username, String sid) {
        return jwt().jwt(j -> j.subject(kcId).claim("preferred_username", username).claim("sid", sid))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
