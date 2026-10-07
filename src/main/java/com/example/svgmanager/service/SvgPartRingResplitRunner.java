package com.example.svgmanager.service;

import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.service.impl.AdminFileServiceImpl;
import com.example.svgmanager.svg.SvgImport;
import com.example.svgmanager.svg.SvgImport.ImportedPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Migration dữ liệu cũ (idempotent) — SA-Nesting §8, board 08/10.
 *
 * Part file tải lên trước 08/10 được tách "mỗi phần tử = một part", nên lỗ khoét của file
 * CorelDRAW nằm trong kho thành part rời và SuperNesting dời chúng ra khỏi part chứa. Runner
 * này đọc lại SVG đã lưu của từng file, tách theo quy tắc ring chẵn/lẻ mới, và thay toàn bộ
 * part của layout đó khi số part lệch. Số part khớp thì bỏ qua — chạy lại bao nhiêu lần cũng
 * không đổi gì.
 *
 * Part chỉ là dữ liệu sinh từ file SVG (không bảng nào tham chiếu id hay part_key của nó), nên
 * xoá và dựng lại là an toàn. File vật lý mất / không đọc được → bỏ qua, ghi warning.
 */
@Component
public class SvgPartRingResplitRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SvgPartRingResplitRunner.class);

    private final SvgFileRepository svgFileRepository;
    private final FileStorageService fileStorageService;
    private final TransactionTemplate tx;

    public SvgPartRingResplitRunner(SvgFileRepository svgFileRepository,
                                    FileStorageService fileStorageService,
                                    PlatformTransactionManager transactionManager) {
        this.svgFileRepository = svgFileRepository;
        this.fileStorageService = fileStorageService;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        resplitAll();
    }

    /** Tách lại mọi file còn hiệu lực; trả về số layout đã thay part. */
    public int resplitAll() {
        List<Long> ids = svgFileRepository.findAll().stream()
                .filter(f -> !"DELETED".equalsIgnoreCase(f.getStatus()))
                .map(SvgFile::getId)
                .toList();

        int changed = 0;
        for (Long id : ids) {
            try {
                Integer n = tx.execute(status -> resplitFile(id));
                changed += n == null ? 0 : n;
            } catch (Exception e) {
                log.warn("Không tách lại được part của file id={}: {}", id, e.getMessage());
            }
        }
        if (changed > 0) {
            log.info("Tách lại part theo ring chẵn/lẻ (SA-Nesting §8): {} layout đã cập nhật.", changed);
        }
        return changed;
    }

    private int resplitFile(Long id) {
        SvgFile file = svgFileRepository.findById(id).orElse(null);
        if (file == null) {
            return 0;
        }
        int changed = 0;
        Integer nested = null;
        Integer raw = null;

        List<ImportedPart> nestedParts = parse(file, file.getFilePath());
        if (nestedParts != null) {
            nested = nestedParts.size();
            if (replaceIfChanged(file, "NESTED", nestedParts)) changed++;
        }
        List<ImportedPart> rawParts = parse(file, file.getRawFilePath());
        if (rawParts != null) {
            raw = rawParts.size();
            if (replaceIfChanged(file, "RAW", rawParts)) changed++;
        }

        if (changed > 0 && nested != null && raw != null && !nested.equals(raw)) {
            log.warn("File id={} ('{}') sau khi tách lại: bản đã xếp {} part, bản chưa xếp {} part — "
                    + "admin nên kiểm lại hai file SVG", id, file.getFileKey(), nested, raw);
        }
        return changed;
    }

    private List<ImportedPart> parse(SvgFile file, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = fileStorageService.loadFileAsBytes(relativePath);
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            // File trong kho đã qua bộ khử độc lúc upload — luôn là UTF-8.
            List<ImportedPart> parts = SvgImport.parseParts(new String(bytes, StandardCharsets.UTF_8));
            return parts.isEmpty() ? null : parts;
        } catch (Exception e) {
            log.warn("Không đọc được '{}' của file id={} để tách lại part: {}",
                    relativePath, file.getId(), e.getMessage());
            return null;
        }
    }

    private boolean replaceIfChanged(SvgFile file, String layout, List<ImportedPart> parts) {
        long current = file.getParts().stream().filter(p -> layout.equalsIgnoreCase(p.getLayout())).count();
        if (current == parts.size()) {
            return false;
        }
        file.getParts().removeIf(p -> layout.equalsIgnoreCase(p.getLayout()));
        svgFileRepository.flush();   // xoá trước khi chèn — unique (svg_file_id, layout, part_key)
        AdminFileServiceImpl.addParts(file, parts, layout);
        svgFileRepository.save(file);
        log.info("File id={} ('{}') layout {}: {} → {} part", file.getId(), file.getFileKey(), layout,
                current, parts.size());
        return true;
    }
}
