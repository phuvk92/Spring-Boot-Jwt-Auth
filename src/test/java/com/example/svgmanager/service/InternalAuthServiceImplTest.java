package com.example.svgmanager.service;

import com.example.svgmanager.dto.internal.InternalChangePasswordRequest;
import com.example.svgmanager.dto.internal.InternalChangePasswordResponse;
import com.example.svgmanager.dto.internal.InternalLoginRequest;
import com.example.svgmanager.dto.internal.InternalLoginResponse;
import com.example.svgmanager.dto.internal.InternalLogoutRequest;
import com.example.svgmanager.dto.internal.InternalLogoutResponse;
import com.example.svgmanager.dto.internal.InternalRefreshTokenRequest;
import com.example.svgmanager.dto.internal.InternalUserResponse;
import com.example.svgmanager.entity.Dealer;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.exception.ServiceUnavailableException;
import com.example.svgmanager.exception.UnauthorizedException;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.impl.InternalAuthServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InternalAuthServiceImplTest {

    private static final String TOKEN_URI = "http://localhost:8180/realms/cutting/protocol/openid-connect/token";
    private static final String LOGOUT_URI = "http://localhost:8180/realms/cutting/protocol/openid-connect/logout";
    private static final String CLIENT_ID = "cutting-manager-web";

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeycloakUserService keycloakUserService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private UserDeviceService userDeviceService;

    private ObjectMapper objectMapper;
    private MockRestServiceServer mockServer;
    private InternalAuthServiceImpl internalAuthService;

    private User validUser;
    private Dealer sampleDealer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        internalAuthService = new InternalAuthServiceImpl(
                userRepository,
                keycloakUserService,
                currentUserService,
                auditLogService,
                userDeviceService,
                objectMapper,
                restClient,
                TOKEN_URI,
                LOGOUT_URI,
                CLIENT_ID
        );

        sampleDealer = Dealer.builder()
                .id(20L)
                .code("DL-01")
                .name("Test Dealer")
                .status("ACTIVE")
                .build();

        validUser = User.builder()
                .id(1001L)
                .username("dealer_user01")
                .email("user01@abc.com")
                .fullName("Nguyen Van A")
                .keycloakUserId("kc-user-uuid-1001")
                .role(Role.USER)
                .dealer(sampleDealer)
                .enabled(true)
                .deleted(false)
                .build();
    }

    @Test
    @DisplayName("Login with valid username returns tokens and user info")
    void testLogin_ValidCredentials_Success() {
        when(userRepository.findByEmail("dealer_user01")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("dealer_user01")).thenReturn(Optional.of(validUser));

        mockServer.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"mock_acc\",\"refresh_token\":\"mock_ref\",\"expires_in\":3600,\"token_type\":\"Bearer\"}", MediaType.APPLICATION_JSON));

        InternalLoginRequest request = new InternalLoginRequest("dealer_user01", "Password123!");

        InternalLoginResponse response = internalAuthService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("mock_acc");
        assertThat(response.refreshToken()).isEqualTo("mock_ref");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.user()).isNotNull();
        assertThat(response.user().username()).isEqualTo("dealer_user01");
        assertThat(response.user().dealerId()).isEqualTo(20L);

        verify(auditLogService).log(eq("dealer_user01"), eq("USER"), eq("LOGIN_SUCCESS"), eq("User"), eq(1001L), any());
    }

    @Test
    @DisplayName("Login with valid Gmail address returns tokens and user info")
    void testLogin_ValidEmail_Success() {
        when(userRepository.findByEmail("user01@abc.com")).thenReturn(Optional.of(validUser));

        mockServer.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"mock_acc\",\"refresh_token\":\"mock_ref\",\"expires_in\":3600,\"token_type\":\"Bearer\"}", MediaType.APPLICATION_JSON));

        InternalLoginRequest request = new InternalLoginRequest("user01@abc.com", "Password123!");

        InternalLoginResponse response = internalAuthService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("mock_acc");
        assertThat(response.user().username()).isEqualTo("dealer_user01");
    }

    @Test
    @DisplayName("Login with unknown username throws 401 Unauthorized")
    void testLogin_UnknownUser_ThrowsUnauthorized() {
        when(userRepository.findByEmail("unknown_user")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("unknown_user")).thenReturn(Optional.empty());

        InternalLoginRequest request = new InternalLoginRequest("unknown_user", "Pass123!");

        assertThatThrownBy(() -> internalAuthService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    @DisplayName("Login with disabled user throws 401 Unauthorized")
    void testLogin_DisabledUser_ThrowsUnauthorized() {
        validUser.setEnabled(false);
        when(userRepository.findByEmail("dealer_user01")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("dealer_user01")).thenReturn(Optional.of(validUser));

        InternalLoginRequest request = new InternalLoginRequest("dealer_user01", "Pass123!");

        assertThatThrownBy(() -> internalAuthService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    @DisplayName("Login with deleted user throws 401 Unauthorized")
    void testLogin_DeletedUser_ThrowsUnauthorized() {
        validUser.setDeleted(true);
        when(userRepository.findByEmail("dealer_user01")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("dealer_user01")).thenReturn(Optional.of(validUser));

        InternalLoginRequest request = new InternalLoginRequest("dealer_user01", "Pass123!");

        assertThatThrownBy(() -> internalAuthService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    @DisplayName("Login with unlinked Keycloak user throws 401 Unauthorized")
    void testLogin_UnlinkedUser_ThrowsUnauthorized() {
        validUser.setKeycloakUserId(null);
        when(userRepository.findByEmail("dealer_user01")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("dealer_user01")).thenReturn(Optional.of(validUser));

        InternalLoginRequest request = new InternalLoginRequest("dealer_user01", "Pass123!");

        assertThatThrownBy(() -> internalAuthService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    @DisplayName("Login with incorrect password throws 401 Unauthorized")
    void testLogin_WrongPassword_ThrowsUnauthorized() {
        when(userRepository.findByEmail("dealer_user01")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("dealer_user01")).thenReturn(Optional.of(validUser));

        mockServer.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        InternalLoginRequest request = new InternalLoginRequest("dealer_user01", "WrongPass!");

        assertThatThrownBy(() -> internalAuthService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid username or password");

        verify(auditLogService).log(eq("dealer_user01"), eq("USER"), eq("LOGIN_FAILED"), eq("User"), eq(1001L), any());
    }

    @Test
    @DisplayName("Refresh token successfully returns new tokens")
    void testRefreshToken_Success() {
        String sub = "kc-user-uuid-1001";
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"sub\":\"" + sub + "\",\"preferred_username\":\"dealer_user01\"}").getBytes(StandardCharsets.UTF_8));
        String validJwt = "header." + payload + ".sig";

        when(userRepository.findByKeycloakUserId(sub)).thenReturn(Optional.of(validUser));

        mockServer.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"" + validJwt + "\",\"refresh_token\":\"new_ref_token\",\"expires_in\":3600,\"token_type\":\"Bearer\"}", MediaType.APPLICATION_JSON));

        InternalRefreshTokenRequest request = new InternalRefreshTokenRequest("old_refresh_token");

        InternalLoginResponse response = internalAuthService.refreshToken(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo(validJwt);
        assertThat(response.refreshToken()).isEqualTo("new_ref_token");
        assertThat(response.user().username()).isEqualTo("dealer_user01");

        verify(auditLogService).log(eq("dealer_user01"), eq("USER"), eq("TOKEN_REFRESH"), eq("User"), eq(1001L), any());
    }

    @Test
    @DisplayName("Refresh token with invalid token throws 401 Unauthorized")
    void testRefreshToken_InvalidToken_ThrowsUnauthorized() {
        mockServer.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        InternalRefreshTokenRequest request = new InternalRefreshTokenRequest("bad_token");

        assertThatThrownBy(() -> internalAuthService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid or expired refresh token");
    }

    @Test
    @DisplayName("Logout revokes token and records audit log")
    void testLogout_Success() {
        Jwt mockJwt = Jwt.withTokenValue("mock.jwt.token")
                .header("alg", "none")
                .claim("preferred_username", "dealer_user01")
                .claim("sub", "kc-user-uuid-1001")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(currentUserService.getCurrentJwt()).thenReturn(Optional.of(mockJwt));
        when(currentUserService.getCurrentUser()).thenReturn(validUser);

        mockServer.expect(requestTo(LOGOUT_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        InternalLogoutRequest request = new InternalLogoutRequest("valid_refresh_token");

        InternalLogoutResponse response = internalAuthService.logout(request);

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Logged out successfully");

        verify(auditLogService).log(eq("dealer_user01"), eq("USER"), eq("LOGOUT"), eq("User"), eq(1001L), any());
    }

    @Test
    @DisplayName("Logout handles Keycloak 400 gracefully (already expired token)")
    void testLogout_KeycloakExpiredToken_StillSucceeds() {
        when(currentUserService.getCurrentJwt()).thenReturn(Optional.empty());

        mockServer.expect(requestTo(LOGOUT_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        InternalLogoutRequest request = new InternalLogoutRequest("expired_refresh_token");

        InternalLogoutResponse response = internalAuthService.logout(request);

        assertThat(response.success()).isTrue();
    }

    @Test
    @DisplayName("Change password verifies current password and resets password via Keycloak")
    void testChangePassword_Success() {
        when(currentUserService.getCurrentUser()).thenReturn(validUser);

        mockServer.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"verified_ok\"}", MediaType.APPLICATION_JSON));

        InternalChangePasswordRequest request = new InternalChangePasswordRequest(
                "CurrentPass123!",
                "NewPass456!",
                "NewPass456!"
        );

        InternalChangePasswordResponse response = internalAuthService.changePassword(request);

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Password changed successfully");

        verify(keycloakUserService).resetPassword("kc-user-uuid-1001", "NewPass456!");
        verify(auditLogService).log(eq("dealer_user01"), eq("USER"), eq("PASSWORD_CHANGED"), eq("User"), eq(1001L), any());
    }

    @Test
    @DisplayName("Change password rejects when new password and confirm password mismatch")
    void testChangePassword_MismatchNewPassword_ThrowsBadRequest() {
        InternalChangePasswordRequest request = new InternalChangePasswordRequest(
                "CurrentPass123!",
                "NewPass456!",
                "DifferentPass789!"
        );

        assertThatThrownBy(() -> internalAuthService.changePassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Confirm password does not match new password");

        verifyNoInteractions(keycloakUserService);
    }

    @Test
    @DisplayName("Change password rejects when new password equals current password")
    void testChangePassword_SameNewPassword_ThrowsBadRequest() {
        InternalChangePasswordRequest request = new InternalChangePasswordRequest(
                "CurrentPass123!",
                "CurrentPass123!",
                "CurrentPass123!"
        );

        assertThatThrownBy(() -> internalAuthService.changePassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("New password must be different from current password");

        verifyNoInteractions(keycloakUserService);
    }

    @Test
    @DisplayName("Change password rejects when current password is wrong in Keycloak")
    void testChangePassword_IncorrectCurrentPassword_ThrowsBadRequest() {
        when(currentUserService.getCurrentUser()).thenReturn(validUser);

        mockServer.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        InternalChangePasswordRequest request = new InternalChangePasswordRequest(
                "WrongCurrentPass123!",
                "NewPass456!",
                "NewPass456!"
        );

        assertThatThrownBy(() -> internalAuthService.changePassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Current password is incorrect");

        verifyNoInteractions(keycloakUserService);
    }

    @Test
    @DisplayName("Login with device and ipAddress includes client info in audit log")
    void testLogin_WithDeviceAndIp_IncludesInAuditLog() {
        when(userRepository.findByEmail("dealer_user01")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("dealer_user01")).thenReturn(Optional.of(validUser));

        mockServer.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"mock_acc\",\"refresh_token\":\"mock_ref\",\"expires_in\":3600,\"token_type\":\"Bearer\"}", MediaType.APPLICATION_JSON));

        InternalLoginRequest request = new InternalLoginRequest(
                "dealer_user01",
                "Password123!",
                "Roland-Cutter-01",
                "192.168.1.50"
        );

        InternalLoginResponse response = internalAuthService.login(request);

        assertThat(response).isNotNull();
        verify(auditLogService).log(
                eq("dealer_user01"),
                eq("USER"),
                eq("LOGIN_SUCCESS"),
                eq("User"),
                eq(1001L),
                contains("Thiết bị: Roland-Cutter-01, IP: 192.168.1.50")
        );
    }

    @Test
    @DisplayName("JSON deserialization with aliases (thietBi, diaChiIp) populates device and ipAddress")
    void testJsonDeserialization_WithAliases() throws Exception {
        String json = "{\"username\":\"user@test.com\",\"password\":\"Pass123!\",\"thietBi\":\"Graphtec-CE7000\",\"diaChiIp\":\"10.0.0.5\"}";
        InternalLoginRequest loginReq = objectMapper.readValue(json, InternalLoginRequest.class);

        assertThat(loginReq.username()).isEqualTo("user@test.com");
        assertThat(loginReq.password()).isEqualTo("Pass123!");
        assertThat(loginReq.device()).isEqualTo("Graphtec-CE7000");
        assertThat(loginReq.ipAddress()).isEqualTo("10.0.0.5");

        String refreshJson = "{\"refreshToken\":\"mock_token\",\"device_name\":\"Graphtec-CE7000\",\"ip_address\":\"10.0.0.5\"}";
        InternalRefreshTokenRequest refReq = objectMapper.readValue(refreshJson, InternalRefreshTokenRequest.class);
        assertThat(refReq.device()).isEqualTo("Graphtec-CE7000");
        assertThat(refReq.ipAddress()).isEqualTo("10.0.0.5");
    }

    // ── F-57: giới hạn thiết bị (SA-GioiHanThietBi) ────────────────────────

    private static String fakeToken(String sid) {
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        return enc.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8)) + "."
                + enc.encodeToString(("{\"sid\":\"" + sid + "\"}").getBytes(StandardCharsets.UTF_8)) + ".sig";
    }

    private void givenKnownUser() {
        when(userRepository.findByEmail("dealer_user01")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("dealer_user01")).thenReturn(Optional.of(validUser));
    }

    @Test
    @DisplayName("F-57: bật kiểm máy mà thiếu device → 400 DEVICE_REQUIRED, không gọi Keycloak")
    void testLogin_Enforced_MissingDevice_Rejected() {
        when(userDeviceService.isEnforced()).thenReturn(true);

        assertThatThrownBy(() -> internalAuthService.login(new InternalLoginRequest("dealer_user01", "Password123!")))
                .isInstanceOf(BadRequestException.class)
                .extracting(e -> ((BadRequestException) e).getCode())
                .isEqualTo(ErrorCodes.DEVICE_REQUIRED);
        mockServer.verify();   // không có request nào tới Keycloak
    }

    @Test
    @DisplayName("F-57: đăng nhập gắn phiên (sid trong token) vào máy")
    void testLogin_Enforced_BindsSessionToDevice() {
        when(userDeviceService.isEnforced()).thenReturn(true);
        givenKnownUser();
        String access = fakeToken("sid-123");
        mockServer.expect(requestTo(TOKEN_URI)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"" + access + "\",\"refresh_token\":\"" + fakeToken("sid-123")
                        + "\",\"expires_in\":300,\"token_type\":\"Bearer\"}", MediaType.APPLICATION_JSON));

        internalAuthService.login(new InternalLoginRequest("dealer_user01", "Password123!", "may-A", "10.0.0.5", "XUONG-01", "Windows"));

        verify(userDeviceService).bindOnLogin(eq(validUser),
                eq(new UserDeviceService.DeviceContext("may-A", "XUONG-01", "Windows", "10.0.0.5")), eq("sid-123"));
        mockServer.verify();
    }

    @Test
    @DisplayName("F-57: vượt giới hạn máy → huỷ phiên Keycloak vừa tạo rồi báo SESSION_LIMIT")
    void testLogin_Enforced_SessionLimit_EndsFreshSession() {
        when(userDeviceService.isEnforced()).thenReturn(true);
        givenKnownUser();
        String refresh = fakeToken("sid-new");
        mockServer.expect(requestTo(TOKEN_URI)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"" + fakeToken("sid-new") + "\",\"refresh_token\":\"" + refresh
                        + "\",\"expires_in\":300}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(LOGOUT_URI)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));
        doThrow(new ForbiddenException("limit", ErrorCodes.SESSION_LIMIT))
                .when(userDeviceService).bindOnLogin(any(), any(), eq("sid-new"));

        assertThatThrownBy(() -> internalAuthService.login(new InternalLoginRequest("dealer_user01", "Password123!", "may-B", null)))
                .isInstanceOf(ForbiddenException.class)
                .extracting(e -> ((ForbiddenException) e).getCode())
                .isEqualTo(ErrorCodes.SESSION_LIMIT);
        mockServer.verify();   // đã gọi logout
    }

    @Test
    @DisplayName("F-57: refresh từ máy bị gỡ → chấm dứt phiên, 401 SESSION_REVOKED, không xin token mới")
    void testRefresh_Enforced_RevokedDevice() {
        when(userDeviceService.isEnforced()).thenReturn(true);
        String refresh = fakeToken("sid-A");
        doThrow(new UnauthorizedException("revoked", ErrorCodes.SESSION_REVOKED))
                .when(userDeviceService).verifyOnRefresh("may-A", "sid-A");
        mockServer.expect(requestTo(LOGOUT_URI)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertThatThrownBy(() -> internalAuthService.refreshToken(new InternalRefreshTokenRequest(refresh, "may-A", null)))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(e -> ((UnauthorizedException) e).getCode())
                .isEqualTo(ErrorCodes.SESSION_REVOKED);
        mockServer.verify();   // chỉ logout, không có lượt xin token
    }

    @Test
    @DisplayName("F-57: refresh thiếu device khi bật kiểm máy → 400 DEVICE_REQUIRED")
    void testRefresh_Enforced_MissingDevice() {
        when(userDeviceService.isEnforced()).thenReturn(true);

        assertThatThrownBy(() -> internalAuthService.refreshToken(new InternalRefreshTokenRequest(fakeToken("sid-A"))))
                .isInstanceOf(BadRequestException.class)
                .extracting(e -> ((BadRequestException) e).getCode())
                .isEqualTo(ErrorCodes.DEVICE_REQUIRED);
    }

    @Test
    @DisplayName("S1: Keycloak lỗi 5xx khi đăng nhập → 503, không phải 401")
    void testLogin_KeycloakDown_Returns503() {
        givenKnownUser();
        mockServer.expect(requestTo(TOKEN_URI)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> internalAuthService.login(new InternalLoginRequest("dealer_user01", "Password123!")))
                .isInstanceOf(ServiceUnavailableException.class)
                .extracting(e -> ((ServiceUnavailableException) e).getCode())
                .isEqualTo(ErrorCodes.AUTH_SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("S1: Keycloak lỗi khi refresh → 503, không bị coi là phiên chết")
    void testRefresh_KeycloakDown_Returns503() {
        mockServer.expect(requestTo(TOKEN_URI)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> internalAuthService.refreshToken(new InternalRefreshTokenRequest("any_refresh")))
                .isInstanceOf(ServiceUnavailableException.class);
    }
}
