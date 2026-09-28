package com.example.svgmanager.service;

import com.example.svgmanager.dto.request.LoginRequest;
import com.example.svgmanager.dto.response.AuthResponse;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.exception.ServiceUnavailableException;
import com.example.svgmanager.mapper.UserMapper;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.impl.AuthServiceImpl;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Đăng nhập trang quản trị web (/api/auth/login).
 * F-57 Q1 (chốt 28/09): thợ không vào web — nếu không, giới hạn thiết bị bị vòng qua.
 * S1: Keycloak lỗi → 503.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceWebLoginTest {

    private static final String TOKEN_URI = "http://kc/realms/cutting/protocol/openid-connect/token";
    private static final String LOGOUT_URI = "http://kc/realms/cutting/protocol/openid-connect/logout";

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    private MockRestServiceServer keycloak;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, new UserMapper(), currentUserService, new ObjectMapper());
        RestClient.Builder builder = RestClient.builder();
        keycloak = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(authService, "restClient", builder.build());
        ReflectionTestUtils.setField(authService, "tokenUri", TOKEN_URI);
        ReflectionTestUtils.setField(authService, "clientId", "cutting-manager-web");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static String token(String username, String role) {
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        String payload = "{\"sub\":\"kc-" + username + "\",\"preferred_username\":\"" + username + "\","
                + "\"realm_access\":{\"roles\":[\"" + role + "\"]}}";
        return enc.encodeToString("{}".getBytes(StandardCharsets.UTF_8)) + "."
                + enc.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + ".sig";
    }

    private void keycloakIssues(String accessToken) {
        keycloak.expect(requestTo(TOKEN_URI)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"" + accessToken + "\",\"refresh_token\":\"r\",\"expires_in\":300}",
                        MediaType.APPLICATION_JSON));
    }

    private static LoginRequest login(String username) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword("Password123!");
        return request;
    }

    @Test
    @DisplayName("Thợ (USER) đăng nhập web → 403 USER_WEB_LOGIN_FORBIDDEN, phiên Keycloak bị huỷ")
    void userRole_IsRejected_AndSessionEnded() {
        when(userRepository.findByKeycloakUserId("kc-user1")).thenReturn(Optional.of(
                User.builder().id(2L).username("user1").email("u@t.vn").keycloakUserId("kc-user1").role(Role.USER).build()));
        keycloakIssues(token("user1", "USER"));
        keycloak.expect(requestTo(LOGOUT_URI)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertThatThrownBy(() -> authService.login(login("user1")))
                .isInstanceOf(ForbiddenException.class)
                .extracting(e -> ((ForbiddenException) e).getCode())
                .isEqualTo(ErrorCodes.USER_WEB_LOGIN_FORBIDDEN);
        keycloak.verify();
    }

    @Test
    @DisplayName("ADMIN đăng nhập web bình thường")
    void adminRole_LogsIn() {
        when(userRepository.findByKeycloakUserId("kc-admin")).thenReturn(Optional.of(
                User.builder().id(1L).username("admin").email("a@t.vn").keycloakUserId("kc-admin").role(Role.ADMIN).build()));
        keycloakIssues(token("admin", "ADMIN"));

        AuthResponse response = authService.login(login("admin"));

        assertThat(response.getUser().getRole()).isEqualTo(Role.ADMIN);
        keycloak.verify();
    }

    @Test
    @DisplayName("S1: Keycloak lỗi → 503, không lộ chi tiết nội bộ trong message")
    void keycloakDown_Returns503() {
        keycloak.expect(requestTo(TOKEN_URI)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> authService.login(login("admin")))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Authentication service is temporarily unavailable");
    }
}
