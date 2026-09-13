package com.example.svgmanager.service;

import com.example.svgmanager.dto.request.LoginRequest;
import com.example.svgmanager.dto.request.RefreshTokenRequest;
import com.example.svgmanager.dto.request.RegisterRequest;
import com.example.svgmanager.dto.response.AuthResponse;
import com.example.svgmanager.dto.response.UserResponse;
import com.example.svgmanager.entity.RefreshToken;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.ConflictException;
import com.example.svgmanager.exception.UnauthorizedException;
import com.example.svgmanager.mapper.UserMapper;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.security.JwtTokenProvider;
import com.example.svgmanager.security.UserPrincipal;
import com.example.svgmanager.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenService refreshTokenService;

    private UserMapper userMapper;
    private JwtTokenProvider jwtTokenProvider;
    private AuthServiceImpl authService;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        jwtTokenProvider = new JwtTokenProvider("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", 3600000L);
        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtTokenProvider,
                refreshTokenService,
                userMapper
        );

        sampleUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .password("encoded_pass")
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .username("john_doe")
                .email("john@example.com")
                .password("Password123!")
                .build();

        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse result = authService.register(request);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("john_doe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getRole()).isEqualTo(Role.USER);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when username already exists during registration")
    void register_UsernameConflict() {
        RegisterRequest request = RegisterRequest.builder()
                .username("john_doe")
                .email("new_email@example.com")
                .password("Password123!")
                .build();

        when(userRepository.existsByUsername("john_doe")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Username is already taken");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when email already exists during registration")
    void register_EmailConflict() {
        RegisterRequest request = RegisterRequest.builder()
                .username("new_user")
                .email("john@example.com")
                .password("Password123!")
                .build();

        when(userRepository.existsByUsername("new_user")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email is already in use");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully login and return access and refresh tokens")
    void login_Success() {
        LoginRequest request = LoginRequest.builder()
                .username("john_doe")
                .password("Password123!")
                .build();

        UserPrincipal principal = UserPrincipal.create(sampleUser);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token("mock-refresh-token")
                .user(sampleUser)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(refreshTokenService.createRefreshToken(sampleUser)).thenReturn(refreshToken);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotEmpty();
        assertThat(response.getRefreshToken()).isEqualTo("mock-refresh-token");
        assertThat(response.getUser().getUsername()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when login credentials are invalid")
    void login_BadCredentials() {
        LoginRequest request = LoginRequest.builder()
                .username("john_doe")
                .password("WrongPassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Should successfully refresh access token using valid refresh token")
    void refreshToken_Success() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token("valid-refresh-token")
                .user(sampleUser)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        when(refreshTokenService.findByToken("valid-refresh-token")).thenReturn(Optional.of(refreshToken));
        when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(refreshToken);

        AuthResponse response = authService.refreshToken(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotEmpty();
        assertThat(response.getRefreshToken()).isEqualTo("valid-refresh-token");
        assertThat(response.getUser().getUsername()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when refresh token is invalid or not found")
    void refreshToken_InvalidToken() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("non-existent-token")
                .build();

        when(refreshTokenService.findByToken("non-existent-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Refresh token not found or invalid");
    }
}
