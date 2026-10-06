package com.example.svgmanager.integration;

import com.example.svgmanager.dto.internal.v2.UserSvgFileV2ShareRequest;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.repository.internal.v2.UserSvgFileV2Repository;
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
class InternalUserFilesV2SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSvgFileV2Repository v2FileRepository;

    private User testUser;

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUser(User u) {
        return jwt().jwt(j -> j.subject(u.getKeycloakUserId()).claim("preferred_username", u.getUsername()))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .username("sec_user_" + UUID.randomUUID())
                .email("sec_user_" + UUID.randomUUID() + "@example.com")
                .keycloakUserId("kc-sec-user-" + UUID.randomUUID())
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private void assertUploadRejected(String svgContent, String filename, String mimeType, int expectedStatus, String expectedErrorCode) throws Exception {
        MockMultipartFile filePart = new MockMultipartFile(
                "file", filename, mimeType, svgContent.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/internal/v2/user-files")
                        .file(filePart)
                        .param("fileName", filename)
                        .with(asUser(testUser)))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code", is(expectedErrorCode)));
    }

    @Test
    @DisplayName("1. XSS payload (<script>): Bị từ chối HTTP 400 SVG_SECURITY_VALIDATION_FAILED")
    void testReject_XssScript() throws Exception {
        String xssSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert('XSS')</script><circle cx=\"50\" cy=\"50\" r=\"40\"/></svg>";
        assertUploadRejected(xssSvg, "xss.svg", "image/svg+xml", 400, "SVG_SECURITY_VALIDATION_FAILED");
    }

    @Test
    @DisplayName("2. Event handler (onload/onclick): Bị từ chối HTTP 400 SVG_SECURITY_VALIDATION_FAILED")
    void testReject_EventHandler() throws Exception {
        String eventSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"><rect width=\"100\" height=\"100\"/></svg>";
        assertUploadRejected(eventSvg, "event.svg", "image/svg+xml", 400, "SVG_SECURITY_VALIDATION_FAILED");

        String clickSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><circle cx=\"10\" cy=\"10\" r=\"5\" onclick=\"fetch('http://evil.com')\"/></svg>";
        assertUploadRejected(clickSvg, "click.svg", "image/svg+xml", 400, "SVG_SECURITY_VALIDATION_FAILED");
    }

    @Test
    @DisplayName("3. External resource (https://): Bị từ chối HTTP 400 SVG_SECURITY_VALIDATION_FAILED")
    void testReject_ExternalResource() throws Exception {
        String extSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><image href=\"https://evil.example/a.png\"/></svg>";
        assertUploadRejected(extSvg, "external.svg", "image/svg+xml", 400, "SVG_SECURITY_VALIDATION_FAILED");
    }

    @Test
    @DisplayName("4. XXE / DOCTYPE / SYSTEM Entity: Bị từ chối HTTP 400 SVG_SECURITY_VALIDATION_FAILED")
    void testReject_XXE() throws Exception {
        String xxeSvg = "<!DOCTYPE svg [\n" +
                "  <!ENTITY xxe SYSTEM \"file:///etc/passwd\">\n" +
                "]><svg xmlns=\"http://www.w3.org/2000/svg\"><text>&xxe;</text></svg>";
        assertUploadRejected(xxeSvg, "xxe.svg", "image/svg+xml", 400, "SVG_SECURITY_VALIDATION_FAILED");
    }

    @Test
    @DisplayName("5. SSRF (http://127.0.0.1): Bị từ chối HTTP 400 SVG_SECURITY_VALIDATION_FAILED")
    void testReject_SSRF() throws Exception {
        String ssrfSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><image href=\"http://127.0.0.1:8080/admin\"/></svg>";
        assertUploadRejected(ssrfSvg, "ssrf.svg", "image/svg+xml", 400, "SVG_SECURITY_VALIDATION_FAILED");
    }

    @Test
    @DisplayName("6. ForeignObject: Bị từ chối HTTP 400 SVG_SECURITY_VALIDATION_FAILED")
    void testReject_ForeignObject() throws Exception {
        String foSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><foreignObject width=\"100\" height=\"50\"><body xmlns=\"http://www.w3.org/1999/xhtml\"><div>test</div></body></foreignObject></svg>";
        assertUploadRejected(foSvg, "foreign_object.svg", "image/svg+xml", 400, "SVG_SECURITY_VALIDATION_FAILED");
    }

    @Test
    @DisplayName("7. Malformed XML: Bị từ chối HTTP 400")
    void testReject_MalformedXml() throws Exception {
        String malformedSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><rect width=\"100\" height=\"50\">";
        assertUploadRejected(malformedSvg, "malformed.svg", "image/svg+xml", 400, "SVG_SECURITY_VALIDATION_FAILED");
    }

    @Test
    @DisplayName("8. Excessive XML depth (> 100): Bị từ chối HTTP 413 SVG_SIZE_LIMIT_EXCEEDED")
    void testReject_ExcessiveXmlDepth() throws Exception {
        StringBuilder sb = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\">");
        for (int i = 0; i < 110; i++) {
            sb.append("<g>");
        }
        sb.append("<circle cx=\"10\" cy=\"10\" r=\"5\"/>");
        for (int i = 0; i < 110; i++) {
            sb.append("</g>");
        }
        sb.append("</svg>");
        assertUploadRejected(sb.toString(), "deep.svg", "image/svg+xml", 413, "SVG_SIZE_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("9. Binary / Executable giả dạng SVG: Bị từ chối HTTP 400 SVG_MALWARE_DETECTED")
    void testReject_BinaryExecutable() throws Exception {
        // MZ (Windows PE header)
        byte[] exeBytes = new byte[]{0x4D, 0x5A, 0x00, 0x00, '<', 's', 'v', 'g', '>'};
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "fake.svg", "image/svg+xml", exeBytes
        );

        mockMvc.perform(multipart("/api/internal/v2/user-files")
                        .file(filePart)
                        .param("fileName", "fake.svg")
                        .with(asUser(testUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("SVG_MALWARE_DETECTED")));
    }

    @Test
    @DisplayName("10. Zip archive giả dạng SVG: Bị từ chối HTTP 400 SVG_MALWARE_DETECTED")
    void testReject_ZipArchive() throws Exception {
        // PK\x03\x04 (ZIP header)
        byte[] zipBytes = new byte[]{'P', 'K', 3, 4, 0, 0, 0, 0};
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "archive.svg", "image/svg+xml", zipBytes
        );

        mockMvc.perform(multipart("/api/internal/v2/user-files")
                        .file(filePart)
                        .param("fileName", "archive.svg")
                        .with(asUser(testUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("SVG_MALWARE_DETECTED")));
    }

    @Test
    @DisplayName("11. Phần mở rộng không phải .svg: Bị từ chối HTTP 400 UNSUPPORTED_FORMAT")
    void testReject_InvalidExtension() throws Exception {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><circle cx=\"10\" cy=\"10\" r=\"5\"/></svg>";
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "payload.exe", "image/svg+xml", svg.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/internal/v2/user-files")
                        .file(filePart)
                        .param("fileName", "payload.exe")
                        .with(asUser(testUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("UNSUPPORTED_FORMAT")));
    }

    @Test
    @DisplayName("12. MIME type không hợp lệ: Bị từ chối HTTP 400 UNSUPPORTED_FORMAT")
    void testReject_InvalidMimeType() throws Exception {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><circle cx=\"10\" cy=\"10\" r=\"5\"/></svg>";
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "valid.svg", "application/x-msdownload", svg.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/internal/v2/user-files")
                        .file(filePart)
                        .param("fileName", "valid.svg")
                        .with(asUser(testUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("UNSUPPORTED_FORMAT")));
    }

    @Test
    @DisplayName("13. Valid cutting SVG: Bảo toàn 100% geometry (path d, viewBox, coordinates, transform, data-cut)")
    void testAccept_ValidCuttingSvg_PreservesGeometry() throws Exception {
        String cuttingSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1520 3000\" width=\"1520mm\" height=\"3000mm\">" +
                "<g transform=\"matrix(1 0 0 1 100 200)\">" +
                "<path d=\"M10,20 L30,40 C50,60 70,80 90,100 Z\" fill=\"none\" stroke=\"#FF0000\" stroke-width=\"1.5\" data-cut=\"outer-contour\" data-part-id=\"P-101\"/>" +
                "<rect x=\"150\" y=\"250\" width=\"400\" height=\"200\" rx=\"10\" ry=\"10\" fill=\"blue\"/>" +
                "</g></svg>";

        MockMultipartFile filePart = new MockMultipartFile(
                "file", "car_hood_cut.svg", "image/svg+xml", cuttingSvg.getBytes(StandardCharsets.UTF_8)
        );

        String res = mockMvc.perform(multipart("/api/internal/v2/user-files")
                        .file(filePart)
                        .param("fileName", "car_hood_cut.svg")
                        .with(asUser(testUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();

        Number fileId = objectMapper.readTree(res).get("id").numberValue();

        // Kiểm tra preview và headers bảo mật (CSP, nosniff, private cache)
        byte[] previewBytes = mockMvc.perform(get("/api/internal/v2/user-files/" + fileId + "/preview")
                        .with(asUser(testUser)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("image/svg+xml")))
                .andExpect(header().string("X-Content-Type-Options", is("nosniff")))
                .andExpect(header().string("Content-Security-Policy", is("default-src 'none'; img-src 'self' data:;")))
                .andExpect(header().string("Cache-Control", is("private, no-store")))
                .andReturn().getResponse().getContentAsByteArray();

        String previewXml = new String(previewBytes, StandardCharsets.UTF_8);

        // Assert 100% geometry và attributes cắt được bảo toàn
        assertTrue(previewXml.contains("viewBox=\"0 0 1520 3000\""));
        assertTrue(previewXml.contains("M10,20 L30,40 C50,60 70,80 90,100 Z"));
        assertTrue(previewXml.contains("transform=\"matrix(1 0 0 1 100 200)\""));
        assertTrue(previewXml.contains("data-cut=\"outer-contour\""));
        assertTrue(previewXml.contains("data-part-id=\"P-101\""));
        assertTrue(previewXml.contains("stroke=\"#FF0000\""));
    }
}
