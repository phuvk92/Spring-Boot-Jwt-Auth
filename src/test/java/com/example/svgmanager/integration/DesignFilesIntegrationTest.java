package com.example.svgmanager.integration;

import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Board 08/10 (SA-Nesting §8): kho part file chỉ lưu SVG, app tải SVG theo fileKey rồi tự tách
 * part — thay cho /parts và /geometry (server tách part) đã gỡ.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DesignFilesIntegrationTest {

    private static final String FILE_KEY = "abarth-695-695-2024-hatchback-3-cửa--full-body";
    private static final String NESTED_SVG =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100mm\" height=\"100mm\" viewBox=\"0 0 100 100\">"
                    + "<path id=\"Capo\" d=\"M0 0 H100 V100 H0 Z M20 20 H40 V40 H20 Z\"/></svg>";
    private static final String RAW_SVG =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"200mm\" height=\"100mm\" viewBox=\"0 0 200 100\">"
                    + "<path id=\"Capo\" d=\"M100 0 H200 V100 H100 Z\"/></svg>";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SvgFileRepository svgFileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileStorageService fileStorageService;

    private User uploader;

    @BeforeEach
    void setUp() {
        uploader = userRepository.save(User.builder()
                .username("content-team")
                .email("content@test.local")
                .fullName("Content Team")
                .keycloakUserId("kc-content-team")
                .password(passwordEncoder.encode("x"))
                .role(Role.ADMIN)
                .enabled(true)
                .deleted(false)
                .build());
    }

    private static RequestPostProcessor asUser() {
        return jwt().jwt(j -> j.subject("kc-user")).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private String store(String svg) {
        return fileStorageService.storeFile(svg.getBytes(StandardCharsets.UTF_8), UUID.randomUUID() + ".svg");
    }

    private SvgFile saveFile(String nestedSvg, String rawSvg) {
        SvgFile file = SvgFile.builder()
                .contentType("image/svg+xml")
                .status("ACTIVE")
                .uploadedBy(uploader)
                .build();
        file.setFileKey(FILE_KEY);
        file.setDisplayName("Ngoại thất — full body");
        if (nestedSvg != null) {
            file.setOriginalFilename("da-xep.svg");
            file.setStoredFilename("da-xep.svg");
            file.setFilePath(store(nestedSvg));
            file.setNestedPartCount(1);
        }
        if (rawSvg != null) {
            file.setRawOriginalFilename("chua-xep.svg");
            file.setRawStoredFilename("chua-xep.svg");
            file.setRawFilePath(store(rawSvg));
            file.setRawPartCount(1);
        }
        return svgFileRepository.save(file);
    }

    @Test
    @DisplayName("GET /svg?layout=nested|raw → đúng nội dung từng bản, image/svg+xml, không Content-Disposition")
    void getFileSvg_bothLayouts() throws Exception {
        saveFile(NESTED_SVG, RAW_SVG);

        mockMvc.perform(get("/api/v1/files/{id}/svg", FILE_KEY).param("layout", "nested").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(NESTED_SVG));

        mockMvc.perform(get("/api/v1/files/{id}/svg", FILE_KEY).param("layout", "raw").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(content().string(RAW_SVG));
    }

    @Test
    @DisplayName("Bỏ trống layout → bản đã xếp nếu có, không thì bản chưa xếp")
    void getFileSvg_defaultLayout() throws Exception {
        saveFile(null, RAW_SVG);

        mockMvc.perform(get("/api/v1/files/{id}/svg", FILE_KEY).with(asUser()))
                .andExpect(status().isOk())
                .andExpect(content().string(RAW_SVG));
    }

    @Test
    @DisplayName("Hỏi bản file không có → 404 FILE_NOT_FOUND; layout lạ → 400")
    void getFileSvg_missingLayoutOrBadParam() throws Exception {
        saveFile(NESTED_SVG, null);

        mockMvc.perform(get("/api/v1/files/{id}/svg", FILE_KEY).param("layout", "raw").with(asUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("FILE_NOT_FOUND")));
        mockMvc.perform(get("/api/v1/files/{id}/svg", FILE_KEY).param("layout", "xoay").with(asUser()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("fileKey không tồn tại → 404 FILE_NOT_FOUND; không có phiên → 401")
    void getFileSvg_unknownOrNoAuth() throws Exception {
        mockMvc.perform(get("/api/v1/files/{id}/svg", "khong-co").with(asUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("FILE_NOT_FOUND")));
        mockMvc.perform(get("/api/v1/files/{id}/svg", FILE_KEY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Danh sách file báo hasNested / hasRaw và số part lưu lúc upload")
    void listFiles_reportsLayoutsAndPartCount() throws Exception {
        saveFile(NESTED_SVG, RAW_SVG);

        mockMvc.perform(get("/api/v1/files").param("q", "full body").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", is(FILE_KEY)))
                .andExpect(jsonPath("$.content[0].hasNested", is(true)))
                .andExpect(jsonPath("$.content[0].hasRaw", is(true)))
                .andExpect(jsonPath("$.content[0].partCount", is(1)));
    }
}
