package com.example.svgmanager.integration;

import com.example.svgmanager.dto.request.ShareFileRequest;
import com.example.svgmanager.entity.*;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.repository.DealerRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.repository.UserSvgFileRepository;
import com.example.svgmanager.repository.UserSvgFileShareRepository;
import com.example.svgmanager.service.FileStorageService;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserSvgFileShareIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private UserSvgFileRepository userSvgFileRepository;

    @Autowired
    private UserSvgFileShareRepository userSvgFileShareRepository;

    @Autowired
    private FileStorageService fileStorageService;

    private User adminUser;
    private User userA;
    private User userB;
    private User userC;
    private User agentUser;
    private Dealer dealer1;
    private Dealer dealer2;
    private UserSvgFile fileA;

    private static final String SVG_CONTENT = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"200\" height=\"200\"><rect width=\"200\" height=\"200\" fill=\"blue\"/></svg>";

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asAdmin() {
        return jwt().jwt(j -> j.subject("kc-admin-share").claim("preferred_username", "admin_share"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUserA() {
        return jwt().jwt(j -> j.subject("kc-userA-share").claim("preferred_username", "userA_share"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUserB() {
        return jwt().jwt(j -> j.subject("kc-userB-share").claim("preferred_username", "userB_share"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUserC() {
        return jwt().jwt(j -> j.subject("kc-userC-share").claim("preferred_username", "userC_share"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asAgent() {
        return jwt().jwt(j -> j.subject("kc-agent-share").claim("preferred_username", "agent_share"))
                .authorities(new SimpleGrantedAuthority("ROLE_AGENT"));
    }

    @BeforeEach
    void setUp() {
        userSvgFileShareRepository.deleteAll();
        userSvgFileRepository.deleteAll();

        dealer1 = dealerRepository.save(Dealer.builder().code("D1").name("Dealer Share 1").build());
        dealer2 = dealerRepository.save(Dealer.builder().code("D2").name("Dealer Share 2").build());

        adminUser = userRepository.save(User.builder()
                .username("admin_share")
                .email("admin_share@test.com")
                .keycloakUserId("kc-admin-share")
                .role(Role.ADMIN)
                .enabled(true)
                .fullName("Admin Share")
                .build());

        userA = userRepository.save(User.builder()
                .username("userA_share")
                .email("userA_share@test.com")
                .keycloakUserId("kc-userA-share")
                .role(Role.USER)
                .dealer(dealer1)
                .enabled(true)
                .fullName("Nguyen Van A")
                .build());

        userB = userRepository.save(User.builder()
                .username("userB_share")
                .email("userB_share@test.com")
                .keycloakUserId("kc-userB-share")
                .role(Role.USER)
                .dealer(dealer2)
                .enabled(true)
                .fullName("Tran Thi B")
                .build());

        userC = userRepository.save(User.builder()
                .username("userC_share")
                .email("userC_share@test.com")
                .keycloakUserId("kc-userC-share")
                .role(Role.USER)
                .dealer(dealer1)
                .enabled(true)
                .fullName("Le Van C")
                .build());

        agentUser = userRepository.save(User.builder()
                .username("agent_share")
                .email("agent_share@test.com")
                .keycloakUserId("kc-agent-share")
                .role(Role.AGENT)
                .dealer(dealer1)
                .enabled(true)
                .fullName("Agent User")
                .build());

        String storedFilename = "test-share-" + System.currentTimeMillis() + ".svg";
        String filePath = fileStorageService.storeFile(SVG_CONTENT.getBytes(StandardCharsets.UTF_8), storedFilename);

        fileA = userSvgFileRepository.save(UserSvgFile.builder()
                .fileName("BMW_X5_FRONT.svg")
                .originalFileName("BMW_X5_FRONT.svg")
                .storedFileName(storedFilename)
                .filePath(filePath)
                .fileSize((long) SVG_CONTENT.getBytes(StandardCharsets.UTF_8).length)
                .mimeType("image/svg+xml")
                .checksum("sha256:dummy")
                .brandName("BMW")
                .modelName("X5")
                .status("ACTIVE")
                .user(userA)
                .dealer(dealer1)
                .build());
    }

    @Test
    @DisplayName("OWNER chia sẻ file cho USER B thành công")
    void testOwnerShareFileSuccess() throws Exception {
        ShareFileRequest request = new ShareFileRequest(userB.getId());

        mockMvc.perform(post("/api/internal/user-files/" + fileA.getId() + "/shares")
                        .with(asUserA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userB.getId()))
                .andExpect(jsonPath("$.username").value("userB_share"))
                .andExpect(jsonPath("$.displayName").value("Tran Thi B"))
                .andExpect(jsonPath("$.dealerId").value(dealer2.getId()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.sharedBy.userId").value(userA.getId()));

        assertTrue(userSvgFileShareRepository.existsByUserSvgFileIdAndSharedToUserIdAndStatus(fileA.getId(), userB.getId(), "ACTIVE"));
    }

    @Test
    @DisplayName("USER khác (không phải owner) cố tình chia sẻ file của User A -> 403 Forbidden")
    void testUnrelatedUserShareForbidden() throws Exception {
        ShareFileRequest request = new ShareFileRequest(userB.getId());

        mockMvc.perform(post("/api/internal/user-files/" + fileA.getId() + "/shares")
                        .with(asUserC())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.USER_FILE_SHARE_FORBIDDEN));
    }

    @Test
    @DisplayName("ADMIN chia sẻ file của User A cho User B -> 201 Created")
    void testAdminShareSuccess() throws Exception {
        ShareFileRequest request = new ShareFileRequest(userB.getId());

        mockMvc.perform(post("/api/admin/user-files/" + fileA.getId() + "/shares")
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userB.getId()))
                .andExpect(jsonPath("$.sharedBy.userId").value(adminUser.getId()));

        assertTrue(userSvgFileShareRepository.existsByUserSvgFileIdAndSharedToUserIdAndStatus(fileA.getId(), userB.getId(), "ACTIVE"));
    }

    @Test
    @DisplayName("AGENT cố tình chia sẻ file -> 403 Forbidden")
    void testAgentShareForbidden() throws Exception {
        ShareFileRequest request = new ShareFileRequest(userB.getId());

        mockMvc.perform(post("/api/internal/user-files/" + fileA.getId() + "/shares")
                        .with(asAgent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.USER_FILE_SHARE_FORBIDDEN));
    }

    @Test
    @DisplayName("Chia sẻ lặp lại (Duplicate) -> Idempotent thành công và không sinh record trùng")
    void testDuplicateShareIdempotent() throws Exception {
        ShareFileRequest request = new ShareFileRequest(userB.getId());

        mockMvc.perform(post("/api/internal/user-files/" + fileA.getId() + "/shares")
                        .with(asUserA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/internal/user-files/" + fileA.getId() + "/shares")
                        .with(asUserA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.userId").value(userB.getId()));

        assertEquals(1, userSvgFileShareRepository.findByUserSvgFileIdAndStatus(fileA.getId(), "ACTIVE").size());
    }

    @Test
    @DisplayName("Tự chia sẻ cho chính mình -> 400 Bad Request, CANNOT_SHARE_TO_SELF")
    void testCannotShareToSelf() throws Exception {
        ShareFileRequest request = new ShareFileRequest(userA.getId());

        mockMvc.perform(post("/api/internal/user-files/" + fileA.getId() + "/shares")
                        .with(asUserA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.CANNOT_SHARE_TO_SELF));
    }

    @Test
    @DisplayName("Chia sẻ cho ADMIN hoặc AGENT -> 400 Bad Request, INVALID_SHARE_TARGET")
    void testCannotShareToAdminOrAgent() throws Exception {
        ShareFileRequest request = new ShareFileRequest(agentUser.getId());

        mockMvc.perform(post("/api/internal/user-files/" + fileA.getId() + "/shares")
                        .with(asUserA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.INVALID_SHARE_TARGET));
    }

    @Test
    @DisplayName("Kiểm tra quyền truy cập: Owner và Shared User xem/tải được, người không liên quan bị 403")
    void testAccessControlOwnerAndShared() throws Exception {
        // Trước khi share: User B tải -> 403
        mockMvc.perform(get("/api/internal/user-files/" + fileA.getId() + "/download")
                        .with(asUserB()))
                .andExpect(status().isForbidden());

        // Share cho User B
        userSvgFileShareRepository.save(UserSvgFileShare.builder()
                .userSvgFile(fileA)
                .sharedToUser(userB)
                .sharedByUser(userA)
                .status("ACTIVE")
                .build());

        // Sau khi share: User B xem detail -> 200, accessType = SHARED
        mockMvc.perform(get("/api/internal/user-files/" + fileA.getId())
                        .with(asUserB()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fileA.getId()))
                .andExpect(jsonPath("$.accessType").value("SHARED"));

        // User B tải file -> 200 OK
        mockMvc.perform(get("/api/internal/user-files/" + fileA.getId() + "/download")
                        .with(asUserB()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/svg+xml"));

        // User A (Owner) xem detail -> accessType = OWNER
        mockMvc.perform(get("/api/internal/user-files/" + fileA.getId())
                        .with(asUserA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessType").value("OWNER"));

        // User C (không liên quan) xem detail -> 403
        mockMvc.perform(get("/api/internal/user-files/" + fileA.getId())
                        .with(asUserC()))
                .andExpect(status().isForbidden());

        // User C tải -> 403
        mockMvc.perform(get("/api/internal/user-files/" + fileA.getId() + "/download")
                        .with(asUserC()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Owner thu hồi quyền chia sẻ: sau khi thu hồi, User B không thể tải file (403)")
    void testOwnerRevokeShare() throws Exception {
        userSvgFileShareRepository.save(UserSvgFileShare.builder()
                .userSvgFile(fileA)
                .sharedToUser(userB)
                .sharedByUser(userA)
                .status("ACTIVE")
                .build());

        // Owner thu hồi quyền của B
        mockMvc.perform(delete("/api/internal/user-files/" + fileA.getId() + "/shares/" + userB.getId())
                        .with(asUserA()))
                .andExpect(status().isNoContent());

        // User B tải -> 403 Forbidden
        mockMvc.perform(get("/api/internal/user-files/" + fileA.getId() + "/download")
                        .with(asUserB()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN thu hồi quyền chia sẻ thành công")
    void testAdminRevokeShare() throws Exception {
        userSvgFileShareRepository.save(UserSvgFileShare.builder()
                .userSvgFile(fileA)
                .sharedToUser(userB)
                .sharedByUser(userA)
                .status("ACTIVE")
                .build());

        // Admin thu hồi quyền của B
        mockMvc.perform(delete("/api/admin/user-files/" + fileA.getId() + "/shares/" + userB.getId())
                        .with(asAdmin()))
                .andExpect(status().isNoContent());

        // User B tải -> 403 Forbidden
        mockMvc.perform(get("/api/internal/user-files/" + fileA.getId() + "/download")
                        .with(asUserB()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User được share không có quyền sửa hoặc xoá file (403)")
    void testSharedUserCannotEditOrDelete() throws Exception {
        userSvgFileShareRepository.save(UserSvgFileShare.builder()
                .userSvgFile(fileA)
                .sharedToUser(userB)
                .sharedByUser(userA)
                .status("ACTIVE")
                .build());

        // User B cố tình xoá -> 403
        mockMvc.perform(delete("/api/internal/user-files/" + fileA.getId())
                        .with(asUserB()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.USER_FILE_SHARE_FORBIDDEN));
    }

    @Test
    @DisplayName("GET /api/internal/user-files trả về cả file sở hữu (OWNER) và file được share (SHARED)")
    void testListUserFilesIncludesOwnedAndShared() throws Exception {
        // Tạo file riêng của B
        String storedB = "test-b-" + System.currentTimeMillis() + ".svg";
        fileStorageService.storeFile(SVG_CONTENT.getBytes(StandardCharsets.UTF_8), storedB);
        UserSvgFile fileB = userSvgFileRepository.save(UserSvgFile.builder()
                .fileName("MERCEDES_C300.svg")
                .originalFileName("MERCEDES_C300.svg")
                .storedFileName(storedB)
                .filePath(storedB)
                .fileSize(100L)
                .mimeType("image/svg+xml")
                .checksum("sha256:dummy2")
                .status("ACTIVE")
                .user(userB)
                .build());

        // Share fileA cho User B
        userSvgFileShareRepository.save(UserSvgFileShare.builder()
                .userSvgFile(fileA)
                .sharedToUser(userB)
                .sharedByUser(userA)
                .status("ACTIVE")
                .build());

        // User B lấy danh sách file -> có 2 files: MERCEDES_C300 (OWNER) và BMW_X5_FRONT (SHARED)
        mockMvc.perform(get("/api/internal/user-files")
                        .with(asUserB()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[?(@.fileName == 'MERCEDES_C300.svg')].accessType", contains("OWNER")))
                .andExpect(jsonPath("$.content[?(@.fileName == 'BMW_X5_FRONT.svg')].accessType", contains("SHARED")));
    }

    @Test
    @DisplayName("Lấy danh sách người được share: Owner và Admin xem được, User lạ 403")
    void testListShares() throws Exception {
        userSvgFileShareRepository.save(UserSvgFileShare.builder()
                .userSvgFile(fileA)
                .sharedToUser(userB)
                .sharedByUser(userA)
                .status("ACTIVE")
                .build());

        // Owner xem
        mockMvc.perform(get("/api/internal/user-files/" + fileA.getId() + "/shares")
                        .with(asUserA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").value(fileA.getId()))
                .andExpect(jsonPath("$.shares", hasSize(1)))
                .andExpect(jsonPath("$.shares[0].userId").value(userB.getId()));

        // Admin xem
        mockMvc.perform(get("/api/admin/user-files/" + fileA.getId() + "/shares")
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").value(fileA.getId()))
                .andExpect(jsonPath("$.shares", hasSize(1)));

        // User C (không liên quan) xem -> 403
        mockMvc.perform(get("/api/internal/user-files/" + fileA.getId() + "/shares")
                        .with(asUserC()))
                .andExpect(status().isForbidden());
    }
}
