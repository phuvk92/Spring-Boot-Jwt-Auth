package com.example.svgmanager.integration;

import com.example.svgmanager.dto.request.CreateUserRequest;
import com.example.svgmanager.dto.request.UpdateUserRequest;
import com.example.svgmanager.entity.Dealer;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.repository.DealerPackageHistoryRepository;
import com.example.svgmanager.repository.DealerRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.service.KeycloakUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DealerAccountPackageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private DealerPackageHistoryRepository historyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KeycloakUserService keycloakUserService;

    private User admin;
    private User agent;
    private Dealer dealer;

    @BeforeEach
    void setUp() {
        dealer = dealerRepository.save(Dealer.builder()
                .code("DL-PKG-TEST")
                .name("Đại lý Gói Quỹ Test")
                .status("ACTIVE")
                .maxAccounts(10)
                .totalAllocatedDays(3600)
                .usedAllocatedDays(0)
                .packageStartDate(LocalDate.now().minusDays(5))
                .packageExpiryDate(LocalDate.now().plusYears(1))
                .packageStatus("ACTIVE")
                .deleted(false)
                .build());

        admin = userRepository.save(User.builder()
                .keycloakUserId("kc-admin-pkg-uuid")
                .username("admin_pkg")
                .email("admin_pkg@example.com")
                .fullName("Admin Package Operator")
                .role(Role.ADMIN)
                .enabled(true)
                .deleted(false)
                .build());

        agent = userRepository.save(User.builder()
                .keycloakUserId("kc-agent-pkg-uuid")
                .username("agent_pkg")
                .email("agent_pkg@example.com")
                .fullName("Agent Package Manager")
                .role(Role.AGENT)
                .dealer(dealer)
                .enabled(true)
                .deleted(false)
                .build());

        when(keycloakUserService.createUser(any(), any(), any(), any(), anyBoolean(), any())).thenReturn("kc-mocked-client-uuid");
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtAdmin() {
        return jwt().jwt(builder -> builder
                .subject(admin.getKeycloakUserId())
                .claim("preferred_username", admin.getUsername())
                .claim("email", admin.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtAgent() {
        return jwt().jwt(builder -> builder
                .subject(agent.getKeycloakUserId())
                .claim("preferred_username", agent.getUsername())
                .claim("email", agent.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_AGENT"));
    }

    @Test
    @DisplayName("AGENT xem tổng quan gói của đại lý mình: GET /api/dealers/my-package")
    void testGetMyDealerPackage_AsAgent() throws Exception {
        mockMvc.perform(get("/api/dealers/my-package")
                        .with(jwtAgent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dealerId").value(dealer.getId()))
                .andExpect(jsonPath("$.dealerCode").value("DL-PKG-TEST"))
                .andExpect(jsonPath("$.maxAccounts").value(10))
                .andExpect(jsonPath("$.totalAllocatedDays").value(3600))
                .andExpect(jsonPath("$.usedAllocatedDays").value(0))
                .andExpect(jsonPath("$.remainingAllocatedDays").value(3600))
                .andExpect(jsonPath("$.packageStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("ADMIN xem tổng quan gói đại lý: GET /api/dealers/{id}/package")
    void testGetDealerPackage_AsAdmin() throws Exception {
        mockMvc.perform(get("/api/dealers/{id}/package", dealer.getId())
                        .with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dealerId").value(dealer.getId()))
                .andExpect(jsonPath("$.maxAccounts").value(10))
                .andExpect(jsonPath("$.totalAllocatedDays").value(3600));
    }

    @Test
    @DisplayName("AGENT tạo user hợp lệ trong hạn mức gói thành công")
    void testCreateUser_UnderPackageLimits_Success() throws Exception {
        CreateUserRequest req = CreateUserRequest.builder()
                .username("client_pkg_1")
                .email("client_pkg_1@example.com")
                .fullName("Client Package 1")
                .password("Password123!")
                .role(Role.USER)
                .allocatedDays(360)
                .build();

        mockMvc.perform(post("/api/users")
                        .with(jwtAgent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.allocatedDays").value(360))
                .andExpect(jsonPath("$.expirationDate").isNotEmpty());

        Dealer updated = dealerRepository.findById(dealer.getId()).orElseThrow();
        assertThat(updated.getUsedAllocatedDays()).isEqualTo(360);
        assertThat(updated.getRemainingAllocatedDays()).isEqualTo(3240);
        assertThat(historyRepository.findByDealerIdOrderByCreatedAtDesc(dealer.getId())).hasSize(1);
    }

    @Test
    @DisplayName("AGENT tạo user vượt quá quỹ ngày còn lại bị từ chối 400")
    void testCreateUser_ExceedRemainingDays_Fails() throws Exception {
        dealer.setUsedAllocatedDays(3500); // Only 100 days remaining
        dealerRepository.save(dealer);

        CreateUserRequest req = CreateUserRequest.builder()
                .username("client_pkg_overflow")
                .email("client_pkg_overflow@example.com")
                .fullName("Client Overflow")
                .password("Password123!")
                .role(Role.USER)
                .allocatedDays(180) // 180 > 100
                .build();

        mockMvc.perform(post("/api/users")
                        .with(jwtAgent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Quỹ ngày còn lại của đại lý không đủ")));
    }

    @Test
    @DisplayName("Cập nhật thời hạn user điều chỉnh quỹ delta đúng")
    void testUpdateUser_AdjustDuration_UpdatesDealerQuota() throws Exception {
        // Tạo user 180 ngày trước
        User u = userRepository.save(User.builder()
                .keycloakUserId("kc-user-adjust-uuid")
                .username("user_adjust")
                .email("user_adjust@example.com")
                .role(Role.USER)
                .agent(agent)
                .dealer(dealer)
                .allocatedDays(180)
                .expirationDate(LocalDate.now().plusDays(180))
                .enabled(true)
                .deleted(false)
                .build());

        dealer.setUsedAllocatedDays(180);
        dealerRepository.save(dealer);

        // Gia hạn lên 360 ngày (+180 ngày)
        UpdateUserRequest updateReq = UpdateUserRequest.builder()
                .email("user_adjust@example.com")
                .allocatedDays(360)
                .build();

        mockMvc.perform(put("/api/users/{id}", u.getId())
                        .with(jwtAgent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocatedDays").value(360));

        Dealer updated = dealerRepository.findById(dealer.getId()).orElseThrow();
        assertThat(updated.getUsedAllocatedDays()).isEqualTo(360);
    }

    @Test
    @DisplayName("Xóa user không hoàn lại quỹ ngày đã phân bổ theo chính sách hiện tại")
    void testDeleteUser_DoesNotRefundUsedDays() throws Exception {
        User u = userRepository.save(User.builder()
                .keycloakUserId("kc-user-del-uuid")
                .username("user_to_delete")
                .email("user_to_delete@example.com")
                .role(Role.USER)
                .agent(agent)
                .dealer(dealer)
                .allocatedDays(360)
                .expirationDate(LocalDate.now().plusDays(360))
                .enabled(true)
                .deleted(false)
                .build());

        dealer.setUsedAllocatedDays(360);
        dealerRepository.save(dealer);

        mockMvc.perform(delete("/api/users/{id}", u.getId())
                        .with(jwtAgent()))
                .andExpect(status().isNoContent());

        Dealer afterDelete = dealerRepository.findById(dealer.getId()).orElseThrow();
        // Quỹ ngày đã sử dụng vẫn là 360, không bị trừ về 0
        assertThat(afterDelete.getUsedAllocatedDays()).isEqualTo(360);
    }
}
