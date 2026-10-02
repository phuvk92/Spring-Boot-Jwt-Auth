package com.example.svgmanager.service;

import com.example.svgmanager.entity.FileCategory;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.SvgFilePart;
import com.example.svgmanager.repository.FileCategoryRepository;
import com.example.svgmanager.repository.SvgFilePartRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SvgPartColorBackfillRunnerTest {

    @Autowired
    private SvgPartColorBackfillRunner backfillRunner;

    @Autowired
    private SvgFileRepository svgFileRepository;

    @Autowired
    private SvgFilePartRepository svgFilePartRepository;

    @Autowired
    private FileCategoryRepository fileCategoryRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private com.example.svgmanager.repository.UserRepository userRepository;

    private com.example.svgmanager.entity.User getOrCreateUser() {
        return userRepository.findAll().stream().findFirst().orElseGet(() -> {
            com.example.svgmanager.entity.User u = com.example.svgmanager.entity.User.builder()
                    .username("test-user-" + UUID.randomUUID())
                    .email("test@pcut.vn")
                    .fullName("Test User")
                    .role(com.example.svgmanager.entity.Role.ADMIN)
                    .keycloakUserId("kc-" + UUID.randomUUID())
                    .enabled(true)
                    .deleted(false)
                    .build();
            return userRepository.save(u);
        });
    }

    @Test
    @Transactional
    @DisplayName("NGO-415: Backfill màu từ file SVG đã lưu trên đĩa cho các part color IS NULL, idempotent")
    void backfillColors_idempotent() {
        String svgContent = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100mm" height="50mm" viewBox="0 0 100 50">
                  <rect id="nap-capo" x="10" y="5" width="30" height="20" fill="#5CC6D0"/>
                  <path id="den-trai" d="M0 0 L10 0 L10 10 Z" fill="#F7ADAF"/>
                </svg>""";
        String storedName = UUID.randomUUID() + ".svg";
        String storedPath = fileStorageService.storeFile(svgContent.getBytes(StandardCharsets.UTF_8), storedName);

        FileCategory cat = fileCategoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            FileCategory c = new FileCategory();
            c.setName("Test Cat");
            c.setDisplayOrder(1);
            c.setActive(true);
            return fileCategoryRepository.save(c);
        });

        SvgFile file = new SvgFile();
        file.setDisplayName("Test Backfill File");
        file.setFileCategory(cat);
        file.setStatus("ACTIVE");
        file.setUploadedBy(getOrCreateUser());
        file.setFilePath(storedPath);
        file.setStoredFilename(storedName);
        file.setFileKey("test-backfill-" + UUID.randomUUID());
        file = svgFileRepository.save(file);

        // Tạo part chưa có màu (color = null)
        SvgFilePart p1 = new SvgFilePart(file, "nap-capo", "nap-capo", "Ngoại thất", "1,00 m", null, 1);
        p1.setLayout("NESTED");
        p1.setColor(null);
        svgFilePartRepository.save(p1);

        SvgFilePart p2 = new SvgFilePart(file, "den-trai", "den-trai", "Ngoại thất", "0,50 m", null, 2);
        p2.setLayout("NESTED");
        p2.setColor(null);
        svgFilePartRepository.save(p2);

        // Chạy backfill lần 1: cập nhật 2 part
        int updated = backfillRunner.backfillColors();
        assertTrue(updated >= 2, "Phải cập nhật ít nhất 2 part");

        SvgFilePart reloadedP1 = svgFilePartRepository.findById(p1.getId()).orElseThrow();
        SvgFilePart reloadedP2 = svgFilePartRepository.findById(p2.getId()).orElseThrow();
        assertEquals("#5CC6D0", reloadedP1.getColor());
        assertEquals("#F7ADAF", reloadedP2.getColor());

        // Chạy backfill lần 2 (idempotent): không cập nhật thêm part nào của file này
        int updatedSecondTime = backfillRunner.backfillColors();
        assertEquals(0, updatedSecondTime, "Lần thứ 2 không còn part nào color IS NULL");
    }

    @Test
    @Transactional
    @DisplayName("NGO-415: Bỏ qua im lặng và ghi log cảnh báo khi file trên đĩa không tồn tại")
    void backfillColors_missingFile_skipsWithoutCrashing() {
        FileCategory cat = fileCategoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            FileCategory c = new FileCategory();
            c.setName("Test Cat 2");
            c.setDisplayOrder(2);
            c.setActive(true);
            return fileCategoryRepository.save(c);
        });

        SvgFile file = new SvgFile();
        file.setDisplayName("Test Missing File");
        file.setFileCategory(cat);
        file.setStatus("ACTIVE");
        file.setUploadedBy(getOrCreateUser());
        file.setFilePath("2026/10/non-existent-file.svg");
        file.setStoredFilename("non-existent-file.svg");
        file.setFileKey("test-missing-" + UUID.randomUUID());
        file = svgFileRepository.save(file);

        SvgFilePart p = new SvgFilePart(file, "some-part", "some-part", "Ngoại thất", "1,00 m", null, 1);
        p.setLayout("NESTED");
        p.setColor(null);
        svgFilePartRepository.save(p);

        // Không ném exception, xử lý an toàn
        assertDoesNotThrow(() -> backfillRunner.backfillColors());
    }
}
