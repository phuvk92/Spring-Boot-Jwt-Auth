package com.example.svgmanager.integration;

import com.example.svgmanager.dto.request.CreateUserRequest;
import com.example.svgmanager.dto.request.UpdateUserRoleRequest;
import com.example.svgmanager.entity.Dealer;
import com.example.svgmanager.entity.DeviceStatus;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.UserDevice;
import com.example.svgmanager.repository.DealerRepository;
import com.example.svgmanager.repository.UserDeviceRepository;
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

import java.time.Instant;
import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserStatsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KeycloakUserService keycloakUserService;

    private User admin;
    private User agentA;
    private User agentB;
    private User cutterA1;
    private User cutterA2;
    private User cutterB1;
    private Dealer dealerA;
    private Dealer dealerB;

    @BeforeEach
    void setUp() {
        dealerA = dealerRepository.save(Dealer.builder()
                .name("Đại lý Alpha")
                .code("DL-ALPHA")
                .status("ACTIVE")
                .deleted(false)
                .build());

        dealerB = dealerRepository.save(Dealer.builder()
                .name("Đại lý Beta")
                .code("DL-BETA")
                .status("ACTIVE")
                .deleted(false)
                .build());

        admin = userRepository.save(User.builder()
                .keycloakUserId("kc-admin-stats-uuid")
                .username("admin_stats")
                .email("admin_stats@example.com")
                .fullName("System Administrator")
                .role(Role.ADMIN)
                .enabled(true)
                .deleted(false)
                .build());

        agentA = userRepository.save(User.builder()
                .keycloakUserId("kc-agent-a-uuid")
                .username("agent_alpha")
                .email("agent_alpha@example.com")
                .fullName("Agent Alpha Manager")
                .role(Role.AGENT)
                .dealer(dealerA)
                .enabled(true)
                .deleted(false)
                .build());

        agentB = userRepository.save(User.builder()
                .keycloakUserId("kc-agent-b-uuid")
                .username("agent_beta")
                .email("agent_beta@example.com")
                .fullName("Agent Beta Manager")
                .role(Role.AGENT)
                .dealer(dealerB)
                .enabled(true)
                .deleted(false)
                .build());

        cutterA1 = userRepository.save(User.builder()
                .keycloakUserId("kc-cutter-a1-uuid")
                .username("cutter_a1")
                .email("cutter_a1@example.com")
                .fullName("Cutter A1")
                .role(Role.USER)
                .agent(agentA)
                .dealer(dealerA)
                .enabled(true)
                .deleted(false)
                .expirationDate(LocalDate.now().plusMonths(3))
                .build());

        cutterA2 = userRepository.save(User.builder()
                .keycloakUserId("kc-cutter-a2-uuid")
                .username("cutter_a2")
                .email("cutter_a2@example.com")
                .fullName("Cutter A2")
                .role(Role.USER)
                .agent(agentA)
                .dealer(dealerA)
                .enabled(true)
                .deleted(false)
                .expirationDate(LocalDate.now().plusMonths(6))
                .build());

        cutterB1 = userRepository.save(User.builder()
                .keycloakUserId("kc-cutter-b1-uuid")
                .username("cutter_b1")
                .email("cutter_b1@example.com")
                .fullName("Cutter B1")
                .role(Role.USER)
                .agent(agentB)
                .dealer(dealerB)
                .enabled(true)
                .deleted(false)
                .expirationDate(LocalDate.now().plusMonths(1))
                .build());

        when(keycloakUserService.createUser(any(), any(), any(), any(), anyBoolean(), any())).thenReturn("kc-new-mock-uuid");
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtAdmin() {
        return jwt().jwt(builder -> builder
                .subject(admin.getKeycloakUserId())
                .claim("preferred_username", admin.getUsername())
                .claim("email", admin.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtAgentA() {
        return jwt().jwt(builder -> builder
                .subject(agentA.getKeycloakUserId())
                .claim("preferred_username", agentA.getUsername())
                .claim("email", agentA.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_AGENT"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtCutterA1() {
        return jwt().jwt(builder -> builder
                .subject(cutterA1.getKeycloakUserId())
                .claim("preferred_username", cutterA1.getUsername())
                .claim("email", cutterA1.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    @DisplayName("Tình huống 1: Có nhiều tài khoản ADMIN, AGENT, USER: số lượng từng nhóm chính xác")
    void test1_CountsAccurateAcrossRoles() throws Exception {
        // Initial setup: 1 ADMIN, 2 AGENT, 3 USER
        mockMvc.perform(get("/api/users/stats").with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminCount", is(1)))
                .andExpect(jsonPath("$.agentCount", is(2)))
                .andExpect(jsonPath("$.userCount", is(3)))
                .andExpect(jsonPath("$.total", is(6)))
                .andExpect(jsonPath("$.admin", is(1)))
                .andExpect(jsonPath("$.agent", is(2)))
                .andExpect(jsonPath("$.user", is(3)));
    }

    @Test
    @DisplayName("Tình huống 2: Một tài khoản có nhiều bản ghi quan hệ (devices) không bị đếm trùng")
    void test2_MultipleRelationsDoNotMultiplyCounts() throws Exception {
        // Give cutterA1 three registered devices in user_devices table
        userDeviceRepository.save(UserDevice.register(
                cutterA1, "device-hash-111111111111", "Máy Cắt 1", "Windows", "127.0.0.1", "sid-1", java.time.LocalDateTime.now()));
        userDeviceRepository.save(UserDevice.register(
                cutterA1, "device-hash-222222222222", "Máy Cắt 2", "Windows", "127.0.0.1", "sid-2", java.time.LocalDateTime.now()));
        userDeviceRepository.save(UserDevice.register(
                cutterA1, "device-hash-333333333333", "Máy Cắt 3", "Windows", "127.0.0.1", "sid-3", java.time.LocalDateTime.now()));

        // Count for USER must remain exactly 3, not multiplied by 3
        mockMvc.perform(get("/api/users/stats").with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCount", is(3)))
                .andExpect(jsonPath("$.total", is(6)));
    }

    @Test
    @DisplayName("Tình huống 3: Tài khoản bị xoá mềm không được tính vào thống kê")
    void test3_SoftDeletedAccountsExcluded() throws Exception {
        // Soft delete cutterB1
        cutterB1.setDeleted(true);
        userRepository.save(cutterB1);

        mockMvc.perform(get("/api/users/stats").with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminCount", is(1)))
                .andExpect(jsonPath("$.agentCount", is(2)))
                .andExpect(jsonPath("$.userCount", is(2))) // decreased from 3 to 2
                .andExpect(jsonPath("$.total", is(5)));
    }

    @Test
    @DisplayName("Tình huống 4: Tài khoản bị vô hiệu hóa hoặc hết hạn: xử lý đúng theo bộ lọc")
    void test4_DisabledAndExpiredHandling() throws Exception {
        // Lock cutterA2
        cutterA2.setEnabled(false);
        userRepository.save(cutterA2);

        // Expire cutterA1
        cutterA1.setExpirationDate(LocalDate.now().minusDays(5));
        userRepository.save(cutterA1);

        // When no filter: still counted as valid registered accounts
        mockMvc.perform(get("/api/users/stats").with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCount", is(3)));

        // When filtering by enabled=true: only active accounts (cutterB1 is active)
        mockMvc.perform(get("/api/users/stats?enabled=true").with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCount", is(2))); // cutterA1 and cutterB1

        // When filtering by expirationStatus=VALID: only unexpired
        mockMvc.perform(get("/api/users/stats?expirationStatus=VALID").with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCount", is(2))); // cutterA2 and cutterB1
    }

    @Test
    @DisplayName("Tình huống 5: Tài khoản có role không hợp lệ hoặc NULL không làm sai số liệu nhóm hợp lệ")
    void test5_InvalidRoleDoesNotCorruptCounts() throws Exception {
        // In PostgreSQL, role has NOT NULL constraint, but even if another record exists
        // role counts only match ADMIN, AGENT, USER
        mockMvc.perform(get("/api/users/stats").with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminCount", is(1)))
                .andExpect(jsonPath("$.agentCount", is(2)))
                .andExpect(jsonPath("$.userCount", is(3)));
    }

    @Test
    @DisplayName("Tình huống 6: Danh sách có phân trang: thống kê vẫn là tổng số chính xác")
    void test6_StatsUnaffectedByPagination() throws Exception {
        // Even if users list is fetched with size=1
        mockMvc.perform(get("/api/users?page=0&size=1").with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.totalElements", is(6)));

        // Stats returns total counts across all pages
        mockMvc.perform(get("/api/users/stats").with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminCount", is(1)))
                .andExpect(jsonPath("$.agentCount", is(2)))
                .andExpect(jsonPath("$.userCount", is(3)))
                .andExpect(jsonPath("$.total", is(6)));
    }

    @Test
    @DisplayName("Tình huống 7: Thêm tài khoản mới: số lượng tăng đúng")
    void test7_CreateUserIncreasesStats() throws Exception {
        CreateUserRequest createReq = CreateUserRequest.builder()
                .username("new_agent_gamma")
                .email("agent_gamma@example.com")
                .password("Password123@")
                .role(Role.AGENT)
                .enabled(true)
                .build();

        mockMvc.perform(post("/api/users")
                .with(jwtAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        // Agent count should be 3 now
        mockMvc.perform(get("/api/users/stats").with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentCount", is(3)))
                .andExpect(jsonPath("$.total", is(7)));
    }

    @Test
    @DisplayName("Tình huống 8: Đổi role tài khoản: số lượng hai nhóm liên quan cập nhật đúng")
    void test8_ChangeRoleUpdatesBothCounts() throws Exception {
        // Change cutterB1 from USER to ADMIN
        UpdateUserRoleRequest roleReq = new UpdateUserRoleRequest();
        roleReq.setRole(Role.ADMIN);

        mockMvc.perform(patch("/api/users/" + cutterB1.getId() + "/role")
                .with(jwtAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roleReq)))
                .andExpect(status().isOk());

        // ADMIN should be 1 + 1 = 2, USER should be 3 - 1 = 2, AGENT stays 2
        mockMvc.perform(get("/api/users/stats").with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminCount", is(2)))
                .andExpect(jsonPath("$.agentCount", is(2)))
                .andExpect(jsonPath("$.userCount", is(2)))
                .andExpect(jsonPath("$.total", is(6)));
    }

    @Test
    @DisplayName("Tình huống 9: Xoá tài khoản: số lượng giảm đúng theo quy tắc nghiệp vụ")
    void test9_DeleteUserDecreasesCount() throws Exception {
        User standaloneUser = userRepository.save(User.builder()
                .keycloakUserId("kc-standalone-uuid")
                .username("standalone_cutter")
                .email("standalone_cutter@example.com")
                .fullName("Standalone Cutter")
                .role(Role.USER)
                .enabled(true)
                .deleted(false)
                .build());

        // Initial check: userCount is 3 + 1 = 4
        mockMvc.perform(get("/api/users/stats").with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCount", is(4)));

        // Delete standalone user
        mockMvc.perform(delete("/api/users/" + standaloneUser.getId()).with(jwtAdmin()))
                .andExpect(status().isNoContent());

        // USER count should decrease from 4 back to 3
        mockMvc.perform(get("/api/users/stats").with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCount", is(3)))
                .andExpect(jsonPath("$.total", is(6)));
    }

    @Test
    @DisplayName("Tình huống 10: Một đại lý có nhiều tài khoản thợ cắt: AGENT chỉ đếm quản lý đại lý, USER chỉ đếm thợ cắt")
    void test10_DealerWithMultipleCuttersCountedSeparately() throws Exception {
        // Dealer A has 1 AGENT (agentA) and 2 USER (cutterA1, cutterA2)
        mockMvc.perform(get("/api/users/stats?dealerId=" + dealerA.getId()).with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminCount", is(0)))
                .andExpect(jsonPath("$.agentCount", is(1)))
                .andExpect(jsonPath("$.userCount", is(2)))
                .andExpect(jsonPath("$.total", is(3)));
    }

    @Test
    @DisplayName("Bảo mật: USER gọi GET /api/users/stats bị 403 Forbidden")
    void testUserRoleForbidden() throws Exception {
        mockMvc.perform(get("/api/users/stats").with(jwtCutterA1()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Phân quyền: AGENT gọi GET /api/users/stats chỉ thấy thống kê trong scope của mình")
    void testAgentScopedStats() throws Exception {
        // Agent A has dealerA with agentA (AGENT) + cutterA1, cutterA2 (USER)
        mockMvc.perform(get("/api/users/stats").with(jwtAgentA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminCount", is(0)))
                .andExpect(jsonPath("$.agentCount", is(1)))
                .andExpect(jsonPath("$.userCount", is(2)))
                .andExpect(jsonPath("$.total", is(3)));
    }
}
