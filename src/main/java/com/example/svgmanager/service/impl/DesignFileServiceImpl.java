package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.response.DesignFileDto;
import com.example.svgmanager.dto.response.PartDto;
import com.example.svgmanager.entity.Category;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.SvgFilePart;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.repository.CategoryRepository;
import com.example.svgmanager.repository.SvgFilePartRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.service.DesignFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DesignFileServiceImpl implements DesignFileService {

    private static final Logger log = LoggerFactory.getLogger(DesignFileServiceImpl.class);

    /** Các cấp dưới 'category', theo đúng thứ tự bộ lọc 6 cấp của hợp đồng. */
    private static final String[] LOWER_LEVELS = {"brand", "model", "variant", "year", "submodel"};

    private final CategoryRepository categoryRepository;
    private final SvgFileRepository svgFileRepository;
    private final SvgFilePartRepository svgFilePartRepository;

    public DesignFileServiceImpl(CategoryRepository categoryRepository,
                                 SvgFileRepository svgFileRepository,
                                 SvgFilePartRepository svgFilePartRepository) {
        this.categoryRepository = categoryRepository;
        this.svgFileRepository = svgFileRepository;
        this.svgFilePartRepository = svgFilePartRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DesignFileDto> getFiles(String category, String brand, String model,
                                        String variant, String year, String submodel) {
        return resolveLeaf(category, brand, model, variant, year, submodel)
                .map(leaf -> svgFileRepository
                        .findByCategoryIdAndStatusOrderByIdAsc(leaf.getId(), "ACTIVE")
                        .stream()
                        .map(file -> toDto(file, leaf))
                        .toList())
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartDto> getFileParts(String fileKey) {
        SvgFile file = svgFileRepository.findByFileKey(fileKey)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy file thiết kế.", ErrorCodes.FILE_NOT_FOUND));
        return svgFilePartRepository.findBySvgFileIdOrderByDisplayOrderAscIdAsc(file.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Dò đúng MỘT đường 6 cấp trong cây danh mục (giá trị trùng nhau giữa các nhánh là
     * bình thường, nên phải so theo parent chứ không theo level+value trần).
     */
    private Optional<Category> resolveLeaf(String category, String brand, String model,
                                           String variant, String year, String submodel) {
        Optional<Category> node =
                categoryRepository.findByLevelAndValueAndParentIsNull("category", category);
        String[] values = {brand, model, variant, year, submodel};
        for (int i = 0; i < LOWER_LEVELS.length && node.isPresent(); i++) {
            Long parentId = node.get().getId();
            node = categoryRepository.findByLevelAndValueAndParentId(LOWER_LEVELS[i], values[i], parentId);
        }
        return node;
    }

    private DesignFileDto toDto(SvgFile file, Category leaf) {
        return new DesignFileDto(
                file.getFileKey(),
                file.getDisplayName() != null ? file.getDisplayName() : file.getOriginalFilename(),
                categoryValue(leaf),
                // Đếm từ DB, không đọc collection lazy — part có thể được nạp sau file
                (int) svgFilePartRepository.countBySvgFileId(file.getId()),
                file.getFilmUsage(),
                file.getNote(),
                file.getUpdatedAt() != null ? file.getUpdatedAt() : file.getCreatedAt()
        );
    }

    private PartDto toDto(SvgFilePart part) {
        return new PartDto(part.getPartKey(), part.getName(), part.getZone(),
                part.getFilmUsage(), part.getNote());
    }

    /** `category` của hợp đồng là cấp 1 — đi ngược lên cây tới gốc để lấy, không tin cột brand/model. */
    private String categoryValue(Category leaf) {
        Category node = leaf;
        while (node.getParent() != null) {
            node = node.getParent();
        }
        return node.getValue();
    }
}
