package com.example.svgmanager.integration;

import com.example.svgmanager.dto.internal.InternalLoginRequest;
import com.example.svgmanager.dto.internal.InternalLoginResponse;
import com.example.svgmanager.dto.internal.InternalLogoutRequest;
import com.example.svgmanager.dto.internal.InternalLogoutResponse;
import com.example.svgmanager.dto.internal.InternalRefreshTokenRequest;
import com.example.svgmanager.dto.internal.InternalUserResponse;
import com.example.svgmanager.entity.*;
import com.example.svgmanager.repository.*;
import com.example.svgmanager.service.FileStorageService;
import com.example.svgmanager.service.InternalAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InternalApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private SvgFileRepository svgFileRepository;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private InternalAuthService internalAuthService;

    private User adminUser;
    private User userInDealerA;
    private User userInDealerB;
    private User userInDealerC;
    private User userNoDealer;

    private Dealer dealerA;
    private Dealer dealerB;
    private Dealer dealerC;

    private SvgFile sampleSvg;

    @BeforeEach
    void setUp() {
        when(fileStorageService.loadFileAsResource(any())).thenReturn(
                new ByteArrayResource("<svg viewBox=\"0 0 100 100\"><circle cx=\"50\" cy=\"50\" r=\"40\"/></svg>".getBytes(StandardCharsets.UTF_8))
        );

        // 1. Create Dealers
        dealerA = dealerRepository.save(Dealer.builder()
                .code("DL-A")
                .name("Dealer Alpha")
                .status("ACTIVE")
                .build());

        dealerB = dealerRepository.save(Dealer.builder()
                .code("DL-B")
                .name("Dealer Beta")
                .status("ACTIVE")
                .build());

        dealerC = dealerRepository.save(Dealer.builder()
                .code("DL-C")
                .name("Dealer Gamma")
                .status("ACTIVE")
                .build());

        // 2. Create Users
        adminUser = userRepository.save(User.builder()
                .username("admin_internal_test")
                .email("admin_int@example.com")
                .fullName("Internal Super Admin")
                .keycloakUserId("kc-admin-internal")
                .role(Role.ADMIN)
                .enabled(true)
                .deleted(false)
                .build());

        userInDealerA = userRepository.save(User.builder()
                .username("user_dealer_a")
                .email("user_a@example.com")
                .fullName("User Dealer A")
                .keycloakUserId("kc-user-dealer-a")
                .role(Role.USER)
                .dealer(dealerA)
                .enabled(true)
                .deleted(false)
                .build());

        userInDealerB = userRepository.save(User.builder()
                .username("user_dealer_b")
                .email("user_b@example.com")
                .fullName("User Dealer B")
                .keycloakUserId("kc-user-dealer-b")
                .role(Role.USER)
                .dealer(dealerB)
                .enabled(true)
                .deleted(false)
                .build());

        userInDealerC = userRepository.save(User.builder()
                .username("user_dealer_c")
                .email("user_c@example.com")
                .fullName("User Dealer C")
                .keycloakUserId("kc-user-dealer-c")
                .role(Role.USER)
                .dealer(dealerC)
                .enabled(true)
                .deleted(false)
                .build());

        userNoDealer = userRepository.save(User.builder()
                .username("user_no_dealer")
                .email("nodealer@example.com")
                .fullName("User Without Dealer")
                .keycloakUserId("kc-user-no-dealer")
                .role(Role.USER)
                .dealer(null)
                .enabled(true)
                .deleted(false)
                .build());

        // 4. Create SVG File
        sampleSvg = svgFileRepository.save(SvgFile.builder()
                .originalFilename("BMW_X5_G05_SIDE_SKIRT.svg")
                .storedFilename("mock_stored_file_123.svg")
                .filePath("2026/09/mock_stored_file_123.svg")
                .fileSize(1258291L)
                .contentType("image/svg+xml")
                .checksum("sha256:mock_checksum_value")
                .uploadedBy(adminUser)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

    }

    // ==========================================
    // 1. AUTHENTICATION TESTS
    // ==========================================

    @Test
    @DisplayName("Public login endpoint is accessible without authentication token")
    void testPublicLoginEndpoint_AccessibleWithoutToken() throws Exception {
        InternalLoginResponse mockResponse = new InternalLoginResponse(
                "jwt.token.mock",
                "jwt.refresh.mock",
                "Bearer",
                3600L,
                new InternalUserResponse(1001L, "dealer_user01", "Nguyen Van A", 20L, "ABC Auto", "USER")
        );
        when(internalAuthService.login(any())).thenReturn(mockResponse);

        InternalLoginRequest request = new InternalLoginRequest("dealer_user01", "Password123!");

        mockMvc.perform(post("/api/internal/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is("jwt.token.mock")))
                .andExpect(jsonPath("$.refreshToken", is("jwt.refresh.mock")))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(3600)))
                .andExpect(jsonPath("$.user.username", is("dealer_user01")))
                .andExpect(jsonPath("$.user.displayName", is("Nguyen Van A")))
                .andExpect(jsonPath("$.user.dealerId", is(20)))
                .andExpect(jsonPath("$.user.dealerName", is("ABC Auto")))
                .andExpect(jsonPath("$.user.role", is("USER")));
    }

    @Test
    @DisplayName("Public refresh token endpoint is accessible without authentication token")
    void testRefreshTokenEndpoint_AccessibleWithoutToken() throws Exception {
        InternalLoginResponse mockResponse = new InternalLoginResponse(
                "jwt.token.refreshed",
                "jwt.refresh.new",
                "Bearer",
                3600L,
                new InternalUserResponse(1001L, "dealer_user01", "Nguyen Van A", 20L, "ABC Auto", "USER")
        );
        when(internalAuthService.refreshToken(any())).thenReturn(mockResponse);

        InternalRefreshTokenRequest request = new InternalRefreshTokenRequest("jwt.refresh.mock");

        mockMvc.perform(post("/api/internal/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is("jwt.token.refreshed")))
                .andExpect(jsonPath("$.refreshToken", is("jwt.refresh.new")))
                .andExpect(jsonPath("$.user.username", is("dealer_user01")));

        mockMvc.perform(post("/api/internal/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is("jwt.token.refreshed")));
    }

    @Test
    @DisplayName("Public logout endpoint is accessible without authentication token")
    void testLogoutEndpoint_AccessibleWithoutToken() throws Exception {
        when(internalAuthService.logout(any())).thenReturn(new InternalLogoutResponse(true, "Logged out successfully"));

        InternalLogoutRequest request = new InternalLogoutRequest("jwt.refresh.mock");

        mockMvc.perform(post("/api/internal/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Logged out successfully")));
    }

    @Test
    @DisplayName("Change password without JWT token returns 401 Unauthorized")
    void testChangePassword_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(post("/api/internal/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Old123!\",\"newPassword\":\"New456!\",\"confirmPassword\":\"New456!\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // 2. SVG DETAIL TESTS
    // ==========================================

    @Test
    @DisplayName("Unauthenticated request to SVG detail returns 401 Unauthorized")
    void testGetSvgDetail_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/internal/svg-files/" + sampleSvg.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin can view SVG detail and receives full metadata & permissions")
    void testGetSvgDetail_Admin_Success() throws Exception {
        mockMvc.perform(get("/api/internal/svg-files/" + sampleSvg.getId())
                        .with(jwt().jwt(j -> j.subject("kc-admin-internal").claim("preferred_username", "admin_internal_test"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(sampleSvg.getId().intValue())))
                .andExpect(jsonPath("$.fileName", is("BMW_X5_G05_SIDE_SKIRT.svg")))
                .andExpect(jsonPath("$.fileSize", is(1258291)))
                .andExpect(jsonPath("$.mimeType", is("image/svg+xml")))
                .andExpect(jsonPath("$.checksum", is("sha256:mock_checksum_value")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.permission.canView", is(true)))
                .andExpect(jsonPath("$.permission.canDownload", is(true)))
                .andExpect(jsonPath("$.vehicles", hasSize(0)));
    }

    @Test
    @DisplayName("User in Dealer A (granted view + download) can view SVG detail")
    void testGetSvgDetail_DealerA_HasBothPermissions() throws Exception {
        mockMvc.perform(get("/api/internal/svg-files/" + sampleSvg.getId())
                        .with(jwt().jwt(j -> j.subject("kc-user-dealer-a").claim("preferred_username", "user_dealer_a"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(sampleSvg.getId().intValue())))
                .andExpect(jsonPath("$.permission.canView", is(true)))
                .andExpect(jsonPath("$.permission.canDownload", is(true)));
    }

    @Test
    @DisplayName("Q6: User ở đại lý khác vẫn xem được chi tiết (không còn quyền đại lý theo file)")
    void testGetSvgDetail_OtherDealer_StillOk() throws Exception {
        mockMvc.perform(get("/api/internal/svg-files/" + sampleSvg.getId())
                        .with(jwt().jwt(j -> j.subject("kc-user-dealer-c").claim("preferred_username", "user_dealer_c"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permission.canView", is(true)))
                .andExpect(jsonPath("$.permission.canDownload", is(true)));
    }

    @Test
    @DisplayName("Q6: User không gắn đại lý vẫn xem được — chỉ còn kiểm phiên")
    void testGetSvgDetail_UserWithoutDealer_StillOk() throws Exception {
        mockMvc.perform(get("/api/internal/svg-files/" + sampleSvg.getId())
                        .with(jwt().jwt(j -> j.subject("kc-user-no-dealer").claim("preferred_username", "user_no_dealer"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 3. SVG DOWNLOAD TESTS
    // ==========================================

    @Test
    @DisplayName("Unauthenticated request to SVG download returns 401 Unauthorized")
    void testDownloadSvg_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/internal/svg-files/" + sampleSvg.getId() + "/download"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin can download SVG file successfully with image/svg+xml and attachment header")
    void testDownloadSvg_Admin_Success() throws Exception {
        mockMvc.perform(get("/api/internal/svg-files/" + sampleSvg.getId() + "/download")
                        .with(jwt().jwt(j -> j.subject("kc-admin-internal").claim("preferred_username", "admin_internal_test"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("image/svg+xml")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().string(containsString("<svg")));
    }

    @Test
    @DisplayName("User in Dealer A (has canDownload=true) can download SVG file")
    void testDownloadSvg_DealerA_Success() throws Exception {
        mockMvc.perform(get("/api/internal/svg-files/" + sampleSvg.getId() + "/download")
                        .with(jwt().jwt(j -> j.subject("kc-user-dealer-a").claim("preferred_username", "user_dealer_a"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("image/svg+xml")))
                .andExpect(content().string(containsString("<svg")));
    }

    @Test
    @DisplayName("Q6: User ở đại lý khác vẫn tải được file (không còn canDownload theo đại lý)")
    void testDownloadSvg_OtherDealer_StillOk() throws Exception {
        mockMvc.perform(get("/api/internal/svg-files/" + sampleSvg.getId() + "/download")
                        .with(jwt().jwt(j -> j.subject("kc-user-dealer-c").claim("preferred_username", "user_dealer_c"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("image/svg+xml")));
    }

    @Test
    @DisplayName("Download non-existent SVG file returns 404 Not Found")
    void testDownloadSvg_NonExistentFile_Returns404() throws Exception {
        mockMvc.perform(get("/api/internal/svg-files/999999/download")
                        .with(jwt().jwt(j -> j.subject("kc-admin-internal").claim("preferred_username", "admin_internal_test"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }
}
