package com.example.svgmanager.integration;

import com.example.svgmanager.entity.AuditLog;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.repository.AuditLogRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.service.KeycloakUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditTrailIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private KeycloakUserService keycloakUserService;

    private User testUser;
    private User testAgent;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findByUsername("audit_test_user").orElseGet(() ->
                userRepository.save(User.builder()
                        .username("audit_test_user")
                        .email("audit_test_user@gmail.com")
                        .fullName("Audit Test User")
                        .keycloakUserId("kc-audit-user-1")
                        .role(Role.USER)
                        .enabled(true)
                        .build())
        );

        testAgent = userRepository.findByUsername("audit_test_agent").orElseGet(() ->
                userRepository.save(User.builder()
                        .username("audit_test_agent")
                        .email("audit_test_agent@gmail.com")
                        .fullName("Audit Test Agent")
                        .keycloakUserId("kc-audit-agent-1")
                        .role(Role.AGENT)
                        .enabled(true)
                        .build())
        );
    }

    @Test
    @DisplayName("API gọi bởi USER tự động ghi nhận vào Audit Trail & Activity Log")
    void shouldLogApiCallByUser() throws Exception {
        long initialCount = auditLogRepository.count();

        mockMvc.perform(get("/api/part-library-categories/active")
                        .with(jwt().jwt(j -> j.subject("kc-audit-user-1").claim("preferred_username", "audit_test_user"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs.size()).isGreaterThan((int) initialCount);

        boolean found = logs.stream().anyMatch(l ->
                "audit_test_user".equals(l.getActor()) &&
                "USER".equals(l.getActorRole()) &&
                l.getAction().contains("GET /api/part-library-categories/active")
        );
        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("API gọi bởi AGENT (Đại lý) tự động ghi nhận vào Audit Trail & Activity Log")
    void shouldLogApiCallByAgent() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(jwt().jwt(j -> j.subject("kc-audit-agent-1").claim("preferred_username", "audit_test_agent"))
                                .authorities(new SimpleGrantedAuthority("ROLE_AGENT"))))
                .andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findAll();
        boolean found = logs.stream().anyMatch(l ->
                "audit_test_agent".equals(l.getActor()) &&
                "AGENT".equals(l.getActorRole()) &&
                l.getAction().contains("GET /api/users")
        );
        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("ADMIN có thể truy vấn danh sách Audit Trail từ /api/audit-logs")
    void adminCanQueryAuditLogs() throws Exception {
        // Tạo trước 1 bản ghi audit log
        auditLogRepository.save(AuditLog.builder()
                .actor("sample_actor")
                .actorRole("USER")
                .action("GET /api/internal/user-files")
                .entity("InternalUserFiles")
                .details("HTTP 200 (10ms)")
                .build());

        mockMvc.perform(get("/api/audit-logs")
                        .param("page", "0")
                        .param("size", "20")
                        .with(jwt().jwt(j -> j.subject("admin-sub").claim("preferred_username", "admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("USER gọi /api/audit-logs bị chặn 403 Forbidden")
    void userCannotAccessAuditLogs() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                        .with(jwt().jwt(j -> j.subject("user-sub").claim("preferred_username", "user1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }
}
