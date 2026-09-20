package com.example.svgmanager.integration;

import com.example.svgmanager.dto.response.SvgResponse;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.repository.RefreshTokenRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.security.JwtTokenProvider;
import com.example.svgmanager.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SvgIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SvgFileRepository svgFileRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String agentToken;

    @BeforeEach
    void setUp() {
        svgFileRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        User admin = User.builder()
                .username("admin_test")
                .email("admin_test@example.com")
                .password(passwordEncoder.encode("AdminPass123!"))
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        admin = userRepository.save(admin);
        adminToken = jwtTokenProvider.generateTokenFromUser(UserPrincipal.create(admin));

        User agent = User.builder()
                .username("agent_test")
                .email("agent_test@example.com")
                .password(passwordEncoder.encode("AgentPass123!"))
                .role(Role.AGENT)
                .enabled(true)
                .build();
        agent = userRepository.save(agent);
        agentToken = jwtTokenProvider.generateTokenFromUser(UserPrincipal.create(agent));
    }

    @Test
    @DisplayName("Integration: Full SVG lifecycle (Upload -> Sanitize -> List -> Detail -> Preview -> Download -> Delete)")
    void fullSvgLifecycle() throws Exception {
        // 1. Upload SVG containing malicious <script>
        String svgWithXss = "<svg width=\"100\" height=\"100\" xmlns=\"http://www.w3.org/2000/svg\">" +
                "<script>alert(1)</script>" +
                "<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>" +
                "</svg>";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicious_logo.svg",
                "image/svg+xml",
                svgWithXss.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/svg/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.originalFilename").value("malicious_logo.svg"))
                .andExpect(jsonPath("$.checksum").isNotEmpty())
                .andReturn();

        SvgResponse svgResponse = objectMapper.readValue(
                uploadResult.getResponse().getContentAsString(),
                SvgResponse.class
        );
        Long svgId = svgResponse.getId();

        // 2. List SVGs
        mockMvc.perform(get("/api/svg")
                        .param("keyword", "malicious")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(svgId));

        // 3. Get SVG Detail
        mockMvc.perform(get("/api/svg/{id}", svgId)
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(svgId))
                .andExpect(jsonPath("$.originalFilename").value("malicious_logo.svg"));

        // 4. Preview SVG (Verify <script> stripped and security headers present)
        MvcResult previewResult = mockMvc.perform(get("/api/svg/{id}/preview", svgId)
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/svg+xml; charset=utf-8"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn();

        String previewContent = previewResult.getResponse().getContentAsString();
        assertThat(previewContent).doesNotContain("<script");
        assertThat(previewContent).doesNotContain("alert(1)");
        assertThat(previewContent).contains("<circle");

        // 5. Download SVG
        mockMvc.perform(get("/api/svg/{id}/download", svgId)
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"malicious_logo.svg\""));

        // 6. Delete SVG (Admin only)
        mockMvc.perform(delete("/api/svg/{id}", svgId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify SVG no longer exists
        mockMvc.perform(get("/api/svg/{id}", svgId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Integration: Uploading standard SVG with DOCTYPE should succeed and sanitize properly")
    void uploadSvg_AllowStandardDoctype() throws Exception {
        String svgWithDoctype = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n" +
                "<svg width=\"200\" height=\"200\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
                "<rect width=\"100\" height=\"100\" fill=\"green\"/>\n" +
                "</svg>";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "standard_logo.svg",
                "image/svg+xml",
                svgWithDoctype.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/svg/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.originalFilename").value("standard_logo.svg"));
    }

    @Test
    @DisplayName("Integration: Uploading SVG with XXE payload should return 400 Bad Request")
    void uploadSvg_RejectXXE() throws Exception {
        String xxePayload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE svg [ <!ENTITY xxe SYSTEM \"file:///etc/passwd\"> ]>\n" +
                "<svg width=\"100\" height=\"100\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
                "<text>&xxe;</text>\n" +
                "</svg>";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "xxe_attack.svg",
                "image/svg+xml",
                xxePayload.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/svg/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("XXE protection")));
    }
}
