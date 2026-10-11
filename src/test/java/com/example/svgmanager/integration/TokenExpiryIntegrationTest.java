package com.example.svgmanager.integration;

import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.UnauthorizedException;
import com.example.svgmanager.service.InternalAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PCU-6 — app cắt phân biệt "access token hết hạn" (chỉ cần refresh) với 401 khác
 * nhờ trường {@code code} trong body. Thiết kế: Pcut-Client/technical/SA-LamMoiToken.md §3.5.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TokenExpiryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private InternalAuthService internalAuthService;

    @Test
    @DisplayName("AC1: Bearer JWT hết hạn → 401 có code TOKEN_EXPIRED")
    void expiredAccessToken_ReturnsTokenExpired() throws Exception {
        when(jwtDecoder.decode("expired-token")).thenThrow(new JwtValidationException(
                "An error occurred while attempting to decode the Jwt: Jwt expired at 2026-10-11T00:00:00Z",
                List.of(new OAuth2Error("invalid_token", "Jwt expired at 2026-10-11T00:00:00Z", null))));

        mockMvc.perform(get("/api/v1/branding").header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.TOKEN_EXPIRED));
    }

    @Test
    @DisplayName("AC2: Bearer JWT sai chữ ký → 401 như cũ, không có TOKEN_EXPIRED")
    void badSignature_ReturnsPlainUnauthorized() throws Exception {
        when(jwtDecoder.decode("bad-sig-token")).thenThrow(new BadJwtException("Invalid signature"));

        mockMvc.perform(get("/api/v1/branding").header("Authorization", "Bearer bad-sig-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    @DisplayName("AC2: thiếu token → 401 như cũ, không có TOKEN_EXPIRED")
    void missingToken_ReturnsPlainUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/branding"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    @DisplayName("AC4: /refresh với refresh token hết hạn → 401 code REFRESH_EXPIRED")
    void expiredRefreshToken_ReturnsRefreshExpired() throws Exception {
        when(internalAuthService.refreshToken(any()))
                .thenThrow(new UnauthorizedException("Invalid or expired refresh token", ErrorCodes.REFRESH_EXPIRED));

        mockMvc.perform(post("/api/internal/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"rt-het-han\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.REFRESH_EXPIRED));
    }

    @Test
    @DisplayName("AC4: /refresh khi máy bị gỡ (F-57) → vẫn 401 SESSION_REVOKED")
    void revokedSession_OnRefresh_StillSessionRevoked() throws Exception {
        when(internalAuthService.refreshToken(any()))
                .thenThrow(new UnauthorizedException("Máy bị gỡ", ErrorCodes.SESSION_REVOKED));

        mockMvc.perform(post("/api/internal/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"rt-may-bi-go\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.SESSION_REVOKED));
    }
}
