package com.example.svgmanager.integration;

import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.SvgFilePart;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.repository.SvgFilePartRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.UserRepository;
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
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * KX-32 · F-56 — part và hình học theo fileKey. Đầu danh sách file lọc theo cây xe
 * mới thuộc NGO-325/326 (SA-DanhMucXe-v2 §3.3) — endpoint 6 cấp cũ đã gỡ cùng V15.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DesignFilesIntegrationTest {

    private static final String FILE_KEY =
            "abarth-695-695-2024-hatchback-3-cửa--full-body";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SvgFileRepository svgFileRepository;

    @Autowired
    private SvgFilePartRepository svgFilePartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    private SvgFile saveFile(String fileKey, String displayName, String filmUsage,
                             String note) {
        SvgFile file = SvgFile.builder()
                .originalFilename(fileKey + ".svg")
                .storedFilename("stored-" + fileKey + ".svg")
                .filePath("/tmp/" + fileKey + ".svg")
                .fileSize(1024L)
                .contentType("image/svg+xml")
                .status("ACTIVE")
                .uploadedBy(uploader)
                .build();
        file.setFileKey(fileKey);
        file.setDisplayName(displayName);
        file.setFilmUsage(filmUsage);
        file.setNote(note);
        return svgFileRepository.save(file);
    }

    @Test
    @DisplayName("KX-32: part trong file, đúng thứ tự, đủ 5 trường (khớp fixture 08)")
    void getFileParts_existingFile_returnsPartsInOrder() throws Exception {
        SvgFile full = saveFile(FILE_KEY, "Ngoại thất — full body 7 mảnh",
                "6,46 m", "Trọn gói ngoài xe. Bản gốc nhà sản xuất");
        svgFilePartRepository.save(new SvgFilePart(
                full, "capo", "Capo", "Ngoại thất", "1,42 m", "Bản gốc nhà sản xuất · 12 node", 0));
        svgFilePartRepository.save(new SvgFilePart(
                full, "đèn-trái", "Đèn trái", "Kính & đèn", "0,24 m", "Có lỗ khoét cảm biến", 1));
        svgFilePartRepository.save(new SvgFilePart(
                full, "đèn-phải", "Đèn phải", "Kính & đèn", "0,24 m", "Lật từ đèn trái", 2));

        mockMvc.perform(get("/api/v1/files/{id}/parts", FILE_KEY)
                        .with(jwt().jwt(j -> j.subject("kc-user"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id", is("capo")))
                .andExpect(jsonPath("$[0].name", is("Capo")))
                .andExpect(jsonPath("$[0].zone", is("Ngoại thất")))
                .andExpect(jsonPath("$[0].filmUsage", is("1,42 m")))
                .andExpect(jsonPath("$[0].note", is("Bản gốc nhà sản xuất · 12 node")))
                .andExpect(jsonPath("$[1].id", is("đèn-trái")))
                .andExpect(jsonPath("$[2].id", is("đèn-phải")));
    }

    @Test
    @DisplayName("KX-32: id không tồn tại → 404 FILE_NOT_FOUND, không trả mảng rỗng")
    void getFileParts_unknownId_returns404WithCode() throws Exception {
        mockMvc.perform(get("/api/v1/files/{id}/parts", "khong-co")
                        .with(jwt().jwt(j -> j.subject("kc-user"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("FILE_NOT_FOUND")));
    }

    @Test
    @DisplayName("File tồn tại nhưng không có part nào → 200 [] (khác với 404)")
    void getFileParts_fileWithoutParts_returnsEmptyArray() throws Exception {
        saveFile(FILE_KEY, "Ngoại thất — full body 7 mảnh", "6,46 m", "Trọn gói ngoài xe");

        mockMvc.perform(get("/api/v1/files/{id}/parts", FILE_KEY)
                        .with(jwt().jwt(j -> j.subject("kc-user"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private SvgFilePart savePartWithGeometry(SvgFile file, String partKey, String name,
                                             String pathData, double widthMm, double heightMm,
                                             double xMm, double yMm, int nodeCount, int holeCount,
                                             int displayOrder) {
        SvgFilePart part = new SvgFilePart(file, partKey, name, null, null, null, displayOrder);
        part.setPathData(pathData);
        part.setWidthMm(widthMm);
        part.setHeightMm(heightMm);
        part.setXMm(xMm);
        part.setYMm(yMm);
        part.setNodeCount(nodeCount);
        part.setHoleCount(holeCount);
        return svgFilePartRepository.save(part);
    }

    @Test
    @DisplayName("F-56: geometry cả file, đủ 9 trường PartOutline, khớp fixture 09 (dấu tiếng Việt giữ nguyên)")
    void getFileGeometry_existingFile_returnsGeometry() throws Exception {
        SvgFile full = saveFile(FILE_KEY, "Ngoại thất — full body 7 mảnh",
                "6,46 m", "Trọn gói ngoài xe");
        savePartWithGeometry(full, "capo", "Capo",
                "M 60,0 L 1040,0 C 1080,0 1100,25 1100,60 L 1100,760 C 1100,840 1040,900 950,900 L 150,900 C 60,900 0,840 0,760 L 0,60 C 0,25 20,0 60,0 Z",
                1100, 900, 30, 30, 12, 0, 0);
        savePartWithGeometry(full, "đèn-trái", "Đèn trái",
                "M 0,60 C 40,10 120,0 210,0 C 320,0 400,20 420,80 C 430,130 380,190 280,198 C 150,208 40,170 0,120 Z M 120,70 C 160,55 220,60 240,95 C 250,125 210,150 170,145 C 130,140 105,105 120,70 Z",
                420, 198, 30, 954, 14, 1, 1);

        mockMvc.perform(get("/api/v1/files/{id}/geometry", FILE_KEY)
                        .with(jwt().jwt(j -> j.subject("kc-user"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(jsonPath("$.fileId", is(FILE_KEY)))
                .andExpect(jsonPath("$.name", is("Ngoại thất — full body 7 mảnh")))
                .andExpect(jsonPath("$.parts", hasSize(2)))
                .andExpect(jsonPath("$.parts[0].partId", is(FILE_KEY + "--capo")))
                .andExpect(jsonPath("$.parts[0].name", is("Capo")))
                .andExpect(jsonPath("$.parts[0].pathData",
                        is("M 60,0 L 1040,0 C 1080,0 1100,25 1100,60 L 1100,760 C 1100,840 1040,900 950,900 L 150,900 C 60,900 0,840 0,760 L 0,60 C 0,25 20,0 60,0 Z")))
                .andExpect(jsonPath("$.parts[0].widthMm", is(1100.0)))
                .andExpect(jsonPath("$.parts[0].heightMm", is(900.0)))
                .andExpect(jsonPath("$.parts[0].xMm", is(30.0)))
                .andExpect(jsonPath("$.parts[0].yMm", is(30.0)))
                .andExpect(jsonPath("$.parts[0].nodeCount", is(12)))
                .andExpect(jsonPath("$.parts[0].holeCount", is(0)))
                .andExpect(jsonPath("$.parts[1].partId", is(FILE_KEY + "--đèn-trái")))
                .andExpect(jsonPath("$.parts[1].xMm", is(30.0)))
                .andExpect(jsonPath("$.parts[1].yMm", is(954.0)))
                .andExpect(jsonPath("$.parts[1].holeCount", is(1)));
    }

    @Test
    @DisplayName("F-56: id không tồn tại → 404 FILE_NOT_FOUND")
    void getFileGeometry_unknownId_returns404WithCode() throws Exception {
        mockMvc.perform(get("/api/v1/files/{id}/geometry", "khong-co")
                        .with(jwt().jwt(j -> j.subject("kc-user"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("FILE_NOT_FOUND")));
    }

    @Test
    @DisplayName("F-56: không có phiên còn hiệu lực → 401")
    void getFileGeometry_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/files/{id}/geometry", FILE_KEY))
                .andExpect(status().isUnauthorized());
    }
}
