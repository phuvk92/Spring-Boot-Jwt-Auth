package com.example.svgmanager.integration;

import com.example.svgmanager.dto.internal.v2.UserSvgFileV2UploadRequest;
import com.example.svgmanager.entity.Dealer;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.v2.AuditLogV2;
import com.example.svgmanager.entity.v2.UserSvgFileV2;
import com.example.svgmanager.repository.DealerRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.repository.internal.v2.AuditLogV2Repository;
import com.example.svgmanager.repository.internal.v2.UserSvgFileV2Repository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalApiV2AuditIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private AuditLogV2Repository auditLogV2Repository;

    @Autowired
    private UserSvgFileV2Repository userSvgFileV2Repository;

    private User ownerUser;
    private User secondUser;
    private User adminUser;
    private Dealer testDealer;

    private static final String SAMPLE_SVG =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"400\" height=\"300\"><circle cx=\"200\" cy=\"150\" r=\"80\" fill=\"#7C3AED\"/></svg>";

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUser(User u, String clientId) {
        return jwt().jwt(j -> {
            j.subject(u.getKeycloakUserId());
            j.claim("preferred_username", u.getUsername());
            if (clientId != null) {
                j.claim("azp", clientId);
            }
        }).authorities(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()));
    }

    @BeforeEach
    void setUp() {
        auditLogV2Repository.deleteAll();

        testDealer = dealerRepository.save(Dealer.builder()
                .name("Đại lý Audit Test " + UUID.randomUUID())
                .code("DL-AUDIT-" + UUID.randomUUID().toString().substring(0, 8))
                .address("123 Test St")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        ownerUser = userRepository.save(User.builder()
                .username("audit_owner_" + UUID.randomUUID())
                .email("owner_" + UUID.randomUUID() + "@example.com")
                .keycloakUserId("kc-owner-" + UUID.randomUUID())
                .role(Role.USER)
                .dealer(testDealer)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        secondUser = userRepository.save(User.builder()
                .username("audit_second_" + UUID.randomUUID())
                .email("second_" + UUID.randomUUID() + "@example.com")
                .keycloakUserId("kc-second-" + UUID.randomUUID())
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        adminUser = userRepository.save(User.builder()
                .username("audit_admin_" + UUID.randomUUID())
                .email("admin_" + UUID.randomUUID() + "@example.com")
                .keycloakUserId("kc-admin-" + UUID.randomUUID())
                .role(Role.ADMIN)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("1. Internal V2 upload SVG multipart thành công được ghi audit đầy đủ thông tin")
    void testSuccessfulV2UploadAndAudit() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test_hood.svg",
                "image/svg+xml",
                SAMPLE_SVG.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult result = mockMvc.perform(multipart("/api/internal/v2/user-files")
                        .file(file)
                        .param("fileName", "Test Hood")
                        .header("Content-Length", String.valueOf(file.getSize()))
                        .with(asUser(ownerUser, "cutting-client"))
                        .header("User-Agent", "CuttingApp/3.0 (Win64)"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Request-ID"))
                .andReturn();

        String respRequestId = result.getResponse().getHeader("X-Request-ID");
        assertNotNull(respRequestId);

        List<AuditLogV2> logs = auditLogV2Repository.findAll();
        assertFalse(logs.isEmpty(), "Audit log must be created");

        AuditLogV2 log = logs.stream()
                .filter(l -> respRequestId.equals(l.getRequestId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected audit record for requestId: " + respRequestId));

        assertEquals("POST", log.getHttpMethod());
        assertEquals("/api/internal/v2/user-files", log.getEndpoint());
        assertEquals("V2", log.getApiVersion());
        assertEquals(201, log.getHttpStatus());
        assertEquals("SUCCESS", log.getResult());
        assertNull(log.getErrorCode());
        assertEquals("cutting-client", log.getClientId());
        assertEquals("Cutting Machine Client", log.getClientName());
        assertEquals("USER", log.getPrincipalType());
        assertEquals(ownerUser.getId(), log.getUserId());
        assertEquals(ownerUser.getUsername(), log.getUsername());
        assertEquals("USER", log.getRole());
        assertEquals(testDealer.getId(), log.getDealerId());
        assertEquals(testDealer.getName(), log.getDealerName());
        assertTrue(log.getExecutionTimeMs() >= 0);
        assertNotNull(log.getClientIp());
        assertEquals("CuttingApp/3.0 (Win64)", log.getUserAgent());
        assertNotNull(log.getRequestSizeBytes());
        assertTrue(log.getRequestSizeBytes() > 0);
        assertNotNull(log.getResponseSizeBytes());
        assertTrue(log.getResponseSizeBytes() > 0);
    }

    @Test
    @DisplayName("2. GET, Preview, Download và DELETE file V2 được audit chính xác mà không lưu nội dung file")
    void testV2GetPreviewDownloadAndDeleteAudit() throws Exception {
        // Upload 1 file trước
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "door.svg",
                "image/svg+xml",
                SAMPLE_SVG.getBytes(StandardCharsets.UTF_8)
        );

        String uploadRes = mockMvc.perform(multipart("/api/internal/v2/user-files")
                        .file(file)
                        .param("fileName", "Door")
                        .with(asUser(ownerUser, "cutting-client")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(uploadRes);
        long fileId = json.get("id").asLong();

        // 2a. GET Detail
        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId)
                        .with(asUser(ownerUser, "cutting-client")))
                .andExpect(status().isOk());

        // 2b. GET Preview
        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/preview")
                        .with(asUser(ownerUser, "cutting-client")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("image/svg+xml")));

        // 2c. GET Download
        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/download")
                        .with(asUser(ownerUser, "cutting-client")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")));

        // 2d. DELETE
        mockMvc.perform(delete("/api/internal/v2/user-files/" + fileId)
                        .with(asUser(ownerUser, "cutting-client")))
                .andExpect(status().isNoContent());

        List<AuditLogV2> logs = auditLogV2Repository.findAll();
        // Upload (POST) + Detail (GET) + Preview (GET) + Download (GET) + Delete (DELETE) = 5 logs
        assertTrue(logs.size() >= 5);

        AuditLogV2 downloadLog = logs.stream()
                .filter(l -> l.getEndpoint().endsWith("/download"))
                .findFirst()
                .orElseThrow();
        assertEquals("GET", downloadLog.getHttpMethod());
        assertEquals(200, downloadLog.getHttpStatus());
        assertEquals("SUCCESS", downloadLog.getResult());
        assertTrue(downloadLog.getResponseSizeBytes() > 0);

        AuditLogV2 deleteLog = logs.stream()
                .filter(l -> "DELETE".equals(l.getHttpMethod()) && l.getEndpoint().endsWith("/" + fileId))
                .findFirst()
                .orElseThrow();
        assertEquals(204, deleteLog.getHttpStatus());
        assertEquals("SUCCESS", deleteLog.getResult());
    }

    @Test
    @DisplayName("3. Request không có xác thực (Unauthenticated) bị từ chối được ghi audit với principalType=ANONYMOUS")
    void testV2UnauthenticatedRequestAudit() throws Exception {
        mockMvc.perform(get("/api/internal/v2/user-files"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-ID"));

        List<AuditLogV2> logs = auditLogV2Repository.findAll();
        assertFalse(logs.isEmpty());

        AuditLogV2 log = logs.get(0);
        assertEquals(401, log.getHttpStatus());
        assertEquals("FAIL", log.getResult());
        assertEquals("ANONYMOUS", log.getPrincipalType());
        assertNull(log.getUserId());
        assertNull(log.getUsername());
        assertNull(log.getDealerId());
        assertEquals("UNAUTHORIZED", log.getErrorCode());
    }

    @Test
    @DisplayName("4. Request bị từ chối quyền (Forbidden) được ghi nhận lỗi FORBIDDEN và đúng danh tính người gọi")
    void testV2ForbiddenRequestAudit() throws Exception {
        // Owner tạo file
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "private.svg",
                "image/svg+xml",
                SAMPLE_SVG.getBytes(StandardCharsets.UTF_8)
        );

        String uploadRes = mockMvc.perform(multipart("/api/internal/v2/user-files")
                        .file(file)
                        .param("fileName", "Private")
                        .with(asUser(ownerUser, "cutting-client")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long fileId = objectMapper.readTree(uploadRes).get("id").asLong();

        // Second user cố tải file này (không được share) -> 403
        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/download")
                        .with(asUser(secondUser, "cutting-client")))
                .andExpect(status().isForbidden());

        List<AuditLogV2> logs = auditLogV2Repository.findAll();
        AuditLogV2 forbiddenLog = logs.stream()
                .filter(l -> secondUser.getUsername().equals(l.getUsername()))
                .findFirst()
                .orElseThrow();

        assertEquals(403, forbiddenLog.getHttpStatus());
        assertEquals("FAIL", forbiddenLog.getResult());
        assertEquals("USER_FILE_SHARE_FORBIDDEN", forbiddenLog.getErrorCode());
        assertEquals(secondUser.getId(), forbiddenLog.getUserId());
        assertEquals("USER", forbiddenLog.getPrincipalType());
    }

    @Test
    @DisplayName("5. Client Identity lấy từ claim JWT đã ký, không bị giả mạo bằng request header")
    void testClientIdentityCannotBeSpoofedByHeaders() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.svg",
                "image/svg+xml",
                SAMPLE_SVG.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/internal/v2/user-files")
                        .file(file)
                        .param("fileName", "Test")
                        .header("X-Client-Id", "fake-spoofed-client")
                        .header("X-Client-Name", "Spoofed Client App")
                        .with(asUser(ownerUser, "cutting-client")))
                .andExpect(status().isCreated());

        AuditLogV2 log = auditLogV2Repository.findAll().get(0);
        assertEquals("cutting-client", log.getClientId(), "Client ID phải lấy từ claim JWT đã ký");
        assertEquals("Cutting Machine Client", log.getClientName());
        assertNotEquals("fake-spoofed-client", log.getClientId());
    }

    @Test
    @DisplayName("6. Header X-Request-ID hợp lệ được giữ nguyên; Header không hợp lệ được thay thế bằng UUID")
    void testRequestIdHandlingAndSanitization() throws Exception {
        String validReqId = "custom-req-valid-12345678";

        mockMvc.perform(get("/api/internal/v2/user-files")
                        .header("X-Request-ID", validReqId)
                        .with(asUser(ownerUser, "cutting-client")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", validReqId));

        AuditLogV2 logValid = auditLogV2Repository.findAll().stream()
                .filter(l -> validReqId.equals(l.getRequestId()))
                .findFirst()
                .orElseThrow();
        assertEquals(validReqId, logValid.getRequestId());

        // Header có ký tự đặc biệt không an toàn (ví dụ injection payload)
        String unsafeReqId = "bad_req_id!@#$%^&*()";
        MvcResult res = mockMvc.perform(get("/api/internal/v2/user-files")
                        .header("X-Request-ID", unsafeReqId)
                        .with(asUser(ownerUser, "cutting-client")))
                .andExpect(status().isOk())
                .andReturn();

        String sanitizedId = res.getResponse().getHeader("X-Request-ID");
        assertNotNull(sanitizedId);
        assertNotEquals(unsafeReqId, sanitizedId);
        assertTrue(sanitizedId.length() >= 16);
    }

    @Test
    @DisplayName("7. Endpoint quản trị Audit Log V2: chỉ ADMIN truy cập được, AGENT và USER nhận 403")
    void testAdminAuditApiAccessControl() throws Exception {
        // ADMIN -> 200 OK
        mockMvc.perform(get("/api/admin/audit/v2")
                        .with(asUser(adminUser, "cutting-manager-web")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // USER -> 403 Forbidden
        mockMvc.perform(get("/api/admin/audit/v2")
                        .with(asUser(ownerUser, "cutting-manager-web")))
                .andExpect(status().isForbidden());

        // AGENT -> 403 Forbidden
        User agentUser = userRepository.save(User.builder()
                .username("audit_agent_" + UUID.randomUUID())
                .email("agent_" + UUID.randomUUID() + "@example.com")
                .keycloakUserId("kc-agent-" + UUID.randomUUID())
                .role(Role.AGENT)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/api/admin/audit/v2")
                        .with(asUser(agentUser, "cutting-manager-web")))
                .andExpect(status().isForbidden());

        // Anonymous -> 401 Unauthorized
        mockMvc.perform(get("/api/admin/audit/v2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("8. Admin Audit API: Phân trang, Bộ lọc, Thống kê Dashboard và Export CSV trả dữ liệu chính xác")
    void testAdminAuditApiFilteringStatsAndExport() throws Exception {
        // Tạo audit bằng vài request
        mockMvc.perform(get("/api/internal/v2/user-files")
                        .with(asUser(ownerUser, "cutting-client")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/internal/v2/user-files/999999")
                        .with(asUser(ownerUser, "cutting-client")))
                .andExpect(status().isNotFound());

        // 8a. Filter by result=SUCCESS
        mockMvc.perform(get("/api/admin/audit/v2")
                        .param("result", "SUCCESS")
                        .with(asUser(adminUser, "cutting-manager-web")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].result").value("SUCCESS"));

        // 8b. Filter by result=FAIL
        mockMvc.perform(get("/api/admin/audit/v2")
                        .param("result", "FAIL")
                        .with(asUser(adminUser, "cutting-manager-web")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].httpStatus").value(404));

        // 8c. Statistics API
        mockMvc.perform(get("/api/admin/audit/v2/stats")
                        .with(asUser(adminUser, "cutting-manager-web")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(2))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failCount").value(1))
                .andExpect(jsonPath("$.successRate").value(50.0))
                .andExpect(jsonPath("$.topEndpoints").isArray())
                .andExpect(jsonPath("$.topClients").isArray())
                .andExpect(jsonPath("$.topUsers").isArray());

        // 8d. Export CSV
        MvcResult csvResult = mockMvc.perform(get("/api/admin/audit/v2/export")
                        .with(asUser(adminUser, "cutting-manager-web")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andReturn();

        String csv = csvResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(csv.contains("Request ID"));
        assertTrue(csv.contains("cutting-client"));
        assertTrue(csv.contains("SUCCESS"));
    }
}
