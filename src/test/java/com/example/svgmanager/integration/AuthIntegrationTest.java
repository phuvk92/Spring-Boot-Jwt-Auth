package com.example.svgmanager.integration;

import com.example.svgmanager.dto.request.LoginRequest;
import com.example.svgmanager.dto.request.RefreshTokenRequest;
import com.example.svgmanager.dto.request.RegisterRequest;
import com.example.svgmanager.dto.response.AuthResponse;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.repository.RefreshTokenRepository;
import com.example.svgmanager.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Integration: Should complete full register -> login -> refresh -> me flow")
    void fullAuthFlow_Success() throws Exception {
        // 1. Register
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("new_user_1")
                .email("new_user_1@example.com")
                .password("StrongPass123!")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new_user_1"))
                .andExpect(jsonPath("$.email").value("new_user_1@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        // 2. Login
        LoginRequest loginRequest = LoginRequest.builder()
                .username("new_user_1")
                .password("StrongPass123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                AuthResponse.class
        );
        String accessToken = authResponse.getAccessToken();
        String refreshToken = authResponse.getRefreshToken();

        // 3. Get /me with Access Token
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new_user_1"))
                .andExpect(jsonPath("$.email").value("new_user_1@example.com"));

        // 4. Refresh Token
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("Integration: Should reject register when password does not meet complexity requirements")
    void register_InvalidPasswordComplexity() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("weak_pass_user")
                .email("weak@example.com")
                .password("simple")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("Integration: Should return 401 when login with invalid credentials")
    void login_InvalidCredentials() throws Exception {
        User user = User.builder()
                .username("existing_user")
                .email("existing@example.com")
                .password(passwordEncoder.encode("CorrectPass123!"))
                .role(Role.USER)
                .enabled(true)
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = LoginRequest.builder()
                .username("existing_user")
                .password("WrongPass123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
}
