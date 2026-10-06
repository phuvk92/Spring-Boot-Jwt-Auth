package com.example.svgmanager.integration;

import com.example.svgmanager.dto.internal.v2.UserSvgFileV2ShareRequest;
import com.example.svgmanager.dto.internal.v2.UserSvgFileV2UploadRequest;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.v2.UserSvgFileV2;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.repository.UserSvgFileRepository;
import com.example.svgmanager.repository.internal.v2.UserSvgFileV2Repository;
import com.example.svgmanager.service.internal.v2.FileStorageV2Service;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InternalUserFilesV2IntegrationTest {

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
    private User thirdUser;

    private static final String SAMPLE_SVG =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"400\" height=\"300\"><circle cx=\"200\" cy=\"150\" r=\"80\" fill=\"#7C3AED\"/></svg>";

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUser(User u) {
        return jwt().jwt(j -> j.subject(u.getKeycloakUserId()).claim("preferred_username", u.getUsername()))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @BeforeEach
    void setUp() {
        ownerUser = userRepository.save(User.builder()
                .username("v2_owner_" + UUID.randomUUID())
                .email("v2_owner_" + UUID.randomUUID() + "@example.com")
                .keycloakUserId("kc-v2-owner-" + UUID.randomUUID())
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        recipientUser = userRepository.save(User.builder()
                .username("v2_recipient_" + UUID.randomUUID())
                .email("v2_recipient_" + UUID.randomUUID() + "@example.com")
                .keycloakUserId("kc-v2-recipient-" + UUID.randomUUID())
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        thirdUser = userRepository.save(User.builder()
                .username("v2_third_" + UUID.randomUUID())
                .email("v2_third_" + UUID.randomUUID() + "@example.com")
                .keycloakUserId("kc-v2-third-" + UUID.randomUUID())
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("V2 upload via Multipart: lưu vào user_svg_file_v2, file vật lý bị mã hoá AES-256-GCM (.enc), không ghi vào V1 table")
    void testUploadMultipart_EncryptedStorage_V1Isolation() throws Exception {
        long v1CountBefore = v1FileRepository.count();
        long v2CountBefore = v2FileRepository.count();

        MockMultipartFile filePart = new MockMultipartFile(
                "file", "Porsche_911_Door.svg", "image/svg+xml", SAMPLE_SVG.getBytes(StandardCharsets.UTF_8)
        );

        String responseJson = mockMvc.perform(multipart("/api/internal/v2/user-files")
                        .file(filePart)
                        .param("fileName", "Porsche_911_Door.svg")
                        .param("brandName", "Porsche")
                        .param("modelName", "911")
                        .with(asUser(ownerUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.fileName", is("Porsche_911_Door.svg")))
                .andExpect(jsonPath("$.accessType", is("OWNER")))
                // Ensure encryption metadata is NOT leaked to response
                .andExpect(jsonPath("$.encryptedDek").doesNotExist())
                .andExpect(jsonPath("$.encryptionIv").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        // 1. Verify V1 table was NOT touched
        assertEquals(v1CountBefore, v1FileRepository.count(), "V1 table user_svg_files must NOT be modified by V2 upload");

        // 2. Verify V2 table was written
        assertEquals(v2CountBefore + 1, v2FileRepository.count(), "V2 table user_svg_file_v2 must have 1 new record");

        Number id = objectMapper.readTree(responseJson).get("id").numberValue();
        UserSvgFileV2 v2Entity = v2FileRepository.findById(id.longValue()).orElseThrow();

        // 3. Verify encryption metadata in database
        assertEquals("AES-256-GCM", v2Entity.getEncryptionAlgorithm());
        assertNotNull(v2Entity.getEncryptedDek(), "Encrypted DEK must be present in DB");
        assertNotNull(v2Entity.getEncryptionIv(), "Encryption IV must be present in DB");
        assertNotNull(v2Entity.getStorageKey());
        assertTrue(v2Entity.getStorageKey().endsWith(".enc"), "Storage key must end with .enc");

        // 4. Verify storage isolation: Plaintext SVG MUST NOT exist on disk; ciphertext bytes must exist
        byte[] storedBytes = fileStorageV2Service.loadEncryptedFile(v2Entity.getStorageKey());
        assertNotNull(storedBytes);
        assertNotEquals(SAMPLE_SVG, new String(storedBytes, StandardCharsets.UTF_8), "Stored file MUST be encrypted ciphertext, NOT plaintext SVG!");
    }

    @Test
    @DisplayName("V2 upload via JSON: hỗ trợ payload JSON, mã hoá và lưu thành công")
    void testUploadJson() throws Exception {
        UserSvgFileV2UploadRequest req = new UserSvgFileV2UploadRequest();
        req.setFileName("Ferrari_F8_Hood.svg");
        req.setSvgContent(SAMPLE_SVG);
        req.setBrandName("Ferrari");
        req.setModelName("F8");

        mockMvc.perform(post("/api/internal/v2/user-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(asUser(ownerUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName", is("Ferrari_F8_Hood.svg")))
                .andExpect(jsonPath("$.accessType", is("OWNER")));
    }

    @Test
    @DisplayName("V2 preview & download: tự động giải mã trong suốt và trả về SVG gốc")
    void testPreviewAndDownload_TransparentDecryption() throws Exception {
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "Test_Preview.svg", "image/svg+xml", SAMPLE_SVG.getBytes(StandardCharsets.UTF_8)
        );

        String res = mockMvc.perform(multipart("/api/internal/v2/user-files")
                        .file(filePart)
                        .with(asUser(ownerUser)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number fileId = objectMapper.readTree(res).get("id").numberValue();

        // 1. Preview
        byte[] previewBytes = mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/preview")
                        .with(asUser(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("image/svg+xml")))
                .andReturn().getResponse().getContentAsByteArray();

        String previewStr = new String(previewBytes, StandardCharsets.UTF_8);
        assertTrue(previewStr.contains("fill=\"#7C3AED\""), "Preview must decrypt to valid SVG content containing original elements");

        // 2. Download
        byte[] downloadBytes = mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/download")
                        .with(asUser(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("image/svg+xml")))
                .andReturn().getResponse().getContentAsByteArray();

        String downloadStr = new String(downloadBytes, StandardCharsets.UTF_8);
        assertTrue(downloadStr.contains("fill=\"#7C3AED\""), "Download must decrypt to valid SVG content containing original elements");
    }

    @Test
    @DisplayName("V2 Share flow: Owner chia sẻ cho recipient -> recipient xem & tải được; recipient KHÔNG được chia sẻ tiếp; revoke share -> chặn quyền")
    void testShareFlow_PermissionEnforcement() throws Exception {
        // 1. Owner tạo file V2
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "Shared_File.svg", "image/svg+xml", SAMPLE_SVG.getBytes(StandardCharsets.UTF_8)
        );
        String res = mockMvc.perform(multipart("/api/internal/v2/user-files")
                        .file(filePart)
                        .with(asUser(ownerUser)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number fileId = objectMapper.readTree(res).get("id").numberValue();

        // 2. Recipient trước khi được share -> không thấy file và bị chặn tải (403 Forbidden)
        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/download")
                        .with(asUser(recipientUser)))
                .andExpect(status().isForbidden());

        // 3. Owner chia sẻ file cho Recipient
        UserSvgFileV2ShareRequest shareReq = new UserSvgFileV2ShareRequest(recipientUser.getId());
        mockMvc.perform(post("/api/internal/v2/user-files/" + fileId + "/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shareReq))
                        .with(asUser(ownerUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is(recipientUser.getId().intValue())))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        // 4. Recipient liệt kê file -> thấy file với accessType = SHARED
        mockMvc.perform(get("/api/internal/v2/user-files")
                        .with(asUser(recipientUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + fileId + ")].accessType", contains("SHARED")));

        // 5. Recipient tải file -> thành công giải mã
        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/download")
                        .with(asUser(recipientUser)))
                .andExpect(status().isOk());

        // 6. Recipient cố chia sẻ tiếp cho thirdUser -> bị chặn 403 Forbidden!
        UserSvgFileV2ShareRequest subShareReq = new UserSvgFileV2ShareRequest(thirdUser.getId());
        mockMvc.perform(post("/api/internal/v2/user-files/" + fileId + "/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subShareReq))
                        .with(asUser(recipientUser)))
                .andExpect(status().isForbidden());

        // 7. Recipient cố xoá file -> bị chặn 403 Forbidden!
        mockMvc.perform(delete("/api/internal/v2/user-files/" + fileId)
                        .with(asUser(recipientUser)))
                .andExpect(status().isForbidden());

        // 8. Owner thu hồi quyền chia sẻ
        mockMvc.perform(delete("/api/internal/v2/user-files/" + fileId + "/shares/" + recipientUser.getId())
                        .with(asUser(ownerUser)))
                .andExpect(status().isNoContent());

        // 9. Recipient sau khi bị thu hồi -> không tải được nữa (403 Forbidden)
        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/download")
                        .with(asUser(recipientUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Xoá mềm V2: Owner xoá file V2 đặt status=DELETED, không ảnh hưởng V1")
    void testDeleteV2File() throws Exception {
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "To_Delete.svg", "image/svg+xml", SAMPLE_SVG.getBytes(StandardCharsets.UTF_8)
        );
        String res = mockMvc.perform(multipart("/api/internal/v2/user-files")
                        .file(filePart)
                        .with(asUser(ownerUser)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number fileId = objectMapper.readTree(res).get("id").numberValue();

        mockMvc.perform(delete("/api/internal/v2/user-files/" + fileId)
                        .with(asUser(ownerUser)))
                .andExpect(status().isNoContent());

        UserSvgFileV2 deletedEntity = v2FileRepository.findById(fileId.longValue()).orElseThrow();
        assertEquals("DELETED", deletedEntity.getStatus());
    }
}
