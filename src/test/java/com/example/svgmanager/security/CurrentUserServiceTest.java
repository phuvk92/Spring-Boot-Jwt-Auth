package com.example.svgmanager.security;

import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.UnauthorizedException;
import com.example.svgmanager.exception.UserAccountExpiredException;
import com.example.svgmanager.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CurrentUserService currentUserService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private Jwt mockJwt;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockJwt = new Jwt(
                "mock-token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("sub", "keycloak-uuid-123", "preferred_username", "testuser")
        );

        testUser = User.builder()
                .id(100L)
                .keycloakUserId("keycloak-uuid-123")
                .username("testuser")
                .email("testuser@example.com")
                .role(Role.USER)
                .enabled(true)
                .deleted(false)
                .expirationDate(LocalDate.now().plusDays(30))
                .allocatedDays(365)
                .build();

        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(mockJwt);
        lenient().doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(authentication).getAuthorities();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Active non-expired user returns successfully")
    void testGetCurrentUser_Success() {
        when(userRepository.findByKeycloakUserId("keycloak-uuid-123")).thenReturn(Optional.of(testUser));

        User result = currentUserService.getCurrentUser();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Inactive/disabled user throws UnauthorizedException with ACCOUNT_DISABLED")
    void testGetCurrentUser_DisabledUser_ThrowsException() {
        testUser.setEnabled(false);
        when(userRepository.findByKeycloakUserId("keycloak-uuid-123")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> currentUserService.getCurrentUser())
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(e -> {
                    UnauthorizedException ue = (UnauthorizedException) e;
                    assertThat(ue.getCode()).isEqualTo(ErrorCodes.ACCOUNT_DISABLED);
                });
    }

    @Test
    @DisplayName("Deleted user throws UnauthorizedException with ACCOUNT_DISABLED")
    void testGetCurrentUser_DeletedUser_ThrowsException() {
        testUser.setDeleted(true);
        when(userRepository.findByKeycloakUserId("keycloak-uuid-123")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> currentUserService.getCurrentUser())
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(e -> {
                    UnauthorizedException ue = (UnauthorizedException) e;
                    assertThat(ue.getCode()).isEqualTo(ErrorCodes.ACCOUNT_DISABLED);
                });
    }

    @Test
    @DisplayName("Expired user with ROLE_USER throws UserAccountExpiredException with USER_ACCOUNT_EXPIRED")
    void testGetCurrentUser_ExpiredUser_ThrowsUserAccountExpiredException() {
        testUser.setExpirationDate(LocalDate.now().minusDays(1)); // Expired yesterday
        when(userRepository.findByKeycloakUserId("keycloak-uuid-123")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> currentUserService.getCurrentUser())
                .isInstanceOf(UserAccountExpiredException.class)
                .satisfies(e -> {
                    UserAccountExpiredException ue = (UserAccountExpiredException) e;
                    assertThat(ue.getCode()).isEqualTo(ErrorCodes.USER_ACCOUNT_EXPIRED);
                });
    }

    @Test
    @DisplayName("Expired expirationDate for ROLE_ADMIN does not throw exception")
    void testGetCurrentUser_AdminExpiredDate_Allowed() {
        testUser.setRole(Role.ADMIN);
        testUser.setExpirationDate(LocalDate.now().minusDays(10));
        lenient().doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();
        when(userRepository.findByKeycloakUserId("keycloak-uuid-123")).thenReturn(Optional.of(testUser));

        User result = currentUserService.getCurrentUser();

        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
    }
}
