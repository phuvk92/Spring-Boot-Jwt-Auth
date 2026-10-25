package com.example.svgmanager.integration;

import com.example.svgmanager.dto.request.ResetPasswordRequest;
import com.example.svgmanager.dto.request.UpdateUserRequest;
import com.example.svgmanager.entity.Dealer;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KeycloakUserService keycloakUserService;

    private User admin;
    private User agent;
    private User targetUser;
    private User anotherUser;
    private Dealer dealer;

    @BeforeEach
    void setUp() {
        dealer = dealerRepository.save(Dealer.builder()
                .name("Đại lý Thử Nghiệm Hà Nội")
                .code("DL-HN-TEST")
                .status("ACTIVE")
                .deleted(false)
                .build());

        admin = userRepository.save(User.builder()
                .keycloakUserId("kc-admin-test-uuid")
                .username("admin_test_operator")
                .email("admin_test_operator@example.com")
                .fullName("Admin Test Operator")
                .role(Role.ADMIN)
                .enabled(true)
                .deleted(false)
                .build());

        agent = userRepository.save(User.builder()
                .keycloakUserId("kc-agent-test-uuid")
                .username("agent_test_manager")
                .email("agent_test_manager@example.com")
                .fullName("Agent Test Manager")
                .role(Role.AGENT)
                .dealer(dealer)
                .enabled(true)
                .deleted(false)
                .build());

        targetUser = userRepository.save(User.builder()
                .keycloakUserId("kc-target-user-uuid")
                .username("target_cutter_user")
                .email("target_cutter_user@example.com")
                .fullName("Thợ Cắt Cũ")
                .phone("0901234567")
                .role(Role.USER)
                .agent(agent)
                .dealer(dealer)
                .enabled(true)
                .deleted(false)
                .expirationDate(LocalDate.of(2026, 12, 31))
                .build());

        anotherUser = userRepository.save(User.builder()
                .keycloakUserId("kc-another-user-uuid")
                .username("another_user")
                .email("another_user@example.com")
                .fullName("Người Dùng Khác")
                .role(Role.USER)
                .enabled(true)
                .deleted(false)
                .build());
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

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTargetUser() {
        return jwt().jwt(builder -> builder
                .subject(targetUser.getKeycloakUserId())
                .claim("preferred_username", targetUser.getUsername())
                .claim("email", targetUser.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    @DisplayName("Admin chỉnh sửa người dùng thành công: cập nhật DB và đồng bộ Keycloak")
    void admin_UpdateUser_Success() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .email("new_target_email@example.com")
                .fullName("Thợ Cắt Đã Đổi Tên")
                .phone("0988776655")
                .role(Role.USER)
                .agentId(agent.getId())
                .dealerId(dealer.getId())
                .enabled(true)
                .expirationDate(LocalDate.of(2027, 6, 30))
                .maxDevices(2)
                .build();

        mockMvc.perform(put("/api/users/" + targetUser.getId())
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetUser.getId()))
                .andExpect(jsonPath("$.email").value("new_target_email@example.com"))
                .andExpect(jsonPath("$.fullName").value("Thợ Cắt Đã Đổi Tên"))
                .andExpect(jsonPath("$.phone").value("0988776655"))
                .andExpect(jsonPath("$.expirationDate").value("2027-06-30"))
                .andExpect(jsonPath("$.maxDevices").value(2));

        User updated = userRepository.findById(targetUser.getId()).orElseThrow();
        assertThat(updated.getEmail()).isEqualTo("new_target_email@example.com");
        assertThat(updated.getFullName()).isEqualTo("Thợ Cắt Đã Đổi Tên");
        assertThat(updated.getPhone()).isEqualTo("0988776655");
        assertThat(updated.getMaxDevices()).isEqualTo(2);

        verify(keycloakUserService).updateUser(eq("kc-target-user-uuid"), eq("new_target_email@example.com"), eq(true), eq("Thợ Cắt Đã Đổi Tên"));
    }

    @Test
    @DisplayName("Admin chỉnh sửa người dùng: đổi sang email đã tồn tại -> 409 Conflict")
    void admin_UpdateUser_DuplicateEmail_Conflict() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .email(anotherUser.getEmail())
                .fullName("Trùng Email")
                .build();

        mockMvc.perform(put("/api/users/" + targetUser.getId())
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Admin chỉnh sửa người dùng: gỡ bỏ đại lý và agent bằng ID 0")
    void admin_UpdateUser_ClearAgentAndDealer() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .email(targetUser.getEmail())
                .fullName(targetUser.getFullName())
                .agentId(0L)
                .dealerId(0L)
                .build();

        mockMvc.perform(put("/api/users/" + targetUser.getId())
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").doesNotExist())
                .andExpect(jsonPath("$.dealerId").doesNotExist());

        User updated = userRepository.findById(targetUser.getId()).orElseThrow();
        assertThat(updated.getAgent()).isNull();
        assertThat(updated.getDealer()).isNull();
    }

    @Test
    @DisplayName("Admin reset password cho user thành công -> 200 OK và gọi Keycloak resetPassword")
    void admin_ResetPassword_Success() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("NewSecurePass123!@#");

        mockMvc.perform(post("/api/users/" + targetUser.getId() + "/reset-password")
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đặt lại mật khẩu thành công"))
                .andExpect(jsonPath("$.userId").value(targetUser.getId()));

        verify(keycloakUserService).resetPassword("kc-target-user-uuid", "NewSecurePass123!@#");
    }

    @Test
    @DisplayName("Agent không có quyền reset password -> 403 Forbidden")
    void agent_ResetPassword_Forbidden() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("NewSecurePass123!@#");

        mockMvc.perform(post("/api/users/" + targetUser.getId() + "/reset-password")
                        .with(jwtAgent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User thường không có quyền reset password -> 403 Forbidden")
    void user_ResetPassword_Forbidden() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("NewSecurePass123!@#");

        mockMvc.perform(post("/api/users/" + targetUser.getId() + "/reset-password")
                        .with(jwtTargetUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin reset password với mật khẩu yếu vi phạm policy -> 400 Bad Request")
    void admin_ResetPassword_WeakPassword_BadRequest() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("weak");

        mockMvc.perform(post("/api/users/" + targetUser.getId() + "/reset-password")
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Admin reset password cho user không tồn tại -> 404 Not Found")
    void admin_ResetPassword_NotFound() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("NewSecurePass123!@#");

        mockMvc.perform(post("/api/users/999999/reset-password")
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tìm kiếm user theo user đại lý: trả về tất cả users thuộc đại lý đó")
    void searchUsers_ByAgentUsername_ReturnsAllUsersBelongingToDealer() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("search", "agent_test_manager")
                        .with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.username == 'agent_test_manager')]").exists())
                .andExpect(jsonPath("$.content[?(@.username == 'target_cutter_user')]").exists())
                .andExpect(jsonPath("$.content[?(@.username == 'another_user')]").doesNotExist());
    }

    @Test
    @DisplayName("Tìm kiếm user theo mã hoặc tên đại lý: trả về tất cả users thuộc đại lý đó")
    void searchUsers_ByDealerCodeOrName_ReturnsAllUsersBelongingToDealer() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("search", "DL-HN-TEST")
                        .with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.username == 'agent_test_manager')]").exists())
                .andExpect(jsonPath("$.content[?(@.username == 'target_cutter_user')]").exists())
                .andExpect(jsonPath("$.content[?(@.username == 'another_user')]").doesNotExist());
    }

    @Test
    @DisplayName("Lọc user theo dealerId: trả về tất cả users thuộc đại lý đó")
    void filterUsers_ByDealerId_ReturnsAllUsersBelongingToDealer() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("dealerId", String.valueOf(dealer.getId()))
                        .with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.username == 'agent_test_manager')]").exists())
                .andExpect(jsonPath("$.content[?(@.username == 'target_cutter_user')]").exists())
                .andExpect(jsonPath("$.content[?(@.username == 'another_user')]").doesNotExist());
    }

    @Test
    @DisplayName("Tìm kiếm user theo tên riêng của user: chỉ trả về user đó")
    void searchUsers_BySpecificUsername_ReturnsOnlyMatchingUser() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("search", "target_cutter_user")
                        .with(jwtAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.username == 'target_cutter_user')]").exists())
                .andExpect(jsonPath("$.content[?(@.username == 'agent_test_manager')]").doesNotExist())
                .andExpect(jsonPath("$.content[?(@.username == 'another_user')]").doesNotExist());
    }

    @Test
    @DisplayName("Admin tạo người dùng mới không cần agentId (chỉ chọn dealerId): tự động liên kết với agent của dealer")
    void admin_CreateUser_WithoutAgentId_Success() throws Exception {
        org.mockito.Mockito.when(keycloakUserService.createUser(any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn("kc-new-cutter-uuid");

        com.example.svgmanager.dto.request.CreateUserRequest request = com.example.svgmanager.dto.request.CreateUserRequest.builder()
                .username("new_cutter_no_agent")
                .email("new_cutter_no_agent@example.com")
                .password("StrongPass123!@#")
                .fullName("Thợ Cắt Mới")
                .role(Role.USER)
                .dealerId(dealer.getId())
                .enabled(true)
                .allocatedDays(360)
                .build();

        mockMvc.perform(post("/api/users")
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new_cutter_no_agent"))
                .andExpect(jsonPath("$.dealerId").value(dealer.getId()))
                .andExpect(jsonPath("$.agentId").value(agent.getId()));
    }

    @Test
    @DisplayName("Admin cập nhật người dùng không truyền agentId: agent tự động đồng bộ theo dealerId")
    void admin_UpdateUser_WithoutAgentId_SyncsAgentFromDealer() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .email(targetUser.getEmail())
                .fullName("Thợ Cắt Cập Nhật")
                .role(Role.USER)
                .dealerId(dealer.getId())
                .enabled(true)
                .build();

        mockMvc.perform(put("/api/users/" + targetUser.getId())
                        .with(jwtAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dealerId").value(dealer.getId()))
                .andExpect(jsonPath("$.agentId").value(agent.getId()));
    }
}
