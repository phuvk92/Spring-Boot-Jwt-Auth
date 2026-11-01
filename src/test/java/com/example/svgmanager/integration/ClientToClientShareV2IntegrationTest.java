package com.example.svgmanager.integration;

import com.example.svgmanager.dto.internal.v2.CreateClientShareRequest;
import com.example.svgmanager.dto.internal.v2.RevokeClientShareRequest;
import com.example.svgmanager.dto.internal.v2.VerifyClientShareRequest;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.v2.ClientSvgShare;
import com.example.svgmanager.entity.v2.UserSvgFileV2;
import com.example.svgmanager.repository.AuditLogRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.repository.internal.v2.ClientSvgShareRepository;
import com.example.svgmanager.repository.internal.v2.UserSvgFileV2Repository;
import com.example.svgmanager.service.internal.v2.FileEncryptionV2Service;
import com.example.svgmanager.service.internal.v2.FileStorageV2Service;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ClientToClientShareV2IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSvgFileV2Repository userSvgFileV2Repository;

    @Autowired
    private ClientSvgShareRepository clientSvgShareRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileStorageV2Service fileStorageV2Service;

    @Autowired
    private FileEncryptionV2Service fileEncryptionV2Service;

    private User clientA;
    private User clientB;
    private User clientC;
    private User adminUser;

    private Long fileAId;

    private static final String SAMPLE_SVG =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"500\" height=\"400\"><rect width=\"500\" height=\"400\" fill=\"#4F46E5\"/><text x=\"50\" y=\"200\" font-size=\"24\" fill=\"#FFF\">Pcut Client Share Test</text></svg>";

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUser(User u) {
        return jwt().jwt(j -> j.subject(u.getKeycloakUserId()).claim("preferred_username", u.getUsername()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()));
    }

    @BeforeEach
    void setUp() throws Exception {
        clientA = userRepository.save(User.builder()
                .username("client_a_" + UUID.randomUUID().toString().substring(0, 8))
                .email("client_a_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com")
                .fullName("Thợ Cắt A")
                .keycloakUserId("kc-client-a-" + UUID.randomUUID())
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        clientB = userRepository.save(User.builder()
                .username("client_b_" + UUID.randomUUID().toString().substring(0, 8))
                .email("client_b_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com")
                .fullName("Thợ Cắt B")
                .keycloakUserId("kc-client-b-" + UUID.randomUUID())
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        clientC = userRepository.save(User.builder()
                .username("client_c_" + UUID.randomUUID().toString().substring(0, 8))
                .email("client_c_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com")
                .fullName("Thợ Cắt C")
                .keycloakUserId("kc-client-c-" + UUID.randomUUID())
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        adminUser = userRepository.save(User.builder()
                .username("admin_" + UUID.randomUUID().toString().substring(0, 8))
                .email("admin_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com")
                .fullName("Quản Trị Viên")
                .keycloakUserId("kc-admin-" + UUID.randomUUID())
                .role(Role.ADMIN)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        // Upload an encrypted SVG V2 file owned by Client A
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "camry_door_left.svg", "image/svg+xml", SAMPLE_SVG.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/internal/v2/user-files")
                        .file(filePart)
                        .param("fileName", "Bản vẽ cửa trước Camry")
                        .with(asUser(clientA)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        fileAId = uploadJson.get("id").asLong();
        assertTrue(fileAId > 0);
    }

    @Test
    @DisplayName("1. Client A tạo Share thành công: Share ID đúng định dạng ddMMyyyy + 6 ký tự, Password được hash")
    void testClientACreateShareSuccess() throws Exception {
        CreateClientShareRequest req = new CreateClientShareRequest(
                fileAId, "Pass1234!", LocalDateTime.now().plusDays(7), "Share test for Client B"
        );

        MvcResult res = mockMvc.perform(post("/api/internal/v2/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(asUser(clientA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shareId").isString())
                .andExpect(jsonPath("$.fileId").value(fileAId))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist()) // Không trả password plaintext
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        String shareId = json.get("shareId").asText();

        // Kiểm tra format: ddMMyyyy (Vietnam time) + 6 ký tự alphanumeric
        String todayVn = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        assertTrue(shareId.startsWith(todayVn), "Share ID phải bắt đầu bằng ddMMyyyy: " + todayVn);
        assertEquals(14, shareId.length(), "Share ID phải có độ dài 14 (8 số ngày + 6 ký tự ngẫu nhiên)");
        Pattern pattern = Pattern.compile("^\\d{8}[A-Za-z0-9]{6}$");
        assertTrue(pattern.matcher(shareId).matches(), "Share ID phải khớp pattern ddMMyyyy + 6 ký tự");

        // Kiểm tra trong DB: password được hash, không lưu plaintext
        ClientSvgShare entity = clientSvgShareRepository.findByShareId(shareId).orElseThrow();
        assertNotEquals("Pass1234!", entity.getPasswordHash());
        assertTrue(passwordEncoder.matches("Pass1234!", entity.getPasswordHash()));
        assertEquals(clientA.getId(), entity.getOwnerUser().getId());
        assertEquals("ACTIVE", entity.getStatus());
    }

    @Test
    @DisplayName("2. Client A xem được danh sách Share của mình, không xem được Share của Client C")
    void testClientAListOnlyOwnShares() throws Exception {
        // Client A creates share 1
        mockMvc.perform(post("/api/internal/v2/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateClientShareRequest(fileAId, "PassA123!", null, "A's share")))
                        .with(asUser(clientA)))
                .andExpect(status().isCreated());

        // Upload file for Client C
        MockMultipartFile fileC = new MockMultipartFile("file", "c_file.svg", "image/svg+xml", SAMPLE_SVG.getBytes(StandardCharsets.UTF_8));
        MvcResult uploadC = mockMvc.perform(multipart("/api/internal/v2/user-files")
                        .file(fileC)
                        .param("fileName", "File của C")
                        .with(asUser(clientC)))
                .andExpect(status().isCreated())
                .andReturn();
        Long fileCId = objectMapper.readTree(uploadC.getResponse().getContentAsString()).get("id").asLong();

        // Client C creates share
        mockMvc.perform(post("/api/internal/v2/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateClientShareRequest(fileCId, "PassC123!", null, "C's share")))
                        .with(asUser(clientC)))
                .andExpect(status().isCreated());

        // Client A lists shares -> only gets A's share
        mockMvc.perform(get("/api/internal/v2/shares")
                        .with(asUser(clientA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].note").value("A's share"));

        // Client C lists shares -> only gets C's share
        mockMvc.perform(get("/api/internal/v2/shares")
                        .with(asUser(clientC)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].note").value("C's share"));

        // Admin lists shares -> can see both
        mockMvc.perform(get("/api/internal/v2/shares")
                        .with(asUser(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("3. Client A xem chi tiết Share của mình; Client B hoặc C bị 403 Forbidden nếu xem share của A")
    void testGetShareDetailAccessControl() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/internal/v2/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateClientShareRequest(fileAId, "Pass1234!", null, "Secret share")))
                        .with(asUser(clientA)))
                .andExpect(status().isCreated())
                .andReturn();
        String shareId = objectMapper.readTree(res.getResponse().getContentAsString()).get("shareId").asText();

        // Client A gets detail -> OK
        mockMvc.perform(get("/api/internal/v2/shares/" + shareId)
                        .with(asUser(clientA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareId").value(shareId))
                .andExpect(jsonPath("$.password").doesNotExist());

        // Client C gets detail of A's share -> 403 Forbidden
        mockMvc.perform(get("/api/internal/v2/shares/" + shareId)
                        .with(asUser(clientC)))
                .andExpect(status().isForbidden());

        // Admin gets detail -> OK
        mockMvc.perform(get("/api/internal/v2/shares/" + shareId)
                        .with(asUser(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareId").value(shareId));
    }

    @Test
    @DisplayName("4. Client B xác thực đúng password -> nhận Share Token; xác thực sai -> 400 Bad Request")
    void testClientBVerifyShare() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/internal/v2/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateClientShareRequest(fileAId, "CorrectPass1!", null, "Verify test")))
                        .with(asUser(clientA)))
                .andExpect(status().isCreated())
                .andReturn();
        String shareId = objectMapper.readTree(res.getResponse().getContentAsString()).get("shareId").asText();

        // Verify with wrong password -> 400
        mockMvc.perform(post("/api/internal/v2/shares/" + shareId + "/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyClientShareRequest("WrongPass!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_SHARE_INVALID_PASSWORD"));

        // Verify with correct password -> 200 OK + token
        MvcResult verifyRes = mockMvc.perform(post("/api/internal/v2/shares/" + shareId + "/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyClientShareRequest("CorrectPass1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareToken").isString())
                .andExpect(jsonPath("$.shareId").value(shareId))
                .andExpect(jsonPath("$.fileId").value(fileAId))
                .andReturn();

        String token = objectMapper.readTree(verifyRes.getResponse().getContentAsString()).get("shareToken").asText();
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("5. Client B Preview và Download SVG thành công khi Share ACTIVE, giải mã on-the-fly")
    void testClientBPreviewAndDownloadSuccess() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/internal/v2/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateClientShareRequest(fileAId, "Pass1234!", null, "Download test")))
                        .with(asUser(clientA)))
                .andExpect(status().isCreated())
                .andReturn();
        String shareId = objectMapper.readTree(res.getResponse().getContentAsString()).get("shareId").asText();

        // Client B verifies
        MvcResult verifyRes = mockMvc.perform(post("/api/internal/v2/shares/" + shareId + "/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyClientShareRequest("Pass1234!"))))
                .andExpect(status().isOk())
                .andReturn();
        String shareToken = objectMapper.readTree(verifyRes.getResponse().getContentAsString()).get("shareToken").asText();

        // Preview with X-Share-Token header
        MvcResult previewRes = mockMvc.perform(get("/api/internal/v2/shares/" + shareId + "/preview")
                        .header("X-Share-Token", shareToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("image/svg+xml")))
                .andReturn();

        String previewContent = previewRes.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(previewContent.contains("<svg") && previewContent.contains("Pcut Client Share Test"));

        // Download with ?token= query param
        MvcResult downloadRes = mockMvc.perform(get("/api/internal/v2/shares/" + shareId + "/download")
                        .param("token", shareToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("image/svg+xml")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andReturn();

        String downloadContent = downloadRes.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(downloadContent.contains("<svg") && downloadContent.contains("Pcut Client Share Test"));

        // Check stats in DB
        ClientSvgShare shareInDb = clientSvgShareRepository.findByShareId(shareId).orElseThrow();
        assertEquals(1, shareInDb.getVerifyCount());
        assertEquals(1, shareInDb.getPreviewCount());
        assertEquals(1, shareInDb.getDownloadCount());
        assertNotNull(shareInDb.getLastAccessedAt());
    }

    @Test
    @DisplayName("6. Client A Unshare thành công; Token của B lập tức mất hiệu lực; File gốc không bị xoá; Gọi lặp lại an toàn")
    void testClientAUnshareRevocation() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/internal/v2/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateClientShareRequest(fileAId, "Pass1234!", null, "Revoke test")))
                        .with(asUser(clientA)))
                .andExpect(status().isCreated())
                .andReturn();
        String shareId = objectMapper.readTree(res.getResponse().getContentAsString()).get("shareId").asText();

        // Client B verifies and gets a token
        MvcResult verifyRes = mockMvc.perform(post("/api/internal/v2/shares/" + shareId + "/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyClientShareRequest("Pass1234!"))))
                .andExpect(status().isOk())
                .andReturn();
        String clientBToken = objectMapper.readTree(verifyRes.getResponse().getContentAsString()).get("shareToken").asText();

        // Verify token works before revoke
        mockMvc.perform(get("/api/internal/v2/shares/" + shareId + "/preview")
                        .header("X-Share-Token", clientBToken))
                .andExpect(status().isOk());

        // Client C tries to unshare A's share -> 403 Forbidden
        mockMvc.perform(delete("/api/internal/v2/shares/" + shareId)
                        .with(asUser(clientC)))
                .andExpect(status().isForbidden());

        // Client A unshares
        RevokeClientShareRequest revokeReq = new RevokeClientShareRequest("Công trình đã hoàn thành");
        mockMvc.perform(delete("/api/internal/v2/shares/" + shareId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revokeReq))
                        .with(asUser(clientA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.revokeReason").value("Công trình đã hoàn thành"));

        // Idempotency: Client A calls delete again -> still 200 OK without error
        mockMvc.perform(delete("/api/internal/v2/shares/" + shareId)
                        .with(asUser(clientA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));

        // Client B tries to verify again with old shareId -> 400 Bad Request (REVOKED)
        mockMvc.perform(post("/api/internal/v2/shares/" + shareId + "/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyClientShareRequest("Pass1234!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_SHARE_REVOKED"));

        // Client B tries to preview with existing token -> 403 Forbidden (REVOKED immediately!)
        mockMvc.perform(get("/api/internal/v2/shares/" + shareId + "/preview")
                        .header("X-Share-Token", clientBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CLIENT_SHARE_REVOKED"));

        // Client B tries to download with existing token -> 403 Forbidden
        mockMvc.perform(get("/api/internal/v2/shares/" + shareId + "/download")
                        .header("X-Share-Token", clientBToken))
                .andExpect(status().isForbidden());

        // File gốc của Client A vẫn tồn tại, không bị xoá
        UserSvgFileV2 originalFile = userSvgFileV2Repository.findById(fileAId).orElseThrow();
        assertEquals("ACTIVE", originalFile.getStatus());
        assertTrue(originalFile.getFileName().contains("Bản vẽ cửa trước Camry"));
    }

    @Test
    @DisplayName("7. Tạo lại lượt chia sẻ cho cùng file sau khi thu hồi: sinh Share ID mới")
    void testReShareFileGeneratesNewShareId() throws Exception {
        // Create share 1
        MvcResult res1 = mockMvc.perform(post("/api/internal/v2/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateClientShareRequest(fileAId, "Pass1!", null, "Share 1")))
                        .with(asUser(clientA)))
                .andExpect(status().isCreated())
                .andReturn();
        String shareId1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("shareId").asText();

        // Revoke share 1
        mockMvc.perform(delete("/api/internal/v2/shares/" + shareId1)
                        .with(asUser(clientA)))
                .andExpect(status().isOk());

        // Create share 2 for same file
        MvcResult res2 = mockMvc.perform(post("/api/internal/v2/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateClientShareRequest(fileAId, "Pass2!", null, "Share 2")))
                        .with(asUser(clientA)))
                .andExpect(status().isCreated())
                .andReturn();
        String shareId2 = objectMapper.readTree(res2.getResponse().getContentAsString()).get("shareId").asText();

        assertNotEquals(shareId1, shareId2, "Mỗi lần tạo share mới phải sinh Share ID mới");
        assertEquals("REVOKED", clientSvgShareRepository.findByShareId(shareId1).orElseThrow().getStatus());
        assertEquals("ACTIVE", clientSvgShareRepository.findByShareId(shareId2).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("8. Chống brute-force: nhập sai mật khẩu 5 lần bị khóa tạm thời với RATE_LIMIT_EXCEEDED")
    void testBruteForceProtectionOnVerify() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/internal/v2/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateClientShareRequest(fileAId, "RealSecretPass!", null, "Brute force test")))
                        .with(asUser(clientA)))
                .andExpect(status().isCreated())
                .andReturn();
        String shareId = objectMapper.readTree(res.getResponse().getContentAsString()).get("shareId").asText();

        // 5 consecutive wrong password attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/internal/v2/shares/" + shareId + "/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VerifyClientShareRequest("WrongPass" + i))))
                    .andExpect(status().isBadRequest());
        }

        // 6th attempt (even with right password) is locked out
        mockMvc.perform(post("/api/internal/v2/shares/" + shareId + "/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyClientShareRequest("RealSecretPass!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("9. Audit Log ghi nhận đầy đủ sự kiện: CREATED, LISTED, VIEWED, REVOKED, VERIFY_SUCCESS, VERIFY_FAILED, PREVIEWED, DOWNLOADED")
    void testAuditLogEventsRecorded() throws Exception {
        // Create
        MvcResult res = mockMvc.perform(post("/api/internal/v2/shares")
                        .header("X-Request-ID", "req-create-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateClientShareRequest(fileAId, "Pass1234!", null, "Audit test")))
                        .with(asUser(clientA)))
                .andExpect(status().isCreated())
                .andReturn();
        String shareId = objectMapper.readTree(res.getResponse().getContentAsString()).get("shareId").asText();

        // List
        mockMvc.perform(get("/api/internal/v2/shares")
                        .header("X-Request-ID", "req-list-12345")
                        .with(asUser(clientA)))
                .andExpect(status().isOk());

        // View detail
        mockMvc.perform(get("/api/internal/v2/shares/" + shareId)
                        .header("X-Request-ID", "req-view-12345")
                        .with(asUser(clientA)))
                .andExpect(status().isOk());

        // Verify fail
        mockMvc.perform(post("/api/internal/v2/shares/" + shareId + "/verify")
                        .header("X-Request-ID", "req-fail-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyClientShareRequest("BadPass"))))
                .andExpect(status().isBadRequest());

        // Verify success
        MvcResult vRes = mockMvc.perform(post("/api/internal/v2/shares/" + shareId + "/verify")
                        .header("X-Request-ID", "req-verify-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyClientShareRequest("Pass1234!"))))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(vRes.getResponse().getContentAsString()).get("shareToken").asText();

        // Preview
        mockMvc.perform(get("/api/internal/v2/shares/" + shareId + "/preview")
                        .header("X-Request-ID", "req-prev-12345")
                        .header("X-Share-Token", token))
                .andExpect(status().isOk());

        // Download
        mockMvc.perform(get("/api/internal/v2/shares/" + shareId + "/download")
                        .header("X-Request-ID", "req-down-12345")
                        .header("X-Share-Token", token))
                .andExpect(status().isOk());

        // Revoke
        mockMvc.perform(delete("/api/internal/v2/shares/" + shareId)
                        .header("X-Request-ID", "req-revoke-12345")
                        .with(asUser(clientA)))
                .andExpect(status().isOk());

        // Verify audit logs exist
        assertTrue(auditLogRepository.findAll().stream().anyMatch(l -> "CLIENT_SHARE_CREATED".equals(l.getAction())));
        assertTrue(auditLogRepository.findAll().stream().anyMatch(l -> "CLIENT_SHARE_LISTED".equals(l.getAction())));
        assertTrue(auditLogRepository.findAll().stream().anyMatch(l -> "CLIENT_SHARE_VIEWED".equals(l.getAction())));
        assertTrue(auditLogRepository.findAll().stream().anyMatch(l -> "CLIENT_SHARE_VERIFY_FAILED".equals(l.getAction())));
        assertTrue(auditLogRepository.findAll().stream().anyMatch(l -> "CLIENT_SHARE_VERIFY_SUCCESS".equals(l.getAction())));
        assertTrue(auditLogRepository.findAll().stream().anyMatch(l -> "CLIENT_SHARE_PREVIEWED".equals(l.getAction())));
        assertTrue(auditLogRepository.findAll().stream().anyMatch(l -> "CLIENT_SHARE_DOWNLOADED".equals(l.getAction())));
        assertTrue(auditLogRepository.findAll().stream().anyMatch(l -> "CLIENT_SHARE_REVOKED".equals(l.getAction())));

        // Verify NO password or SVG content in any audit details
        auditLogRepository.findAll().forEach(l -> {
            if (l.getDetails() != null) {
                assertFalse(l.getDetails().contains("Pass1234!"), "Password không được xuất hiện trong audit log!");
                assertFalse(l.getDetails().contains("<svg"), "SVG content không được xuất hiện trong audit log!");
            }
        });
    }

    @Test
    @DisplayName("10. Tương thích: Admin share cho Client theo cơ chế cũ không bị ảnh hưởng")
    void testAdminShareCompatibility() throws Exception {
        // Admin shares fileA to recipientUser using V2 UserSvgFileV2Share
        mockMvc.perform(post("/api/admin/v2/user-files/" + fileAId + "/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.example.svgmanager.dto.internal.v2.UserSvgFileV2ShareRequest(clientB.getId())))
                        .with(asUser(adminUser)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.userId").value(clientB.getId()));
    }
}
