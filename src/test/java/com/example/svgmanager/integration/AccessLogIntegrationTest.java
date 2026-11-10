package com.example.svgmanager.integration;

import com.example.svgmanager.entity.AuditLog;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.UserDevice;
import com.example.svgmanager.repository.AuditLogRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.UserDeviceRepository;
import com.example.svgmanager.repository.UserRepository;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-54 · CL-34 — POST /api/v1/access-log: mỗi lượt mở một file trong kho thiết kế
 * (template_download) thành đúng MỘT dòng audit OPEN_FILE có tên file, máy, IP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccessLogIntegrationTest {

    private static final String FILE_KEY = "audi-a1-2016--test0001";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private UserDeviceRepository userDeviceRepository;
    @Autowired private SvgFileRepository svgFileRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    private SvgFile file;
    /** audit_logs ghi bằng REQUIRES_NEW nên không rollback theo ca test — chỉ xét dòng sau mốc này. */
    private long baselineId;

    @BeforeEach
    void setUp() {
        baselineId = auditLogRepository.findAll().stream().mapToLong(AuditLog::getId).max().orElse(0L);

        User user = userRepository.save(User.builder().username("open_user_a").email("open_a@t.vn")
                .keycloakUserId("kc-open-a").role(Role.USER).enabled(true).build());
        userDeviceRepository.save(UserDevice.register(user, "dev-open-a", "Máy xưởng 1", "windows",
                "10.0.0.9", "sid-open-A", LocalDateTime.now()));

        SvgFile f = SvgFile.builder().contentType("image/svg+xml").status("ACTIVE").uploadedBy(user).build();
        f.setFileKey(FILE_KEY);
        f.setDisplayName("Audi A1 2016 — full body");
        f.setOriginalFilename("da-xep.svg");
        f.setStoredFilename("da-xep.svg");
        f.setFilePath("test/da-xep.svg");
        file = svgFileRepository.save(f);
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asWorker() {
        return jwt().jwt(j -> j.subject("kc-open-a")
                        .claim("preferred_username", "open_user_a")
                        .claim("sid", "sid-open-A"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private void postEvent(String json) throws Exception {
        mockMvc.perform(post("/api/v1/access-log").with(asWorker())
                        .header("X-Forwarded-For", "171.224.23.255, 10.0.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isAccepted());
    }

    private List<AuditLog> rowsOf(String actor) {
        return auditLogRepository.findAll().stream()
                .filter(a -> a.getId() > baselineId && actor.equals(a.getActor()))
                .toList();
    }

    @Test
    @DisplayName("Không có token → 401")
    void noToken_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/access-log").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"template_download\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Mở file trong kho → đúng một dòng OPEN_FILE gắn id file, tên file, máy, IP")
    void templateDownload_WritesOneOpenFileRow() throws Exception {
        postEvent("""
                {"kind":"template_download","at":"2026-11-10T21:53:29+07:00",
                 "detail":"%s · Audi A1 2016","downloadsInSession":3}
                """.formatted(FILE_KEY));

        List<AuditLog> rows = rowsOf("open_user_a");
        // Interceptor không ghi thêm dòng "POST /api/v1/access-log" — mỗi lượt mở chỉ một dòng.
        assertThat(rows).hasSize(1);
        AuditLog a = rows.get(0);
        assertThat(a.getAction()).isEqualTo("OPEN_FILE");
        assertThat(a.getActorRole()).isEqualTo("USER");
        assertThat(a.getEntity()).isEqualTo("SvgFile");
        assertThat(a.getEntityId()).isEqualTo(file.getId());
        assertThat(a.getDetails())
                .contains("File: Audi A1 2016 — full body [" + FILE_KEY + "]")
                .contains("Lượt mở trong phiên: 3")
                .contains("Máy: Máy xưởng 1 [dev-open-a]")
                .contains("IP: 171.224.23.255")
                .contains("Lúc (client): 2026-11-10T21:53:29+07:00");
    }

    @Test
    @DisplayName("fileKey không có trong kho → vẫn ghi OPEN_FILE, entityId null, nói rõ không có trong kho")
    void unknownFileKey_StillRecorded() throws Exception {
        postEvent("""
                {"kind":"template_download","detail":"khong-ton-tai · Tên client gửi","downloadsInSession":1}
                """);

        AuditLog a = rowsOf("open_user_a").get(0);
        assertThat(a.getAction()).isEqualTo("OPEN_FILE");
        assertThat(a.getEntityId()).isNull();
        assertThat(a.getDetails()).contains("File: Tên client gửi [khong-ton-tai] (không có trong kho)");
    }

    @Test
    @DisplayName("session_start và loại lạ ghi nguyên văn, không từ chối (contract 0.4.0)")
    void otherKinds_RecordedVerbatim() throws Exception {
        postEvent("{\"kind\":\"session_start\",\"downloadsInSession\":0}");

        AuditLog a = rowsOf("open_user_a").get(0);
        assertThat(a.getAction()).isEqualTo("SESSION_START");
        assertThat(a.getEntity()).isEqualTo("ClientSession");
    }

    @Test
    @DisplayName("Chi tiết có xuống dòng → gộp một dòng, không giả được dòng log mới")
    void newlinesInDetail_AreFlattened() throws Exception {
        // fileKey lạ ⇒ server dùng tên client gửi, đúng chỗ chuỗi do client kiểm soát lọt vào audit.
        postEvent("""
                {"kind":"template_download","detail":"khong-co · Tên\\nGIẢ DÒNG","downloadsInSession":2}
                """);

        String details = rowsOf("open_user_a").get(0).getDetails();
        assertThat(details).doesNotContain("\n").contains("File: Tên GIẢ DÒNG [khong-co]");
    }

    @Test
    @DisplayName("Thiếu kind → 400")
    void missingKind_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/access-log").with(asWorker())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"detail\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }
}
