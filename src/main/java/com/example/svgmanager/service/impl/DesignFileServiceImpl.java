package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.response.CatalogOptionDto;
import com.example.svgmanager.dto.response.CutAreaDto;
import com.example.svgmanager.dto.response.DesignFileDto;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.entity.FileCategory;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.SvgFileVehicleNode;
import com.example.svgmanager.entity.VehicleNode;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.VehicleNodeRepository;
import com.example.svgmanager.service.DesignFileService;
import com.example.svgmanager.service.FileStorageService;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class DesignFileServiceImpl implements DesignFileService {

    private static final Logger log = LoggerFactory.getLogger(DesignFileServiceImpl.class);

    private final SvgFileRepository svgFileRepository;
    private final VehicleNodeRepository vehicleNodeRepository;
    private final FileStorageService fileStorageService;

    public DesignFileServiceImpl(SvgFileRepository svgFileRepository,
                                 VehicleNodeRepository vehicleNodeRepository,
                                 FileStorageService fileStorageService) {
        this.svgFileRepository = svgFileRepository;
        this.vehicleNodeRepository = vehicleNodeRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DesignFileDto> getFiles(String q, Long categoryId, Integer year,
                                                Long brandId, Long seriesId, Long modelId, Long subtypeId,
                                                int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size),
                Sort.by("updatedAt").descending().and(Sort.by("id").descending()));

        Specification<SvgFile> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), "ACTIVE"));

            if (StringUtils.hasText(q)) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("originalFilename")), like),
                        cb.like(cb.lower(root.get("displayName")), like)));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("fileCategory").get("id"), categoryId));
            }
            if (year != null) {
                // Q3 — file không ghi năm hiện với mọi năm khi lọc.
                predicates.add(cb.or(cb.equal(root.get("modelYear"), year),
                        cb.isNull(root.get("modelYear"))));
            }

            if (subtypeId != null) {
                VehicleNode subtype = vehicleNodeRepository.findById(subtypeId).orElse(null);
                if (subtype == null) {
                    predicates.add(cb.disjunction());
                } else {
                    List<Long> targetNodeIds = new ArrayList<>();
                    targetNodeIds.add(subtypeId);
                    if (subtype.getParent() != null) {
                        targetNodeIds.add(subtype.getParent().getId());
                    }
                    if (modelId != null && (subtype.getParent() == null || !modelId.equals(subtype.getParent().getId()))) {
                        predicates.add(cb.disjunction());
                    } else {
                        Subquery<Long> sq = query.subquery(Long.class);
                        Root<SvgFileVehicleNode> link = sq.from(SvgFileVehicleNode.class);
                        sq.select(link.get("svgFile").get("id"))
                                .where(link.get("vehicleNode").get("id").in(targetNodeIds));
                        predicates.add(root.get("id").in(sq));
                    }
                }
            } else {
                Long nodeFilter = modelId != null ? modelId
                        : seriesId != null ? seriesId
                        : brandId;
                if (nodeFilter != null) {
                    if (!vehicleNodeRepository.existsById(nodeFilter)) {
                        predicates.add(cb.disjunction());
                    } else {
                        List<Long> subtree = vehicleNodeRepository.findSubtreeIds(nodeFilter);
                        Subquery<Long> sq = query.subquery(Long.class);
                        Root<SvgFileVehicleNode> link = sq.from(SvgFileVehicleNode.class);
                        sq.select(link.get("svgFile").get("id"))
                                .where(link.get("vehicleNode").get("id").in(subtree));
                        predicates.add(root.get("id").in(sq));
                    }
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<SvgFile> pageResult = svgFileRepository.findAll(spec, pageable);
        return PageResponse.of(pageResult.map(this::toDesignFileDto));
    }

    private DesignFileDto toDesignFileDto(SvgFile file) {
        FileCategory category = file.getFileCategory();
        CatalogOptionDto categoryDto = category != null
                ? CatalogOptionDto.of(category.getId(), category.getName())
                : new CatalogOptionDto("", "");

        String path = null;
        if (file.getVehicleNodes() != null && !file.getVehicleNodes().isEmpty()) {
            VehicleNode node = file.getVehicleNodes().get(0).getVehicleNode();
            if (node != null) {
                path = vehiclePath(node);
            }
        }

        // Số part tính một lần lúc upload (V29) — server không lưu part nữa (board 08/10).
        int partCount = file.partCount();

        String displayName = file.getDisplayName() != null ? file.getDisplayName()
                : (file.getOriginalFilename() != null ? file.getOriginalFilename() : file.getRawOriginalFilename());
        DesignFileDto dto = new DesignFileDto(
                file.getFileKey(),
                displayName,
                categoryDto,
                file.getModelYear(),
                partCount,
                file.getFilmUsage(),
                file.getNote(),
                file.getUpdatedAt() != null ? file.getUpdatedAt() : file.getCreatedAt(),
                path);
        dto.setCutArea(CutAreaDto.of(file.getCutAreaLengthMm(), file.getCutAreaWidthMm()));
        dto.setHasNested(file.hasNested());
        dto.setHasRaw(file.hasRaw());
        return dto;
    }

    /** Đường dẫn tên từ gốc tới node: "Toyota › Camry › Camry 2.5Q". */
    private String vehiclePath(VehicleNode node) {
        List<String> names = new ArrayList<>();
        for (VehicleNode n = node; n != null; n = n.getParent()) {
            names.add(0, n.getName());
        }
        return String.join(" › ", names);
    }

    @Override
    @Transactional(readOnly = true)
    public DesignFileDto getFile(String fileKey) {
        return toDesignFileDto(svgFileRepository.findByFileKey(fileKey)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy file thiết kế.", ErrorCodes.FILE_NOT_FOUND)));
    }

    @Override
    @Transactional(readOnly = true)
    public String getFileSvg(String fileKey, String layout) {
        SvgFile file = svgFileRepository.findByFileKey(fileKey)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy file thiết kế.", ErrorCodes.FILE_NOT_FOUND));
        boolean raw;
        if (layout == null || layout.isBlank()) {
            raw = !file.hasNested();
        } else if ("raw".equalsIgnoreCase(layout)) {
            raw = true;
        } else if ("nested".equalsIgnoreCase(layout)) {
            raw = false;
        } else {
            throw new BadRequestException("layout phải là 'nested' hoặc 'raw'");
        }
        String path = raw ? file.getRawFilePath() : file.getFilePath();
        if (path == null || path.isBlank()) {
            throw new ResourceNotFoundException("File không có bản " + (raw ? "chưa xếp" : "đã xếp") + ".",
                    ErrorCodes.FILE_NOT_FOUND);
        }
        // File trong kho đã qua bộ khử độc lúc upload — luôn là UTF-8.
        return new String(fileStorageService.loadFileAsBytes(path), java.nio.charset.StandardCharsets.UTF_8);
    }

}
