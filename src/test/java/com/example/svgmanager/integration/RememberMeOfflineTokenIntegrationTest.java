package com.example.svgmanager.integration;

import com.example.svgmanager.dto.internal.InternalChangePasswordRequest;
import com.example.svgmanager.dto.internal.InternalLoginRequest;
import com.example.svgmanager.dto.internal.InternalLoginResponse;
import com.example.svgmanager.dto.internal.InternalLogoutRequest;
import com.example.svgmanager.dto.internal.InternalRefreshTokenRequest;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.UnauthorizedException;
import com.example.svgmanager.repository.UserDeviceRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.KeycloakUserService;
import com.example.svgmanager.service.UserDeviceService;
import com.example.svgmanager.service.impl.InternalAuthServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * F-01 — ghi nhớ đăng nhập bằng offline token (SA-GhiNhoDangNhap §2.1).
 * Keycloak được GIẢ LẬP bằng {@link MockRestServiceServer} trên token/revoke endpoint,
 * giống lối {@code InternalAuthServiceImplTest}; DB (H2) và UserDeviceService là thật.
 * Không có Keycloak thật trong môi trường test — phần sid offline của Keycloak thật cần
 * kiểm lại một lần khi deploy (ghi trong comment kết thúc issue).
 * Lưu ý: MockRestServiceServer không cho thêm expectation sau request đầu tiên,
 * nên mỗi ca khai báo TRƯỚC toàn bộ lượt gọi Keycloak mong đợi.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.device.enforce=true",
        "app.device.max-per-user=3",
        "app.device.session-cache-seconds=0"
})
@Transactional
class RememberMeOfflineTokenIntegrationTest {

    private static final String TOKEN_URI = "http://keycloak.test/realms/cutting/protocol/openid-connect/token";
    private static final String LOGOUT_URI = "http://keycloak.test/realms/cutting/protocol/openid-connect/logout";
    private static final String REVOKE_URI = "http://keycloak.test/realms/cutting/protocol/openid-connect/revoke";
    private static final String CLIENT_ID = "cutting-manager-web";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @Autowired
    private UserDeviceService userDeviceService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KeycloakUserService keycloakUserService;

    private CurrentUserService currentUserService;
    private MockRestServiceServer mockServer;
    private InternalAuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        currentUserService = mock(CurrentUserService.class);
        authService = new InternalAuthServiceImpl(
                userRepository, keycloakUserService, currentUserService, auditLogService,
                userDeviceService, objectMapper, builder.build(), TOKEN_URI, LOGOUT_URI, CLIENT_ID);

        user = userRepository.save(User.builder()
                .username("tho_ghi_nho")
                .email("remember@example.com")
                .fullName("Tho Ghi Nho")
                .keycloakUserId("kc-remember-1")
                .role(Role.USER)
                .enabled(true)
                .deleted(false)
                .build());
    }

    private static String fakeToken(String sid, String sub) {
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        return enc.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8)) + "."
                + enc.encodeToString(("{\"sid\":\"" + sid + "\",\"sub\":\"" + sub
                        + "\",\"preferred_username\":\"tho_ghi_nho\"}").getBytes(StandardCharsets.UTF_8))
                + ".sig";
    }

    private static String tokenJson(String sid) {
        return "{\"access_token\":\"" + fakeToken(sid, "kc-remember-1")
                + "\",\"refresh_token\":\"" + fakeToken(sid, "kc-remember-1")
                + "\",\"expires_in\":300,\"refresh_expires_in\":0,\"token_type\":\"Bearer\"}";
    }

    /** Lượt login password-grant PHẢI kèm scope=offline_access (rememberMe=true). */
    private void expectTokenOfflineLogin(String sid) {
        mockServer.expect(requestTo(TOKEN_URI)).andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("scope=offline_access")))
                .andRespond(withSuccess(tokenJson(sid), MediaType.APPLICATION_JSON));
    }

    /** Lượt gọi token endpoint bất kỳ (refresh grant hoặc login thường — kiểm scope riêng nếu cần). */
    private void expectTokenAny(String bodyJson) {
        mockServer.expect(requestTo(TOKEN_URI)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(bodyJson, MediaType.APPLICATION_JSON));
    }

    private void expectRevoke() {
        mockServer.expect(requestTo(REVOKE_URI)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));
    }

    private InternalLoginResponse loginRemembered(String device) {
        return authService.login(new InternalLoginRequest(
                "tho_ghi_nho", "Pass123!", device, "10.0.0.1", "MAY", "Windows", true));
    }

    // ── AC1 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AC1: rememberMe → gửi scope=offline_access, response có refreshExpiresIn; mặc định thì không gửi scope")
    void ac1_login_rememberMe_offlineScope() {
        expectTokenOfflineLogin("sid-A");
        mockServer.expect(requestTo(TOKEN_URI)).andExpect(method(HttpMethod.POST))
                .andExpect(content().string(not(containsString("offline_access"))))
                .andRespond(withSuccess("{\"access_token\":\"" + fakeToken("sid-A2", "kc-remember-1")
                        + "\",\"refresh_token\":\"r\",\"expires_in\":300,\"refresh_expires_in\":1800}", MediaType.APPLICATION_JSON));

        InternalLoginResponse res = loginRemembered("may-A");
        assertThat(res.refreshExpiresIn()).isEqualTo(0L);
        assertThat(userDeviceRepository.findByKeycloakSessionId("sid-A")).isPresent();

        InternalLoginResponse res2 = authService.login(
                new InternalLoginRequest("tho_ghi_nho", "Pass123!", "may-A", "10.0.0.1"));
        assertThat(res2.refreshExpiresIn()).isEqualTo(1800L);
        mockServer.verify();
    }

    // ── AC2 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AC2: refresh bằng offline token (sid khớp bản ghi) → 200; sai device → 401 SESSION_REVOKED")
    void ac2_refresh_offlineToken_verifyDevice() {
        expectTokenOfflineLogin("sid-A");
        expectTokenAny(tokenJson("sid-A"));
        expectRevoke(); // sai máy → verifyOnRefresh chặn, service vẫn cố thu hồi token phía Keycloak

        loginRemembered("may-A");
        String offlineRefresh = fakeToken("sid-A", "kc-remember-1");

        InternalLoginResponse res = authService.refreshToken(
                new InternalRefreshTokenRequest(offlineRefresh, "may-A", "10.0.0.1"));
        assertThat(res.refreshExpiresIn()).isEqualTo(0L);

        assertThatThrownBy(() -> authService.refreshToken(
                new InternalRefreshTokenRequest(offlineRefresh, "may-KHAC", "10.0.0.9")))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(e -> ((UnauthorizedException) e).getCode())
                .isEqualTo(ErrorCodes.SESSION_REVOKED);
        mockServer.verify(); // lượt refresh lỗi không chạm token endpoint
    }

    // ── AC3 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AC3: logout gọi revocation endpoint (token + token_type_hint); refresh sau đó → 401 SESSION_REVOKED")
    void ac3_logout_revokesOfflineToken() {
        expectTokenOfflineLogin("sid-A");
        mockServer.expect(requestTo(REVOKE_URI)).andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("token_type_hint=refresh_token")))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));
        expectRevoke(); // lượt refresh sau logout bị chặn vẫn cố thu hồi phía Keycloak

        loginRemembered("may-A");
        String offlineRefresh = fakeToken("sid-A", "kc-remember-1");

        authService.logout(new InternalLogoutRequest(offlineRefresh, "may-A", "10.0.0.1"));

        // Liên kết phiên đã bỏ → verifyOnRefresh chặn ngay, không cần Keycloak từ chối
        assertThatThrownBy(() -> authService.refreshToken(
                new InternalRefreshTokenRequest(offlineRefresh, "may-A", "10.0.0.1")))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(e -> ((UnauthorizedException) e).getCode())
                .isEqualTo(ErrorCodes.SESSION_REVOKED);
        mockServer.verify();
    }

    @Test
    @DisplayName("AC3: gỡ máy → offline token của máy đó refresh → 401 SESSION_REVOKED")
    void ac3_revokeDevice_killsOfflineToken() {
        expectTokenOfflineLogin("sid-A");
        expectRevoke(); // refresh sau gỡ máy bị chặn vẫn cố thu hồi phía Keycloak

        loginRemembered("may-A");
        String offlineRefresh = fakeToken("sid-A", "kc-remember-1");

        Long registrationId = userDeviceRepository.findByKeycloakSessionId("sid-A").orElseThrow().getId();
        userDeviceService.revoke(user, registrationId, "admin_test", "ADMIN");

        assertThatThrownBy(() -> authService.refreshToken(
                new InternalRefreshTokenRequest(offlineRefresh, "may-A", "10.0.0.1")))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(e -> ((UnauthorizedException) e).getCode())
                .isEqualTo(ErrorCodes.SESSION_REVOKED);
        verify(keycloakUserService).deleteSession("sid-A");
        mockServer.verify();
    }

    // ── AC4 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AC4: đổi mật khẩu → offline session của máy khác bị thu hồi, phiên đang đổi giữ nguyên")
    void ac4_changePassword_revokesOtherOfflineSessions() {
        expectTokenOfflineLogin("sid-A");
        expectTokenOfflineLogin("sid-B");
        expectTokenAny("{\"access_token\":\"ok\"}"); // lượt verify mật khẩu hiện tại
        expectRevoke(); // refresh của máy B sau khi bị thu hồi vẫn cố thu hồi phía Keycloak

        loginRemembered("may-A");
        loginRemembered("may-B");

        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "none")
                .claim("sid", "sid-A")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(currentUserService.getCurrentJwt()).thenReturn(Optional.of(jwt));

        authService.changePassword(new InternalChangePasswordRequest("Pass123!", "Moi456!", "Moi456!"));

        // Máy B: liên kết phiên bị gỡ + Keycloak xoá session → refresh → 401
        assertThat(userDeviceRepository.findByKeycloakSessionId("sid-B")).isEmpty();
        verify(keycloakUserService).resetPassword("kc-remember-1", "Moi456!");
        verify(keycloakUserService).deleteSession("sid-B");
        assertThat(userDeviceRepository.findByKeycloakSessionId("sid-A")).isPresent();

        String offlineRefreshB = fakeToken("sid-B", "kc-remember-1");
        assertThatThrownBy(() -> authService.refreshToken(
                new InternalRefreshTokenRequest(offlineRefreshB, "may-B", "10.0.0.2")))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(e -> ((UnauthorizedException) e).getCode())
                .isEqualTo(ErrorCodes.SESSION_REVOKED);
        mockServer.verify();
    }
}
