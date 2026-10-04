package com.example.svgmanager.service;

import com.example.svgmanager.dto.internal.InternalLoginRequest;
import com.example.svgmanager.dto.internal.InternalLoginResponse;
import com.example.svgmanager.dto.request.CreateUserRequest;
import com.example.svgmanager.dto.request.LoginRequest;
import com.example.svgmanager.dto.request.UpdateUserRequest;
import com.example.svgmanager.dto.response.AuthResponse;
import com.example.svgmanager.dto.response.UserResponse;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.UserAccountExpiredException;
import com.example.svgmanager.mapper.UserMapper;
import com.example.svgmanager.repository.DealerRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.impl.AuthServiceImpl;
import com.example.svgmanager.service.impl.InternalAuthServiceImpl;
import com.example.svgmanager.service.impl.UserServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserExpirationTest {

    private static final String TOKEN_URI = "http://kc/realms/cutting/protocol/openid-connect/token";

    @Mock
    private UserRepository userRepository;

    @Mock
    private DealerRepository dealerRepository;

    @Mock
    private SvgFileRepository svgFileRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private KeycloakUserService keycloakUserService;

    @Mock
    private UserDeviceService userDeviceService;

    @Mock
    private AuditLogService auditLogService;

    private UserMapper userMapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        objectMapper = new ObjectMapper();
    }

    // ─────────────────────────────────────────────────────────────
    // 1. Entity Business Rule Tests: User.isExpired()
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ADMIN với ngày hết hạn trong quá khứ -> isExpired() vẫn là FALSE (bỏ qua hết hạn)")
    void admin_ExpiredDateInPast_NeverExpired() {
        User admin = User.builder()
                .role(Role.ADMIN)
                .expirationDate(LocalDate.of(2020, 1, 1))
                .build();

        LocalDate today = LocalDate.of(2026, 10, 4);
        assertThat(admin.isExpired(today)).isFalse();
        assertThat(admin.isExpired()).isFalse();
    }

    @Test
    @DisplayName("USER không có ngày hết hạn (null) -> isExpired() là FALSE")
    void user_NullExpiration_NotExpired() {
        User user = User.builder()
                .role(Role.USER)
                .expirationDate(null)
                .build();

        LocalDate today = LocalDate.of(2026, 10, 4);
        assertThat(user.isExpired(today)).isFalse();
    }

    @Test
    @DisplayName("USER còn hạn (expirationDate > today) -> isExpired() là FALSE")
    void user_FutureExpiration_NotExpired() {
        User user = User.builder()
                .role(Role.USER)
                .expirationDate(LocalDate.of(2026, 12, 31))
                .build();

        LocalDate today = LocalDate.of(2026, 10, 4);
        assertThat(user.isExpired(today)).isFalse();
    }

    @Test
    @DisplayName("USER đúng ngày hết hạn (expirationDate == today) -> được dùng đến hết ngày, isExpired() là FALSE")
    void user_ExpirationEqualsToday_NotExpired() {
        LocalDate today = LocalDate.of(2026, 10, 4);
        User user = User.builder()
                .role(Role.USER)
                .expirationDate(today)
                .build();

        assertThat(user.isExpired(today)).isFalse();
    }

    @Test
    @DisplayName("USER đã hết hạn (expirationDate < today) -> isExpired() là TRUE")
    void user_ExpirationInPast_IsExpired() {
        LocalDate today = LocalDate.of(2026, 10, 4);
        User user = User.builder()
                .role(Role.USER)
                .expirationDate(LocalDate.of(2026, 10, 3))
                .build();

        assertThat(user.isExpired(today)).isTrue();
    }

    @Test
    @DisplayName("AGENT với ngày hết hạn trong quá khứ -> isExpired() là TRUE")
    void agent_ExpirationInPast_IsExpired() {
        LocalDate today = LocalDate.of(2026, 10, 4);
        User agent = User.builder()
                .role(Role.AGENT)
                .expirationDate(LocalDate.of(2026, 9, 30))
                .build();

        assertThat(agent.isExpired(today)).isTrue();
    }

    // ─────────────────────────────────────────────────────────────
    // 2. Web Admin Login Tests: AuthServiceImpl
    // ─────────────────────────────────────────────────────────────

    private static String fakeJwt(String username, String role) {
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        String payload = "{\"sub\":\"kc-" + username + "\",\"preferred_username\":\"" + username + "\","
                + "\"realm_access\":{\"roles\":[\"" + role + "\"]}}";
        return enc.encodeToString("{}".getBytes(StandardCharsets.UTF_8)) + "."
                + enc.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + ".sig";
    }

    @Test
    @DisplayName("Web Login: AGENT đã hết hạn -> chặn trước khi gọi Keycloak, trả 401 USER_ACCOUNT_EXPIRED và ghi AuditLog")
    void webLogin_ExpiredAgent_BlockedWithAuditLog() {
        AuthServiceImpl authService = new AuthServiceImpl(
                userRepository, userMapper, currentUserService, objectMapper, auditLogService
        );
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(authService, "restClient", builder.build());
        ReflectionTestUtils.setField(authService, "tokenUri", TOKEN_URI);
        ReflectionTestUtils.setField(authService, "clientId", "cutting-manager-web");

        User expiredAgent = User.builder()
                .id(10L)
                .username("agent_old")
                .email("agent_old@test.com")
                .role(Role.AGENT)
                .expirationDate(LocalDate.now().minusDays(1))
                .enabled(true)
                .build();

        when(userRepository.findByUsername("agent_old")).thenReturn(Optional.of(expiredAgent));

        LoginRequest req = new LoginRequest();
        req.setUsername("agent_old");
        req.setPassword("Password123!");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UserAccountExpiredException.class)
                .extracting(e -> ((UserAccountExpiredException) e).getCode())
                .isEqualTo(ErrorCodes.USER_ACCOUNT_EXPIRED);

        verify(auditLogService).log(
                eq("agent_old"),
                eq("AGENT"),
                eq("USER_LOGIN_BLOCKED_EXPIRED"),
                eq("User"),
                eq(10L),
                contains("reason=ACCOUNT_EXPIRED")
        );
        mockServer.verify(); // Không hề gửi request tới Keycloak
    }

    @Test
    @DisplayName("Web Login: ADMIN với expirationDate trong quá khứ -> VẪN ĐƯỢC LOGIN BÌNH THƯỜNG")
    void webLogin_AdminExpiredDate_AllowedToLogin() {
        AuthServiceImpl authService = new AuthServiceImpl(
                userRepository, userMapper, currentUserService, objectMapper, auditLogService
        );
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(authService, "restClient", builder.build());
        ReflectionTestUtils.setField(authService, "tokenUri", TOKEN_URI);
        ReflectionTestUtils.setField(authService, "clientId", "cutting-manager-web");

        User adminUser = User.builder()
                .id(1L)
                .username("admin")
                .email("admin@test.com")
                .keycloakUserId("kc-admin")
                .role(Role.ADMIN)
                .expirationDate(LocalDate.now().minusYears(2))
                .enabled(true)
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.findByKeycloakUserId("kc-admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        mockServer.expect(requestTo(TOKEN_URI)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"" + fakeJwt("admin", "ADMIN") + "\",\"refresh_token\":\"r\",\"expires_in\":300}",
                        MediaType.APPLICATION_JSON));

        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("Password123!");

        AuthResponse res = authService.login(req);
        assertThat(res).isNotNull();
        assertThat(res.getUser().getRole()).isEqualTo(Role.ADMIN);
        mockServer.verify();
    }

    // ─────────────────────────────────────────────────────────────
    // 3. Client App Login Tests: InternalAuthServiceImpl
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Client Login: USER đã hết hạn -> chặn trước khi gọi Keycloak, trả USER_ACCOUNT_EXPIRED và ghi AuditLog")
    void clientLogin_ExpiredUser_BlockedWithAuditLog() {
        InternalAuthServiceImpl internalAuthService = new InternalAuthServiceImpl(
                userRepository, keycloakUserService, currentUserService,
                auditLogService, userDeviceService, objectMapper,
                RestClient.builder().build(), TOKEN_URI, "http://kc/logout", "cutting-manager-web"
        );

        User expiredUser = User.builder()
                .id(20L)
                .username("worker01")
                .email("worker01@test.com")
                .keycloakUserId("kc-worker01")
                .role(Role.USER)
                .expirationDate(LocalDate.now().minusDays(5))
                .enabled(true)
                .build();

        when(userRepository.findByEmail("worker01")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("worker01")).thenReturn(Optional.of(expiredUser));

        InternalLoginRequest req = new InternalLoginRequest("worker01", "Password123!", "machine-01", "10.0.0.1");

        assertThatThrownBy(() -> internalAuthService.login(req))
                .isInstanceOf(UserAccountExpiredException.class)
                .extracting(e -> ((UserAccountExpiredException) e).getCode())
                .isEqualTo(ErrorCodes.USER_ACCOUNT_EXPIRED);

        verify(auditLogService).log(
                eq("worker01"),
                eq("USER"),
                eq("USER_LOGIN_BLOCKED_EXPIRED"),
                eq("User"),
                eq(20L),
                contains("reason=ACCOUNT_EXPIRED")
        );
    }

    @Test
    @DisplayName("Client Login: USER hạn là hôm nay (today) -> VẪN ĐƯỢC PHÉP ĐĂNG NHẬP")
    void clientLogin_UserExpiresToday_Allowed() {
        InternalAuthServiceImpl internalAuthService = new InternalAuthServiceImpl(
                userRepository, keycloakUserService, currentUserService,
                auditLogService, userDeviceService, objectMapper,
                RestClient.builder().build(), TOKEN_URI, "http://kc/logout", "cutting-manager-web"
        );
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(internalAuthService, "restClient", builder.build());

        User validTodayUser = User.builder()
                .id(21L)
                .username("worker_today")
                .email("worker_today@test.com")
                .keycloakUserId("kc-worker-today")
                .role(Role.USER)
                .expirationDate(LocalDate.now()) // Hôm nay
                .enabled(true)
                .build();

        when(userRepository.findByEmail("worker_today")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("worker_today")).thenReturn(Optional.of(validTodayUser));

        mockServer.expect(requestTo(TOKEN_URI)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"" + fakeJwt("worker_today", "USER") + "\",\"refresh_token\":\"r\",\"expires_in\":300}",
                        MediaType.APPLICATION_JSON));

        InternalLoginRequest req = new InternalLoginRequest("worker_today", "Password123!", "machine-01", "10.0.0.1");
        InternalLoginResponse res = internalAuthService.login(req);

        assertThat(res).isNotNull();
        assertThat(res.accessToken()).isNotNull();
        mockServer.verify();
    }

    // ─────────────────────────────────────────────────────────────
    // 4. UserServiceImpl Tests: Create & Update with Expiration Date
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("UserService: Tạo User có ngày hết hạn -> Lưu ngày và ghi AuditLog USER_EXPIRATION_DATE_CREATED")
    void createUser_WithExpirationDate_SavedAndAudited() {
        User adminOperator = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        when(currentUserService.getCurrentUser()).thenReturn(adminOperator);
        when(currentUserService.isAdmin()).thenReturn(true);

        when(keycloakUserService.createUser(any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn("kc-new-user");

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });

        UserServiceImpl userService = new UserServiceImpl(
                userRepository, dealerRepository, svgFileRepository,
                userMapper, currentUserService, keycloakUserService,
                userDeviceService, auditLogService
        );

        LocalDate exp = LocalDate.of(2026, 12, 31);
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("new_worker");
        req.setEmail("new_worker@test.com");
        req.setPassword("Password123!");
        req.setRole(Role.USER);
        req.setExpirationDate(exp);

        UserResponse response = userService.createUser(req);

        assertThat(response.getExpirationDate()).isEqualTo(exp);
        assertThat(response.isExpired()).isFalse();

        verify(auditLogService).log(
                eq("admin"),
                eq("ADMIN"),
                eq("USER_EXPIRATION_DATE_CREATED"),
                eq("User"),
                eq(99L),
                contains("newExpirationDate=2026-12-31")
        );
    }

    @Test
    @DisplayName("UserService: Sửa User gia hạn ngày mới -> ghi AuditLog USER_EXPIRATION_DATE_UPDATED")
    void updateUser_UpdateExpirationDate_Audited() {
        User adminOperator = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        when(currentUserService.getCurrentUser()).thenReturn(adminOperator);
        when(currentUserService.isAdmin()).thenReturn(true);

        LocalDate oldExp = LocalDate.of(2026, 10, 1);
        LocalDate newExp = LocalDate.of(2027, 1, 1);

        User existingUser = User.builder()
                .id(50L)
                .username("worker50")
                .email("worker50@test.com")
                .role(Role.USER)
                .expirationDate(oldExp)
                .enabled(true)
                .build();

        when(userRepository.findById(50L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserServiceImpl userService = new UserServiceImpl(
                userRepository, dealerRepository, svgFileRepository,
                userMapper, currentUserService, keycloakUserService,
                userDeviceService, auditLogService
        );

        UpdateUserRequest updateReq = new UpdateUserRequest();
        updateReq.setEmail("worker50@test.com");
        updateReq.setRole(Role.USER);
        updateReq.setEnabled(true);
        updateReq.setExpirationDate(newExp);

        UserResponse res = userService.updateUser(50L, updateReq);

        assertThat(res.getExpirationDate()).isEqualTo(newExp);
        verify(auditLogService).log(
                eq("admin"),
                eq("ADMIN"),
                eq("USER_EXPIRATION_DATE_UPDATED"),
                eq("User"),
                eq(50L),
                contains("oldExpirationDate=2026-10-01, newExpirationDate=2027-01-01")
        );
    }

    @Test
    @DisplayName("UserService: Xóa ngày hết hạn (clear -> null) -> ghi AuditLog USER_EXPIRATION_DATE_REMOVED")
    void updateUser_RemoveExpirationDate_Audited() {
        User adminOperator = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        when(currentUserService.getCurrentUser()).thenReturn(adminOperator);
        when(currentUserService.isAdmin()).thenReturn(true);

        LocalDate oldExp = LocalDate.of(2026, 10, 1);

        User existingUser = User.builder()
                .id(51L)
                .username("worker51")
                .email("worker51@test.com")
                .role(Role.USER)
                .expirationDate(oldExp)
                .enabled(true)
                .build();

        when(userRepository.findById(51L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserServiceImpl userService = new UserServiceImpl(
                userRepository, dealerRepository, svgFileRepository,
                userMapper, currentUserService, keycloakUserService,
                userDeviceService, auditLogService
        );

        UpdateUserRequest updateReq = new UpdateUserRequest();
        updateReq.setEmail("worker51@test.com");
        updateReq.setRole(Role.USER);
        updateReq.setEnabled(true);
        updateReq.setExpirationDate(null); // Clear expiration date

        UserResponse res = userService.updateUser(51L, updateReq);

        assertThat(res.getExpirationDate()).isNull();
        verify(auditLogService).log(
                eq("admin"),
                eq("ADMIN"),
                eq("USER_EXPIRATION_DATE_REMOVED"),
                eq("User"),
                eq(51L),
                contains("oldExpirationDate=2026-10-01, newExpirationDate=null")
        );
    }
}
