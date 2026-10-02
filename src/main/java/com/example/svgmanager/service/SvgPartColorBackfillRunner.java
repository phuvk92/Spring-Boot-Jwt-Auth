package com.example.svgmanager.service;

import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.SvgFilePart;
import com.example.svgmanager.repository.SvgFilePartRepository;
import com.example.svgmanager.svg.SvgImport;
import com.example.svgmanager.svg.SvgImport.ImportedPart;
import com.example.svgmanager.util.SlugUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Migration dữ liệu cũ (idempotent) — NGO-415.
 * Bổ sung màu tô cho các part cũ có color IS NULL từ các file SVG đã lưu trên đĩa.
 * Khớp part theo layout + part_key (hoặc thứ tự tương ứng).
 * Nếu file vật lý không tồn tại trên đĩa thì bỏ qua và ghi log warning, không làm sập ứng dụng.
 */
@Component
public class SvgPartColorBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SvgPartColorBackfillRunner.class);

    private final SvgFilePartRepository svgFilePartRepository;
    private final FileStorageService fileStorageService;

    public SvgPartColorBackfillRunner(SvgFilePartRepository svgFilePartRepository,
                                      FileStorageService fileStorageService) {
        this.svgFilePartRepository = svgFilePartRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        backfillColors();
    }

    public int backfillColors() {
        if (!svgFilePartRepository.existsByColorIsNull()) {
            return 0;
        }

        List<SvgFile> files = svgFilePartRepository.findDistinctSvgFilesWithNullColor();
        if (files.isEmpty()) {
            return 0;
        }

        log.info("Bắt đầu backfill màu cho {} file thiết kế có part chưa có màu...", files.size());
        int updatedCount = 0;

        for (SvgFile file : files) {
            try {
                updatedCount += backfillFile(file);
            } catch (Exception e) {
                log.warn("Lỗi khi backfill màu cho file id={}, fileKey='{}': {}",
                        file.getId(), file.getFileKey(), e.getMessage());
            }
        }

        log.info("Hoàn thành backfill màu part: cập nhật {} part.", updatedCount);
        return updatedCount;
    }

    private int backfillFile(SvgFile file) {
        int updated = 0;

        // Xử lý NESTED layout nếu có filePath
        if (file.getFilePath() != null && !file.getFilePath().isBlank()) {
            updated += backfillLayout(file, "NESTED", file.getFilePath());
        }

        // Xử lý RAW layout nếu có rawFilePath
        if (file.getRawFilePath() != null && !file.getRawFilePath().isBlank()) {
            updated += backfillLayout(file, "RAW", file.getRawFilePath());
        }

        return updated;
    }

    private int backfillLayout(SvgFile file, String layout, String relativePath) {
        byte[] bytes;
        try {
            bytes = fileStorageService.loadFileAsBytes(relativePath);
        } catch (Exception e) {
            log.warn("Không thể đọc file '{}' của file id={} để trích xuất màu part: {}",
                    relativePath, file.getId(), e.getMessage());
            return 0;
        }

        if (bytes == null || bytes.length == 0) {
            return 0;
        }

        List<ImportedPart> importedParts;
        try {
            String xml = new String(bytes, StandardCharsets.UTF_8);
            importedParts = SvgImport.parseParts(xml);
        } catch (Exception e) {
            log.warn("Không thể parse SVG '{}' của file id={}: {}", relativePath, file.getId(), e.getMessage());
            return 0;
        }

        if (importedParts.isEmpty()) {
            return 0;
        }

        // Lấy danh sách part trong DB của file + layout này
        List<SvgFilePart> dbParts = svgFilePartRepository
                .findBySvgFileIdAndLayoutOrderByDisplayOrderAscIdAsc(file.getId(), layout);
        if (dbParts.isEmpty()) {
            return 0;
        }

        // Map importedParts theo part_key tạo theo cùng thuật toán
        Set<String> usedKeys = new HashSet<>();
        Map<String, String> colorByKey = new HashMap<>();
        for (ImportedPart ip : importedParts) {
            String key = uniquePartKey(ip.name(), usedKeys);
            if (ip.color() != null) {
                colorByKey.put(key, ip.color());
            }
        }

        int updated = 0;
        for (int i = 0; i < dbParts.size(); i++) {
            SvgFilePart dbPart = dbParts.get(i);
            if (dbPart.getColor() != null) {
                continue;
            }

            // Thử khớp theo partKey
            String color = colorByKey.get(dbPart.getPartKey());
            // Nếu không khớp theo partKey nhưng kích thước khớp theo vị trí thứ tự i
            if (color == null && i < importedParts.size()) {
                color = importedParts.get(i).color();
            }

            if (color != null) {
                dbPart.setColor(color);
                svgFilePartRepository.save(dbPart);
                updated++;
            }
        }

        return updated;
    }

    private static String uniquePartKey(String name, Set<String> used) {
        String base = SlugUtils.slugify(name);
        if (base.isEmpty()) {
            base = "part";
        }
        String key = base;
        int n = 2;
        while (!used.add(key)) {
            key = base + "-" + n++;
        }
        return key;
    }
}
