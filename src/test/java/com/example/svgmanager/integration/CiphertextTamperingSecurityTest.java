package com.example.svgmanager.integration;

import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.v2.UserSvgFileV2;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.repository.internal.v2.UserSvgFileV2Repository;
import com.example.svgmanager.service.internal.v2.FileStorageV2Service;
import com.example.svgmanager.service.internal.v2.InternalUserFileV2Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CiphertextTamperingSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSvgFileV2Repository v2FileRepository;

    @Autowired
    private InternalUserFileV2Service userFileV2Service;

    @Autowired
    private FileStorageV2Service fileStorageV2Service;

    private User ownerUser;
    private Long fileId;

    private static final String ORIGINAL_SVG =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"200\" height=\"100\"><path d=\"M10 10 H 90 V 90 H 10 Z\" fill=\"#2563EB\"/></svg>";

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUser(User u) {
        return jwt().jwt(j -> j.subject(u.getKeycloakUserId()).claim("preferred_username", u.getUsername()))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @BeforeEach
    void setUp() {
        ownerUser = userRepository.save(User.builder()
                .username("tamper_owner_" + UUID.randomUUID())
                .email("tamper_owner_" + UUID.randomUUID() + "@example.com")
                .keycloakUserId("kc-tamper-" + UUID.randomUUID())
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        var resp = userFileV2Service.saveUserFile(
                ownerUser,
                ORIGINAL_SVG.getBytes(StandardCharsets.UTF_8),
                "tamper_test.svg",
                "image/svg+xml",
                "Tamper Test.svg",
                null, null, "Tesla", "Model 3",
                2023, 2024, "Highland",
                "PPF_BODY", "PPF Thân xe",
                1520.0, "MM", 2000.0, "MM",
                2000.0, 1520.0, null, "Test toàn vẹn AEAD"
        );
        fileId = resp.getId();
    }

    @Test
    @DisplayName("AES-256-GCM Integrity: Ciphertext bị chỉnh sửa trên đĩa -> Xác minh Auth Tag thất bại, không trả dữ liệu")
    void testTamperedCiphertext_FailsSafely() throws Exception {
        UserSvgFileV2 entity = v2FileRepository.findById(fileId).orElseThrow();
        byte[] originalCiphertext = fileStorageV2Service.loadEncryptedFile(entity.getStorageKey());
        assertNotNull(originalCiphertext);
        assertTrue(originalCiphertext.length > 16, "Ciphertext phải chứa payload và auth tag");

        // Giả lập kẻ tấn công sửa đổi 1 byte trong ciphertext blob
        byte[] tamperedCiphertext = originalCiphertext.clone();
        tamperedCiphertext[tamperedCiphertext.length - 1] ^= 0xFF; // Đảo bit ở authentication tag

        // Ghi đè file vật lý bằng ciphertext đã bị sửa đổi
        Path storageRoot = fileStorageV2Service.getRootLocation();
        String relative = entity.getStorageKey();
        if (relative.startsWith("v2/user-files/")) {
            relative = relative.substring("v2/user-files/".length());
        }
        Path physicalFile = storageRoot.resolve(relative);
        Files.write(physicalFile, tamperedCiphertext);

        // Thử preview: Auth tag mismatch phải gây lỗi giải mã, không rò rỉ dữ liệu (HTTP 500)
        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/preview")
                        .with(asUser(ownerUser)))
                .andExpect(status().isInternalServerError());

        // Thử download: Auth tag mismatch cũng phải gây lỗi giải mã an toàn
        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/download")
                        .with(asUser(ownerUser)))
                .andExpect(status().isInternalServerError());

        // Phục hồi lại ciphertext gốc
        Files.write(physicalFile, originalCiphertext);

        // Kiểm tra sau khi phục hồi đúng ciphertext, giải mã lại thành công bình thường
        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/preview")
                        .with(asUser(ownerUser)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Envelope Encryption: Encrypted DEK trong DB bị sửa -> Giải mã DEK thất bại, không trả dữ liệu")
    void testTamperedEncryptedDek_FailsSafely() throws Exception {
        UserSvgFileV2 entity = v2FileRepository.findById(fileId).orElseThrow();
        byte[] corruptedDek = entity.getEncryptedDek().clone();
        corruptedDek[corruptedDek.length - 1] ^= 0xAA;
        entity.setEncryptedDek(corruptedDek);
        v2FileRepository.saveAndFlush(entity);

        mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/download")
                        .with(asUser(ownerUser)))
                .andExpect(status().isInternalServerError());
    }
}
