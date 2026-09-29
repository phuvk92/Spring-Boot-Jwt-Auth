package com.example.svgmanager.integration;

import com.example.svgmanager.entity.*;
import com.example.svgmanager.repository.*;
import com.example.svgmanager.service.FileStorageService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kho SVG web (/api/svg) sau V14 — đã gỡ cấu hình xe cũ và quyền đại lý (Q6).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SvgIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SvgFileRepository svgFileRepository;

    @MockBean
    private FileStorageService fileStorageService;

    private User adminUser;
    private User workerUser;
    private SvgFile sampleSvg;

    @BeforeEach
    void setUp() {
        when(fileStorageService.storeFile(any(), any())).thenReturn("/mock/storage/sample.svg");
        when(fileStorageService.loadFileAsResource(any())).thenReturn(
                new ByteArrayResource("<svg viewBox=\"0 0 100 100\"><circle cx=\"50\" cy=\"50\" r=\"40\"/></svg>".getBytes(StandardCharsets.UTF_8))
        );

        adminUser = userRepository.save(User.builder()
                .username("admin_svg_test")
                .keycloakUserId("kc-admin-svg")
                .email("admin_svg@example.com")
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        workerUser = userRepository.save(User.builder()
                .username("worker_svg")
                .keycloakUserId("kc-worker-svg")
                .email("worker_svg@example.com")
                .role(Role.USER)
                .enabled(true)
                .build());

        sampleSvg = svgFileRepository.save(SvgFile.builder()
                .originalFilename("bmw_x5_hood.svg")
                .storedFilename("uuid-bmw-x5.svg")
                .filePath("/mock/storage/sample.svg")
                .fileSize(1024L)
                .contentType("image/svg+xml")
                .checksum("fakechecksum123")
                .status("ACTIVE")
                .uploadedBy(adminUser)
                .build());

        entityManager.flush();
        entityManager.clear();
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asAdmin() {
        return jwt().jwt(j -> j.subject("kc-admin-svg").claim("preferred_username", "admin_svg_test"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUser() {
        return jwt().jwt(j -> j.subject("kc-worker-svg").claim("preferred_username", "worker_svg"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    @DisplayName("Admin batch upload tối đa 10 file SVG")
    void adminBatchUpload_Success() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "part1.svg", "image/svg+xml",
                "<svg><rect width=\"10\" height=\"10\"/></svg>".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile file2 = new MockMultipartFile("files", "part2.svg", "image/svg+xml",
                "<svg><circle cx=\"5\" cy=\"5\" r=\"4\"/></svg>".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/svg/batch")
                        .file(file1)
                        .file(file2)
                        .with(asAdmin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalUploaded", is(2)))
                .andExpect(jsonPath("$.files", hasSize(2)));
    }

    @Test
    @DisplayName("Batch upload rejects if files exceed 10")
    void adminBatchUpload_Exceed10_BadRequest() throws Exception {
        var builder = multipart("/api/svg/batch");
        for (int i = 0; i < 11; i++) {
            builder.file(new MockMultipartFile("files", "file" + i + ".svg", "image/svg+xml", "<svg/>".getBytes(StandardCharsets.UTF_8)));
        }

        mockMvc.perform(builder.with(asAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("tối đa 10 file")));
    }

    @Test
    @DisplayName("User cannot upload SVG files")
    void userCannotUpload_Forbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "part1.svg", "image/svg+xml", "<svg/>".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/svg/batch")
                        .file(file)
                        .with(asUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin sees all SVG files")
    void adminList_SeesAll() throws Exception {
        mockMvc.perform(get("/api/svg")
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].originalFilename", is("bmw_x5_hood.svg")));
    }

    @Test
    @DisplayName("Q6: thợ đăng nhập hợp lệ thấy file trong list (không còn quyền đại lý)")
    void userList_SeesActiveFiles() throws Exception {
        mockMvc.perform(get("/api/svg")
                        .with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].canView", is(true)))
                .andExpect(jsonPath("$.content[0].canDownload", is(true)));
    }

    // F-57 (Q1 chốt 28/09): thợ không lấy nội dung file qua đường web nữa — chỉ qua /api/internal/svg-files/*,
    // nơi phiên bị kiểm theo máy.
    @Test
    @DisplayName("USER cannot download SVG via web endpoint")
    void user_Download_ForbiddenOnWebEndpoint() throws Exception {
        mockMvc.perform(get("/api/svg/" + sampleSvg.getId() + "/download")
                        .with(asUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER cannot preview or read SVG content via web endpoint")
    void user_PreviewAndContent_ForbiddenOnWebEndpoint() throws Exception {
        mockMvc.perform(get("/api/svg/" + sampleSvg.getId() + "/preview").with(asUser()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/svg/" + sampleSvg.getId() + "/content").with(asUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin can update SVG status")
    void adminUpdateStatus_Success() throws Exception {
        mockMvc.perform(put("/api/svg/" + sampleSvg.getId())
                        .param("status", "DELETED")
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DELETED")));
    }
}
