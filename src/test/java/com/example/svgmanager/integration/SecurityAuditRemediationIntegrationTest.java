package com.example.svgmanager.integration;

import com.example.svgmanager.dto.request.UpdateUserRequest;
import com.example.svgmanager.entity.Dealer;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.repository.DealerRepository;
import com.example.svgmanager.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityAuditRemediationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Dealer agentDealer;
    private Dealer otherDealer;
    private User agentUser;
    private User regularUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        agentDealer = dealerRepository.save(Dealer.builder()
                .name("Agent Owned Dealer")
                .code("DL-OWNED-01")
                .status("ACTIVE")
                .deleted(false)
                .build());

        otherDealer = dealerRepository.save(Dealer.builder()
                .name("Other Private Dealer")
                .code("DL-OTHER-02")
                .status("ACTIVE")
                .deleted(false)
                .build());

        agentUser = userRepository.save(User.builder()
                .keycloakUserId("kc-agent-sec-test")
                .username("agent_sec_test")
                .email("agent_sec_test@example.com")
                .fullName("Agent Security Tester")
                .role(Role.AGENT)
                .dealer(agentDealer)
                .enabled(true)
                .deleted(false)
                .build());

        regularUser = userRepository.save(User.builder()
                .keycloakUserId("kc-user-sec-test")
                .username("user_sec_test")
                .email("user_sec_test@example.com")
                .fullName("Regular User Tester")
                .role(Role.USER)
                .dealer(agentDealer)
                .enabled(true)
                .deleted(false)
                .build());

        adminUser = userRepository.save(User.builder()
                .keycloakUserId("kc-admin-sec-test")
                .username("admin_sec_test")
                .email("admin_sec_test@example.com")
                .fullName("Admin Security Tester")
                .role(Role.ADMIN)
                .enabled(true)
                .deleted(false)
                .build());
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtAgent() {
        return jwt().jwt(b -> b.subject(agentUser.getKeycloakUserId())
                .claim("preferred_username", agentUser.getUsername())
                .claim("email", agentUser.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_AGENT"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtUser() {
        return jwt().jwt(b -> b.subject(regularUser.getKeycloakUserId())
                .claim("preferred_username", regularUser.getUsername())
                .claim("email", regularUser.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtAdmin() {
        return jwt().jwt(b -> b.subject(adminUser.getKeycloakUserId())
                .claim("preferred_username", adminUser.getUsername())
                .claim("email", adminUser.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("BOLA: AGENT không thể xem chi tiết đại lý khác -> 403 Forbidden")
    void agent_GetOtherDealerById_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/dealers/" + otherDealer.getId())
                        .with(jwtAgent()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("BOLA: AGENT chỉ xem được chi tiết đại lý của chính mình -> 200 OK")
    void agent_GetOwnDealerById_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/dealers/" + agentDealer.getId())
                        .with(jwtAgent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(agentDealer.getId()))
                .andExpect(jsonPath("$.code").value("DL-OWNED-01"));
    }

    @Test
    @DisplayName("Scope Leak: AGENT gọi GET /api/dealers chỉ nhận đại lý thuộc quyền quản lý")
    void agent_GetDealersList_ScopedToOwnDealer() throws Exception {
        mockMvc.perform(get("/api/dealers")
                        .with(jwtAgent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(agentDealer.getId()));
    }

    @Test
    @DisplayName("Scope Leak: AGENT gọi GET /api/dealers/all chỉ nhận đại lý thuộc quyền quản lý")
    void agent_GetAllDealers_ScopedToOwnDealer() throws Exception {
        mockMvc.perform(get("/api/dealers/all")
                        .with(jwtAgent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(agentDealer.getId()));
    }

    @Test
    @DisplayName("Authorization: AGENT gọi GET /api/dealers/stats bị cấm -> 403 Forbidden")
    void agent_GetDealerStats_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/dealers/stats")
                        .with(jwtAgent()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authorization: ADMIN gọi GET /api/dealers/stats được phép -> 200 OK")
    void admin_GetDealerStats_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/dealers/stats")
                        .with(jwtAdmin()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Privilege Escalation: Non-admin không thể đổi mật khẩu người khác qua updateUser")
    void nonAdmin_UpdateUser_WithPassword_ReturnsForbidden() throws Exception {
        UpdateUserRequest req = UpdateUserRequest.builder()
                .email(regularUser.getEmail())
                .fullName("Hacked Name")
                .password("MaliciousPass123!")
                .build();

        mockMvc.perform(put("/api/users/" + regularUser.getId())
                        .with(jwtAgent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authentication: Endpoint /api/auth/me không có JWT phải trả về 401 Unauthorized")
    void unauthenticated_GetAuthMe_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security Headers: Cấu hình X-Content-Type-Options và Frame-Options")
    void securityHeaders_PresentOnResponses() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }
}
