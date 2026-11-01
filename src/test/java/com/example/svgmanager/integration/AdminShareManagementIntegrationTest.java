package com.example.svgmanager.integration;

import com.example.svgmanager.dto.request.AdminRevokeShareRequest;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.UserSvgFile;
import com.example.svgmanager.entity.UserSvgFileShare;
import com.example.svgmanager.entity.v2.ClientSvgShare;
import com.example.svgmanager.entity.v2.UserSvgFileV2;
import com.example.svgmanager.entity.v2.UserSvgFileV2Share;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.repository.UserSvgFileRepository;
import com.example.svgmanager.repository.UserSvgFileShareRepository;
import com.example.svgmanager.repository.internal.v2.ClientSvgShareRepository;
import com.example.svgmanager.repository.internal.v2.UserSvgFileV2Repository;
import com.example.svgmanager.repository.internal.v2.UserSvgFileV2ShareRepository;
import com.example.svgmanager.service.internal.v2.EncryptedFileResult;
import com.example.svgmanager.service.internal.v2.FileEncryptionV2Service;
import com.example.svgmanager.service.internal.v2.FileStorageV2Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminShareManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSvgFileV2Repository userSvgFileV2Repository;

    @Autowired
    private UserSvgFileRepository userSvgFileRepository;

    @Autowired
    private ClientSvgShareRepository clientSvgShareRepository;

    @Autowired
    private UserSvgFileV2ShareRepository userSvgFileV2ShareRepository;

    @Autowired
    private UserSvgFileShareRepository userSvgFileShareRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileEncryptionV2Service fileEncryptionV2Service;

    @Autowired
    private FileStorageV2Service fileStorageV2Service;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private User adminUser;
    private User clientA;
    private User clientB;
    private UserSvgFileV2 v2File;
    private ClientSvgShare c2cShare;
    private UserSvgFileV2Share directV2Share;

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminJwt() {
        return jwt().jwt(jwt -> jwt.subject(adminUser.getKeycloakUserId())
                .claim("preferred_username", adminUser.getUsername())
                .claim("email", adminUser.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor userJwt(User user) {
        return jwt().jwt(jwt -> jwt.subject(user.getKeycloakUserId())
                .claim("preferred_username", user.getUsername())
                .claim("email", user.getEmail()))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @BeforeEach
    void setup() {
        adminUser = userRepository.save(User.builder()
                .username("admin_test_" + UUID.randomUUID())
                .email("admin_" + UUID.randomUUID() + "@test.com")
                .role(Role.ADMIN)
                .enabled(true)
                .keycloakUserId(UUID.randomUUID().toString())
                .build());

        clientA = userRepository.save(User.builder()
                .username("client_a_" + UUID.randomUUID())
                .email("clienta_" + UUID.randomUUID() + "@test.com")
                .role(Role.USER)
                .enabled(true)
                .keycloakUserId(UUID.randomUUID().toString())
                .build());

        clientB = userRepository.save(User.builder()
                .username("client_b_" + UUID.randomUUID())
                .email("clientb_" + UUID.randomUUID() + "@test.com")
                .role(Role.USER)
                .enabled(true)
                .keycloakUserId(UUID.randomUUID().toString())
                .build());

        // Create V2 encrypted SVG file for clientA
        String sampleSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"200\" height=\"200\"><circle cx=\"100\" cy=\"100\" r=\"80\" fill=\"blue\"/></svg>";
        byte[] plaintextBytes = sampleSvg.getBytes(StandardCharsets.UTF_8);
        EncryptedFileResult encResult = fileEncryptionV2Service.encrypt(plaintextBytes);
        String storageKey = fileStorageV2Service.storeEncryptedFile(encResult.ciphertext(), clientA.getId(), UUID.randomUUID().toString());

        v2File = userSvgFileV2Repository.save(UserSvgFileV2.builder()
                .fileName("Admin_Managed_Door.svg")
                .originalFileName("Admin_Managed_Door.svg")
                .storageKey(storageKey)
                .filePath(storageKey)
                .fileSize((long) plaintextBytes.length)
                .mimeType("image/svg+xml")
                .plaintextChecksum(encResult.plaintextChecksum())
                .encryptedChecksum(encResult.encryptedChecksum())
                .encryptionAlgorithm("AES-256-GCM")
                .encryptionKeyVersion("v1")
                .encryptedDek(encResult.encryptedDek())
                .encryptionIv(encResult.iv())
                .status("ACTIVE")
                .user(clientA)
                .build());

        // Create Client-to-Client Share (CLIENT_TO_CLIENT)
        c2cShare = clientSvgShareRepository.save(ClientSvgShare.builder()
                .shareId("01112026AdminT1")
                .userSvgFile(v2File)
                .ownerClientId("cutting-client")
                .ownerUser(clientA)
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .status("ACTIVE")
                .verifyCount(2)
                .previewCount(3)
                .downloadCount(1)
                .lastAccessedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        // Create Direct/Admin Share (ADMIN_TO_CLIENT)
        directV2Share = userSvgFileV2ShareRepository.save(UserSvgFileV2Share.builder()
                .userSvgFile(v2File)
                .sharedByUser(adminUser)
                .sharedToUser(clientB)
                .status("ACTIVE")
                .build());

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("ADMIN tra cứu danh sách tất cả lượt chia sẻ (Tab 1: Unified List)")
    void testAdminListAllShares() throws Exception {
        mockMvc.perform(get("/api/admin/shares")
                        .with(adminJwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[*].shareId", hasItem(c2cShare.getShareId())))
                .andExpect(jsonPath("$.content[*].shareType", hasItem("CLIENT_TO_CLIENT")))
                .andExpect(jsonPath("$.content[*].shareType", hasItem("ADMIN_TO_CLIENT")));
    }

    @Test
    @DisplayName("ADMIN lọc theo loại chia sẻ ADMIN_TO_CLIENT (Tab 2)")
    void testAdminFilterByAdminToClient() throws Exception {
        mockMvc.perform(get("/api/admin/shares")
                        .param("shareType", "ADMIN_TO_CLIENT")
                        .with(adminJwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].shareType", everyItem(is("ADMIN_TO_CLIENT"))));
    }

    @Test
    @DisplayName("ADMIN lọc theo loại chia sẻ CLIENT_TO_CLIENT (Tab 3)")
    void testAdminFilterByClientToClient() throws Exception {
        mockMvc.perform(get("/api/admin/shares")
                        .param("shareType", "CLIENT_TO_CLIENT")
                        .with(adminJwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].shareType", everyItem(is("CLIENT_TO_CLIENT"))))
                .andExpect(jsonPath("$.content[*].shareId", hasItem(c2cShare.getShareId())));
    }

    @Test
    @DisplayName("ADMIN xem chi tiết một lượt chia sẻ")
    void testAdminGetShareDetail() throws Exception {
        mockMvc.perform(get("/api/admin/shares/{shareId}", c2cShare.getShareId())
                        .with(adminJwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareId", is(c2cShare.getShareId())))
                .andExpect(jsonPath("$.fileName", is(v2File.getFileName())))
                .andExpect(jsonPath("$.sharedBy.username", is(clientA.getUsername())))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.verifyCount", is(2)))
                .andExpect(jsonPath("$.previewCount", is(3)))
                .andExpect(jsonPath("$.downloadCount", is(1)));
    }

    @Test
    @DisplayName("ADMIN xem thống kê tổng quan (Statistics Cards)")
    void testAdminGetStatistics() throws Exception {
        mockMvc.perform(get("/api/admin/shares/statistics")
                        .with(adminJwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalShares", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.clientShares", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.adminShares", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.activeShares", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.totalPreviews", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.totalDownloads", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("ADMIN xem toàn bộ lịch sử hoạt động chia sẻ file (Tab 4)")
    void testAdminListActivityHistory() throws Exception {
        mockMvc.perform(get("/api/admin/shares/activity-history")
                        .with(adminJwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("ADMIN thu hồi lượt chia sẻ Client Share thành công")
    void testAdminRevokeClientShare() throws Exception {
        AdminRevokeShareRequest revokeReq = new AdminRevokeShareRequest("Thu hồi vi phạm chính sách bảo mật");

        mockMvc.perform(post("/api/admin/shares/{shareId}/revoke", c2cShare.getShareId())
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revokeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareId", is(c2cShare.getShareId())))
                .andExpect(jsonPath("$.status", is("REVOKED")))
                .andExpect(jsonPath("$.revokeReason", containsString("Thu hồi vi phạm")));

        // Kiểm tra sau khi thu hồi: Client B không thể xác thực được nữa
        mockMvc.perform(post("/api/internal/v2/shares/{shareId}/verify", c2cShare.getShareId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"SecretPass123!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("CLIENT_SHARE_REVOKED")));
    }

    @Test
    @DisplayName("ADMIN thu hồi lượt chia sẻ Admin/Direct Share thành công")
    void testAdminRevokeDirectShare() throws Exception {
        AdminRevokeShareRequest revokeReq = new AdminRevokeShareRequest("Hết hạn hợp đồng đại lý");

        mockMvc.perform(post("/api/admin/shares/{shareId}/revoke", "DIR-V2-" + directV2Share.getId())
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revokeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REVOKED")));
    }

    @Test
    @DisplayName("Người dùng thường (USER) bị cấm truy cập API quản trị chia sẻ (403 Forbidden)")
    void testUserForbiddenFromAdminShareApis() throws Exception {
        mockMvc.perform(get("/api/admin/shares")
                        .with(userJwt(clientA))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/shares/statistics")
                        .with(userJwt(clientA))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/shares/{shareId}/revoke", c2cShare.getShareId())
                        .with(userJwt(clientA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"test\"}"))
                .andExpect(status().isForbidden());
    }
}
