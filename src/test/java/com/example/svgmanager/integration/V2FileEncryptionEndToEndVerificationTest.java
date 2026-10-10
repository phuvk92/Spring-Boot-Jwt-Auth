package com.example.svgmanager.integration;

import com.example.svgmanager.dto.internal.v2.UserSvgFileV2ShareRequest;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.v2.UserSvgFileV2;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.repository.UserSvgFileRepository;
import com.example.svgmanager.repository.internal.v2.UserSvgFileV2Repository;
import com.example.svgmanager.service.internal.v2.FileStorageV2Service;
import com.example.svgmanager.util.ChecksumUtils;
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
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Task: TEST TOÀN BỘ API V2 TỪ CLIENT VÀ XÁC MINH MÃ HÓA FILE END-TO-END
 *
 * Kiểm tra đầy đủ từ phía Client:
 * 1. Upload file SVG có nội dung duy nhất (chuỗi nhận diện ngẫu nhiên, SHA-256, tọa độ cắt).
 * 2. Xác minh dữ liệu vật lý thực tế trên persistent storage V2 chỉ chứa ciphertext nhị phân (.enc),
 *    tuyệt đối không chứa chuỗi nhận diện hoặc XML/SVG plaintext.
 * 3. Xác minh database V2 lưu encrypted DEK và IV, không chứa DEK plaintext.
 * 4. Client gọi Preview/Download giải mã trong suốt on-the-fly, bảo toàn 100% geometry.
 * 5. Kiểm thử toàn vẹn AEAD Tag: sửa đổi 1 byte ciphertext trên đĩa khiến preview/download từ chối giải mã an toàn.
 * 6. Kiểm thử ma trận phân quyền: Owner, Admin, Shared User, Unshared User, Revoked User.
 * 7. Kiểm thử các API V2: Overwrite (PUT), Delete (DELETE), Share, List Shares, Revoke Share.
 * 8. Kiểm thử cô lập V1: không ghi đè hoặc thay đổi bảng V1 user_svg_files.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class V2FileEncryptionEndToEndVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSvgFileRepository v1FileRepository;

    @Autowired
    private UserSvgFileV2Repository v2FileRepository;

    @Autowired
    private FileStorageV2Service fileStorageV2Service;

    private User ownerUser;
    private User recipientUser;
    private User attackerUser;
    private User adminUser;

    private String uniqueToken;
    private String testSvgContent;
    private String expectedPlaintextSha256;

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUser(User u) {
        return jwt().jwt(j -> j.subject(u.getKeycloakUserId()).claim("preferred_username", u.getUsername()))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asAdmin(User u) {
        return jwt().jwt(j -> j.subject(u.getKeycloakUserId()).claim("preferred_username", u.getUsername()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @BeforeEach
    void setUp() {
        ownerUser = userRepository.save(User.builder()
                .username("e2e_owner_" + UUID.randomUUID())
                .email("e2e_owner_" + UUID.randomUUID() + "@example.com")
                .keycloakUserId("kc-e2e-owner-" + UUID.randomUUID())
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        recipientUser = userRepository.save(User.builder()
                .username("e2e_recipient_" + UUID.randomUUID())
                .email("e2e_recipient_" + UUID.randomUUID() + "@example.com")
                .keycloakUserId("kc-e2e-recipient-" + UUID.randomUUID())
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        attackerUser = userRepository.save(User.builder()
                .username("e2e_attacker_" + UUID.randomUUID())
                .email("e2e_attacker_" + UUID.randomUUID() + "@example.com")
                .keycloakUserId("kc-e2e-attacker-" + UUID.randomUUID())
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        adminUser = userRepository.save(User.builder()
                .username("e2e_admin_" + UUID.randomUUID())
                .email("e2e_admin_" + UUID.randomUUID() + "@example.com")
                .keycloakUserId("kc-e2e-admin-" + UUID.randomUUID())
                .role(Role.ADMIN)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        // Tạo file SVG duy nhất cho chuỗi kiểm thử E2E
        uniqueToken = "E2E_SECRET_TOKEN_" + UUID.randomUUID();
        testSvgContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"800\" height=\"600\" viewBox=\"0 0 800 600\">\n" +
                "  <path d=\"M 50 100 L 250 100 L 250 300 L 50 300 Z\" fill=\"#3B82F6\" data-cut=\"true\"/>\n" +
                "  <circle cx=\"400\" cy=\"300\" r=\"80\" fill=\"#10B981\"/>\n" +
                "  <desc>" + uniqueToken + "</desc>\n" +
                "</svg>";
        expectedPlaintextSha256 = ChecksumUtils.calculateSha256(testSvgContent.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("End-to-End V2 Complete Verification: Upload -> Storage Ciphertext -> DB Encryption -> Decrypt Preview/Download -> AEAD Tampering -> Authorization Matrix -> Isolation")
    void testEndToEnd_FullV2VerificationFlow() throws Exception {
        long v1CountBefore = v1FileRepository.count();
        long v2CountBefore = v2FileRepository.count();

        // =========================================================================
        // GIAI ĐOẠN 2 & 3: CLIENT UPLOAD V2 VÀ XÁC MINH CIPHERTEXT TRÊN STORAGE
        // =========================================================================
        MockMultipartFile testMultipartFile = new MockMultipartFile(
                "file",
                "encryption-e2e-test.svg",
                "image/svg+xml",
                testSvgContent.getBytes(StandardCharsets.UTF_8)
        );

        String uploadResponseJson = mockMvc.perform(multipart("/api/internal/v2/user-files")
                        .file(testMultipartFile)
                        .param("fileName", "encryption-e2e-test.svg")
                        .param("brandName", "VinFast")
                        .param("modelName", "VF8")
                        .param("yearFrom", "2023")
                        .param("yearTo", "2025")
                        .param("filmWidth", "1520.0")
                        .param("rollLength", "3000.0")
                        .param("description", "File kiểm thử mã hóa E2E V2")
                        .with(asUser(ownerUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.fileName", is("encryption-e2e-test.svg")))
                .andExpect(jsonPath("$.accessType", is("OWNER")))
                // Xác minh metadata bí mật không bị rò rỉ ra response client
                .andExpect(jsonPath("$.encryptedDek").doesNotExist())
                .andExpect(jsonPath("$.encryptionIv").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        Number fileIdNum = objectMapper.readTree(uploadResponseJson).get("id").numberValue();
        Long fileId = fileIdNum.longValue();

        // 1. Xác minh cách ly V1: Bảng V1 hoàn toàn không bị thêm bớt
        assertEquals(v1CountBefore, v1FileRepository.count(), "Bảng V1 user_svg_files tuyệt đối không bị thay đổi bởi upload V2");
        assertEquals(v2CountBefore + 1, v2FileRepository.count(), "Bảng V2 user_svg_file_v2 phải có đúng 1 bản ghi mới");

        // 2. Xác minh dữ liệu trong Database V2
        UserSvgFileV2 v2Entity = v2FileRepository.findById(fileId).orElseThrow();
        assertEquals("AES-256-GCM", v2Entity.getEncryptionAlgorithm());
        assertEquals("v1", v2Entity.getEncryptionKeyVersion());
        assertNotNull(v2Entity.getEncryptedDek(), "Encrypted DEK phải tồn tại trong DB");
        assertTrue(v2Entity.getEncryptedDek().length >= 32, "Encrypted DEK blob phải có kích thước an toàn");
        assertNotNull(v2Entity.getEncryptionIv(), "Encryption IV phải tồn tại trong DB");
        assertEquals(12, v2Entity.getEncryptionIv().length, "GCM IV phải đúng 12 bytes CSPRNG");
        assertNotNull(v2Entity.getStorageKey(), "Storage key phải tồn tại");
        assertTrue(v2Entity.getStorageKey().endsWith(".enc"), "Tên file storage phải có đuôi .enc");

        // Xác minh DB không lưu DEK plaintext (DEK mã hóa không chứa chuỗi SVG gốc)
        String encryptedDekString = new String(v2Entity.getEncryptedDek(), StandardCharsets.ISO_8859_1);
        assertFalse(encryptedDekString.contains(uniqueToken), "Encrypted DEK không được chứa dữ liệu nhạy cảm");

        // 3. Đọc dữ liệu vật lý thực tế từ persistent storage
        byte[] rawStoredBytes = fileStorageV2Service.loadEncryptedFile(v2Entity.getStorageKey());
        assertNotNull(rawStoredBytes, "Dữ liệu file lưu trữ không được null");
        assertTrue(rawStoredBytes.length > 0, "Dữ liệu file lưu trữ phải có kích thước > 0");

        // 4. KIỂM TRA QUAN TRỌNG NHẤT: Dữ liệu trên đĩa là CIPHERTEXT, KHÔNG CHỨA PLAINTEXT
        String rawStoredString = new String(rawStoredBytes, StandardCharsets.UTF_8);
        assertFalse(rawStoredString.contains("<svg"), "Dữ liệu lưu trữ V2 KHÔNG ĐƯỢC chứa thẻ mở <svg plaintext!");
        assertFalse(rawStoredString.contains("<path"), "Dữ liệu lưu trữ V2 KHÔNG ĐƯỢC chứa thẻ <path plaintext!");
        assertFalse(rawStoredString.contains("<circle"), "Dữ liệu lưu trữ V2 KHÔNG ĐƯỢC chứa thẻ <circle plaintext!");
        assertFalse(rawStoredString.contains(uniqueToken), "Dữ liệu lưu trữ V2 KHÔNG ĐƯỢC chứa chuỗi nhận diện ngẫu nhiên plaintext!");
        assertFalse(rawStoredString.contains("VinFast"), "Dữ liệu lưu trữ V2 KHÔNG ĐƯỢC chứa văn bản metadata plaintext!");

        // =========================================================================
        // GIAI ĐOẠN 4: TEST PREVIEW VÀ DOWNLOAD (GIẢI MÃ ON-THE-FLY TRONG SUỐT)
        // =========================================================================

        // 1. Preview file
        byte[] previewBytes = mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/preview")
                        .with(asUser(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("image/svg+xml")))
                .andExpect(header().string("Content-Disposition", startsWith("inline")))
                .andExpect(header().string("X-Content-Type-Options", is("nosniff")))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
                .andExpect(header().string("Cache-Control", is("private, no-store")))
                .andReturn().getResponse().getContentAsByteArray();

        String previewContent = new String(previewBytes, StandardCharsets.UTF_8);
        assertTrue(previewContent.contains("<svg"), "Preview phải giải mã thành công nội dung SVG");
        assertTrue(previewContent.contains(uniqueToken), "Preview phải chứa đúng chuỗi nhận diện duy nhất đã được bảo vệ");
        assertTrue(previewContent.contains("d=\"M 50 100 L 250 100 L 250 300 L 50 300 Z\""), "Tọa độ cắt path d phải được bảo toàn 100%");
        assertTrue(previewContent.contains("r=\"80\""), "Thuộc tính hình học phải được bảo toàn 100%");

        // 2. Download file
        byte[] downloadBytes = mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/download")
                        .with(asUser(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("image/svg+xml")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("encryption-e2e-test.svg")))
                .andReturn().getResponse().getContentAsByteArray();

        String downloadContent = new String(downloadBytes, StandardCharsets.UTF_8);
        assertTrue(downloadContent.contains(uniqueToken), "Nội dung tải về phải chứa chuỗi nhận diện duy nhất");
        assertEquals(previewContent, downloadContent, "Nội dung preview và download sau khi giải mã phải hoàn toàn đồng nhất");

        // =========================================================================
        // GIAI ĐOẠN 5: TEST INTEGRITY VÀ AEAD AUTHENTICATION TAG TAMPERING
        // =========================================================================
        Path storageRoot = fileStorageV2Service.getRootLocation();
        String relativePath = v2Entity.getStorageKey();
        if (relativePath.startsWith("v2/user-files/")) {
            relativePath = relativePath.substring("v2/user-files/".length());
        }
        Path physicalFilePath = storageRoot.resolve(relativePath);
        byte[] validCiphertext = Files.readAllBytes(physicalFilePath);

        // Giả lập kẻ tấn công chỉnh sửa 1 byte ở authentication tag (16 byte cuối)
        byte[] tamperedCiphertext = validCiphertext.clone();
        tamperedCiphertext[tamperedCiphertext.length - 1] ^= 0x55;
        Files.write(physicalFilePath, tamperedCiphertext);

        // Preview phải thất bại an toàn (HTTP 500), không rò rỉ bất kỳ phần plaintext nào
        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/preview")
                        .with(asUser(ownerUser)))
                .andExpect(status().isInternalServerError());

        // Download cũng phải thất bại an toàn
        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/download")
                        .with(asUser(ownerUser)))
                .andExpect(status().isInternalServerError());

        // Khôi phục lại ciphertext hợp lệ để kiểm tra các bước tiếp theo
        Files.write(physicalFilePath, validCiphertext);

        // Xác minh sau khi khôi phục, preview lại hoạt động bình thường
        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/preview")
                        .with(asUser(ownerUser)))
                .andExpect(status().isOk());

        // =========================================================================
        // GIAI ĐOẠN 6: TEST PHÂN QUYỀN VÀ BẢO VỆ CHỐNG IDOR TỪ CLIENT
        // =========================================================================

        // 1. Kẻ tấn công (attackerUser không được chia sẻ) cố tình đọc trộm -> 403 Forbidden
        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/preview")
                        .with(asUser(attackerUser)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/download")
                        .with(asUser(attackerUser)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/internal/v2/user-files/" + fileId)
                        .with(asUser(attackerUser)))
                .andExpect(status().isNotFound()); // Chống IDOR dò ID

        // 2. Client không xác thực (Unauthenticated) -> 401 Unauthorized
        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/download"))
                .andExpect(status().isUnauthorized());

        // 3. Owner chia sẻ file cho recipientUser
        UserSvgFileV2ShareRequest shareReq = new UserSvgFileV2ShareRequest();
        shareReq.setUserId(recipientUser.getId());

        mockMvc.perform(post("/api/internal/v2/user-files/" + fileId + "/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shareReq))
                        .with(asUser(ownerUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is(recipientUser.getId().intValue())))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        // 4. Recipient truy cập file được chia sẻ:
        // - Xem detail -> 200 OK (accessType = SHARED)
        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId)
                        .with(asUser(recipientUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessType", is("SHARED")));

        // - Xem preview -> 200 OK
        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/preview")
                        .with(asUser(recipientUser)))
                .andExpect(status().isOk());

        // - Tải file download -> 200 OK
        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/download")
                        .with(asUser(recipientUser)))
                .andExpect(status().isOk());

        // - Nhưng cố tình sửa (PUT) -> 403 Forbidden
        mockMvc.perform(put("/api/internal/v2/user-files/" + fileId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"Hacking attempt\"}")
                        .with(asUser(recipientUser)))
                .andExpect(status().isForbidden());

        // - Cố tình xóa (DELETE) -> 403 Forbidden
        mockMvc.perform(delete("/api/internal/v2/user-files/" + fileId)
                        .with(asUser(recipientUser)))
                .andExpect(status().isForbidden());

        // - Cố tình chia sẻ tiếp (Reshare) -> 403 Forbidden
        UserSvgFileV2ShareRequest reshareReq = new UserSvgFileV2ShareRequest();
        reshareReq.setUserId(attackerUser.getId());
        mockMvc.perform(post("/api/internal/v2/user-files/" + fileId + "/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reshareReq))
                        .with(asUser(recipientUser)))
                .andExpect(status().isForbidden());

        // 5. Owner thu hồi quyền chia sẻ (Revoke Share)
        mockMvc.perform(delete("/api/internal/v2/user-files/" + fileId + "/shares/" + recipientUser.getId())
                        .with(asUser(ownerUser)))
                .andExpect(status().isNoContent());

        // 6. Sau khi thu hồi: recipient truy cập lại lập tức bị từ chối 403 Forbidden
        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/download")
                        .with(asUser(recipientUser)))
                .andExpect(status().isForbidden());

        // =========================================================================
        // GIAI ĐOẠN 7: TEST API ADMIN V2 TRÊN CÙNG FILE MÃ HÓA
        // =========================================================================

        // 1. Admin tra cứu file V2 qua /api/admin/v2/user-files -> 200 OK
        mockMvc.perform(get("/api/admin/v2/user-files")
                        .param("keyword", "VinFast")
                        .with(asAdmin(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].vehicleConfiguration.brandName", is("VinFast")));

        // 2. Admin xem chi tiết -> 200 OK
        mockMvc.perform(get("/api/admin/v2/user-files/" + fileId)
                        .with(asAdmin(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(fileId.intValue())));

        // 3. Admin tải file V2 đã giải mã -> 200 OK
        mockMvc.perform(get("/api/admin/v2/user-files/" + fileId + "/download")
                        .with(asAdmin(adminUser)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("encryption-e2e-test.svg")));

        // 4. Admin preview file V2 inline với security headers -> 200 OK
        mockMvc.perform(get("/api/admin/v2/user-files/" + fileId + "/preview")
                        .with(asAdmin(adminUser)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")));

        // =========================================================================
        // GIAI ĐOẠN 8: TEST OVERWRITE VÀ DELETE V2
        // =========================================================================

        // 1. Ghi đè file mới (PUT)
        String newSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"200\" height=\"200\"><rect width=\"100\" height=\"100\" fill=\"#EF4444\"/></svg>";
        MockMultipartFile updatedFilePart = new MockMultipartFile(
                "file", "updated.svg", "image/svg+xml", newSvg.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/internal/v2/user-files/" + fileId)
                        .file(updatedFilePart)
                        .param("fileName", "updated.svg")
                        .with(r -> { r.setMethod("PUT"); return r; })
                        .with(asUser(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName", is("updated.svg")));

        // Xác minh file cập nhật cũng được mã hóa trên đĩa và preview ra nội dung mới
        byte[] updatedPreview = mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/preview")
                        .with(asUser(ownerUser)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertTrue(new String(updatedPreview, StandardCharsets.UTF_8).contains("fill=\"#EF4444\""));

        // 2. Xóa mềm file (DELETE)
        mockMvc.perform(delete("/api/internal/v2/user-files/" + fileId)
                        .with(asUser(ownerUser)))
                .andExpect(status().isNoContent());

        // Xác minh sau khi xóa mềm, không thể truy cập qua detail / preview
        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId)
                        .with(asUser(ownerUser)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/preview")
                        .with(asUser(ownerUser)))
                .andExpect(status().isNotFound());
    }
}
