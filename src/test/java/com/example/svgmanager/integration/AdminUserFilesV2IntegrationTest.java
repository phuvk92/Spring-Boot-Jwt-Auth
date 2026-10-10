package com.example.svgmanager.integration;

import com.example.svgmanager.dto.internal.v2.UserSvgFileV2ShareRequest;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.v2.UserSvgFileV2;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.repository.internal.v2.UserSvgFileV2Repository;
import com.example.svgmanager.service.internal.v2.FileStorageV2Service;
import com.example.svgmanager.service.internal.v2.InternalUserFileV2Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class AdminUserFilesV2IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSvgFileV2Repository v2FileRepository;

    @Autowired
    private InternalUserFileV2Service userFileV2Service;

    @Autowired
    private FileStorageV2Service fileStorageV2Service;

    private User adminUser;
    private User regularUser;
    private User targetUser;
    private Long sampleV2FileId;

    private static final String SAMPLE_SVG =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"500\" height=\"400\"><rect x=\"10\" y=\"10\" width=\"100\" height=\"50\" fill=\"#10B981\"/></svg>";

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asAdmin() {
        return jwt().jwt(j -> j.subject(adminUser.getKeycloakUserId()).claim("preferred_username", adminUser.getUsername()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUser(User u) {
        return jwt().jwt(j -> j.subject(u.getKeycloakUserId()).claim("preferred_username", u.getUsername()))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @BeforeEach
    void setUp() {
        adminUser = userRepository.save(User.builder()
                .username("v2_admin_" + UUID.randomUUID())
                .email("v2_admin_" + UUID.randomUUID() + "@example.com")
                .keycloakUserId("kc-v2-admin-" + UUID.randomUUID())
                .role(Role.ADMIN)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        regularUser = userRepository.save(User.builder()
                .username("v2_reg_user_" + UUID.randomUUID())
                .email("v2_reg_" + UUID.randomUUID() + "@example.com")
                .keycloakUserId("kc-v2-reg-" + UUID.randomUUID())
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        targetUser = userRepository.save(User.builder()
                .username("v2_tgt_user_" + UUID.randomUUID())
                .email("v2_tgt_" + UUID.randomUUID() + "@example.com")
                .keycloakUserId("kc-v2-tgt-" + UUID.randomUUID())
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        // Tạo sẵn một file V2 do regularUser upload
        var savedResp = userFileV2Service.saveUserFile(
                regularUser,
                SAMPLE_SVG.getBytes(StandardCharsets.UTF_8),
                "admin_test_sample.svg",
                "image/svg+xml",
                "Admin Test Sample.svg",
                null,
                null,
                "Audi",
                "A6",
                2022,
                2025,
                "C8",
                "PPF_BODY",
                "PPF Thân xe",
                1520.0,
                "MM",
                3000.0,
                "MM",
                3000.0,
                1520.0,
                null,
                "File mẫu kiểm thử Admin V2"
        );
        sampleV2FileId = savedResp.getId();
    }

    @Test
    @DisplayName("Admin V2 List: ADMIN xem được danh sách file V2 của mọi user có phân trang và bộ lọc")
    void testAdminListV2Files() throws Exception {
        mockMvc.perform(get("/api/admin/v2/user-files")
                        .param("keyword", "Audi")
                        .param("page", "0")
                        .param("size", "10")
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].fileName", is("Admin Test Sample.svg")))
                .andExpect(jsonPath("$.content[0].vehicleConfiguration.brandName", is("Audi")));
    }

    @Test
    @DisplayName("Admin V2 Detail: ADMIN xem chi tiết file V2 của user")
    void testAdminGetDetail() throws Exception {
        mockMvc.perform(get("/api/admin/v2/user-files/" + sampleV2FileId)
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(sampleV2FileId.intValue())))
                .andExpect(jsonPath("$.fileName", is("Admin Test Sample.svg")))
                .andExpect(jsonPath("$.createdBy.username", is(regularUser.getUsername())));
    }

    @Test
    @DisplayName("Admin V2 Preview: ADMIN xem trước file V2 đã giải mã an toàn on-the-fly kèm CSP và nosniff")
    void testAdminPreview() throws Exception {
        byte[] previewBytes = mockMvc.perform(get("/api/admin/v2/user-files/" + sampleV2FileId + "/preview")
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("image/svg+xml")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andReturn().getResponse().getContentAsByteArray();

        String content = new String(previewBytes, StandardCharsets.UTF_8);
        assertTrue(content.contains("fill=\"#10B981\""), "Preview phải giải mã đúng nội dung SVG gốc");
    }

    @Test
    @DisplayName("Admin V2 Download: ADMIN tải file V2 đã giải mã trong suốt")
    void testAdminDownload() throws Exception {
        byte[] downloadBytes = mockMvc.perform(get("/api/admin/v2/user-files/" + sampleV2FileId + "/download")
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("image/svg+xml")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andReturn().getResponse().getContentAsByteArray();

        String content = new String(downloadBytes, StandardCharsets.UTF_8);
        assertTrue(content.contains("fill=\"#10B981\""), "Download phải giải mã đúng nội dung SVG gốc");
    }

    @Test
    @DisplayName("Admin V2 Share & Revoke: ADMIN có quyền chia sẻ file V2 của user cho user khác và thu hồi")
    void testAdminShareAndRevoke() throws Exception {
        UserSvgFileV2ShareRequest req = new UserSvgFileV2ShareRequest();
        req.setUserId(targetUser.getId());

        // 1. Admin share
        mockMvc.perform(post("/api/admin/v2/user-files/" + sampleV2FileId + "/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(asAdmin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is(targetUser.getId().intValue())))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        // 2. Admin xem danh sách share
        mockMvc.perform(get("/api/admin/v2/user-files/" + sampleV2FileId + "/shares")
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shares", hasSize(1)))
                .andExpect(jsonPath("$.shares[0].userId", is(targetUser.getId().intValue())));

        // 3. Admin thu hồi share
        mockMvc.perform(delete("/api/admin/v2/user-files/" + sampleV2FileId + "/shares/" + targetUser.getId())
                        .with(asAdmin()))
                .andExpect(status().isNoContent());

        // 4. TargetUser bị thu hồi không thể tải file
        mockMvc.perform(get("/api/internal/v2/user-files/" + sampleV2FileId + "/download")
                        .with(asUser(targetUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin V2 Delete: ADMIN xoá mềm file V2")
    void testAdminDelete() throws Exception {
        mockMvc.perform(delete("/api/admin/v2/user-files/" + sampleV2FileId)
                        .with(asAdmin()))
                .andExpect(status().isNoContent());

        UserSvgFileV2 entity = v2FileRepository.findById(sampleV2FileId).orElseThrow();
        assertEquals("DELETED", entity.getStatus());
    }

    @Test
    @DisplayName("Authorization Check: USER thường gọi /api/admin/v2/user-files bị chặn 403 Forbidden")
    void testNonAdminForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/v2/user-files")
                        .with(asUser(regularUser)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/v2/user-files/" + sampleV2FileId)
                        .with(asUser(regularUser)))
                .andExpect(status().isForbidden());
    }
}
