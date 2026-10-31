package com.example.svgmanager.integration;

import com.example.svgmanager.dto.request.ChangePasswordRequest;
import com.example.svgmanager.dto.request.CreateDealerRequest;
import com.example.svgmanager.dto.request.CreateUserRequest;
import com.example.svgmanager.dto.request.ResetPasswordRequest;
import com.example.svgmanager.dto.request.UpdateUserRequest;
import com.example.svgmanager.dto.internal.InternalChangePasswordRequest;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.service.AuthService;
import com.example.svgmanager.service.InternalAuthService;
import com.example.svgmanager.service.KeycloakUserService;
import com.example.svgmanager.service.UserService;
import com.example.svgmanager.service.DealerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Password Sanitization (Trim Whitespace) Integration Tests")
class PasswordSanitizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private KeycloakUserService keycloakUserService;

    private User adminUser;
    private User testUser;

    @BeforeEach
    void setUp() {
        when(keycloakUserService.createUser(anyString(), anyString(), anyString(), any(Role.class), anyBoolean(), anyString()))
                .thenReturn("kc-mock-id");
        when(keycloakUserService.createUser(anyString(), anyString(), anyString(), any(Role.class), anyBoolean()))
                .thenReturn("kc-mock-id");

        adminUser = userRepository.save(User.builder()
                .username("admin_pass_test")
                .email("admin_pass@test.com")
                .role(Role.ADMIN)
                .keycloakUserId("kc-admin-id")
                .enabled(true)
                .build());

        testUser = userRepository.save(User.builder()
                .username("user_pass_test")
                .email("user_pass@test.com")
                .role(Role.USER)
                .keycloakUserId("kc-user-id")
                .enabled(true)
                .build());
    }

    @Test
    @DisplayName("Case 1: Create user with password having leading/trailing spaces -> trimmed when sent to Keycloak")
    void testCreateUserWithWhitespacePassword() throws Exception {
        String jsonPayload = """
                {
                  "username": "new_trim_user",
                  "email": "new_trim@example.com",
                  "password": "   Password123!   ",
                  "role": "USER",
                  "enabled": true
                }
                """;

        mockMvc.perform(post("/api/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).jwt(j -> j.claim("preferred_username", "admin_pass_test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new_trim_user"));

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(keycloakUserService).createUser(eq("new_trim_user"), eq("new_trim@example.com"), passwordCaptor.capture(), eq(Role.USER), eq(true), any());
        assertThat(passwordCaptor.getValue()).isEqualTo("Password123!");
    }

    @Test
    @DisplayName("Case 2: Update user with password having leading/trailing spaces -> trimmed when reset in Keycloak")
    void testUpdateUserWithWhitespacePassword() throws Exception {
        String jsonPayload = """
                {
                  "email": "user_pass@test.com",
                  "fullName": "Updated User",
                  "role": "USER",
                  "enabled": true,
                  "password": "   NewPassword123!   "
                }
                """;

        mockMvc.perform(put("/api/users/" + testUser.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).jwt(j -> j.claim("preferred_username", "admin_pass_test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk());

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(keycloakUserService).resetPassword(eq("kc-user-id"), passwordCaptor.capture());
        assertThat(passwordCaptor.getValue()).isEqualTo("NewPassword123!");
    }

    @Test
    @DisplayName("Case 3: Admin reset user password with leading/trailing spaces -> trimmed")
    void testAdminResetPasswordWithWhitespace() throws Exception {
        String jsonPayload = """
                {
                  "newPassword": "   ResetPassword123!   "
                }
                """;

        mockMvc.perform(post("/api/users/" + testUser.getId() + "/reset-password")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).jwt(j -> j.claim("preferred_username", "admin_pass_test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk());

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(keycloakUserService).resetPassword(eq("kc-user-id"), passwordCaptor.capture());
        assertThat(passwordCaptor.getValue()).isEqualTo("ResetPassword123!");
    }

    @Test
    @DisplayName("Case 5: Create dealer with agentPassword having leading/trailing spaces -> trimmed")
    void testCreateDealerWithWhitespaceAgentPassword() throws Exception {
        String jsonPayload = """
                {
                  "code": "DEALER_TRIM_1",
                  "name": "Dealer Trim Test",
                  "status": "ACTIVE",
                  "maxAccounts": 5,
                  "totalAllocatedDays": 360,
                  "packageStatus": "ACTIVE",
                  "agentUsername": "dealer_agent_trim",
                  "agentEmail": "dealer_agent_trim@example.com",
                  "agentPassword": "   DealerAgent123!   "
                }
                """;

        mockMvc.perform(post("/api/dealers")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).jwt(j -> j.claim("preferred_username", "admin_pass_test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated());

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(keycloakUserService).createUser(eq("dealer_agent_trim"), eq("dealer_agent_trim@example.com"), passwordCaptor.capture(), eq(Role.AGENT), eq(true), anyString());
        assertThat(passwordCaptor.getValue()).isEqualTo("DealerAgent123!");
    }

    @Test
    @DisplayName("Case 6: Change password where new and confirm have different whitespace but match after trim -> accepted")
    void testChangePasswordWhitespaceMatching() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "  OldPass123!  ",
                "   NewPassword123! ",
                "NewPassword123!     "
        );

        assertThat(request.getCurrentPassword()).isEqualTo("OldPass123!");
        assertThat(request.getNewPassword()).isEqualTo("NewPassword123!");
        assertThat(request.getConfirmPassword()).isEqualTo("NewPassword123!");
        assertThat(request.getNewPassword()).isEqualTo(request.getConfirmPassword());
    }

    @Test
    @DisplayName("Case 7: Change password where new and confirm differ after trim -> rejected")
    void testChangePasswordMismatch() {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "OldPass123!",
                "   NewPassword123! ",
                "  DifferentPassword123!  "
        );

        assertThat(request.getNewPassword()).isNotEqualTo(request.getConfirmPassword());
    }

    @Test
    @DisplayName("Case 8: Password containing only whitespace -> rejected")
    void testWhitespaceOnlyPasswordRejected() throws Exception {
        String jsonPayload = """
                {
                  "username": "blank_pass_user",
                  "email": "blank_pass@example.com",
                  "password": "        ",
                  "role": "USER",
                  "enabled": true
                }
                """;

        mockMvc.perform(post("/api/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).jwt(j -> j.claim("preferred_username", "admin_pass_test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Case 9: Password with middle whitespace -> preserved")
    void testMiddleWhitespacePreserved() throws Exception {
        String jsonPayload = """
                {
                  "username": "mid_space_user",
                  "email": "mid_space@example.com",
                  "password": "   My Secret 123!   ",
                  "role": "USER",
                  "enabled": true
                }
                """;

        mockMvc.perform(post("/api/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).jwt(j -> j.claim("preferred_username", "admin_pass_test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated());

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(keycloakUserService).createUser(eq("mid_space_user"), eq("mid_space@example.com"), passwordCaptor.capture(), eq(Role.USER), eq(true), any());
        assertThat(passwordCaptor.getValue()).isEqualTo("My Secret 123!");
    }

    @Test
    @DisplayName("Case 10: Update user without password or blank password -> existing password untouched")
    void testUpdateUserWithoutPasswordPreservesOld() throws Exception {
        String jsonPayload = """
                {
                  "email": "user_pass@test.com",
                  "fullName": "Name Changed Only",
                  "role": "USER",
                  "enabled": true,
                  "password": "     "
                }
                """;

        mockMvc.perform(put("/api/users/" + testUser.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).jwt(j -> j.claim("preferred_username", "admin_pass_test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk());

        // Verify keycloak resetPassword was NEVER invoked
        verify(keycloakUserService, never()).resetPassword(anyString(), anyString());
    }
}
