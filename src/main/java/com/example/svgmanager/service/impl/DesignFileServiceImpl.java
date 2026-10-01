package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.response.CatalogOptionDto;
import com.example.svgmanager.dto.response.DesignFileDto;
import com.example.svgmanager.dto.response.DesignFileGeometryDto;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.PartDto;
import com.example.svgmanager.dto.response.PartOutlineDto;
import com.example.svgmanager.entity.FileCategory;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.SvgFilePart;
import com.example.svgmanager.entity.SvgFileVehicleNode;
import com.example.svgmanager.entity.VehicleNode;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.repository.SvgFilePartRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.VehicleNodeRepository;
import com.example.svgmanager.service.DesignFileService;
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

    /**
     * Ngưỡng cảnh báo payload geometry (AC F-56 — mục streaming): hợp đồng ghi "cân nhắc
     * streaming", quyết định đợt này là JSON thường + log khi vượt ngưỡng; hoãn streaming
     * tới khi có file thật vượt ngưỡng, không tối ưu trước khi có số đo.
     */
    private static final long GEOMETRY_WARN_BYTES = 2L * 1024 * 1024;

    private final SvgFileRepository svgFileRepository;
    private final SvgFilePartRepository svgFilePartRepository;
    private final VehicleNodeRepository vehicleNodeRepository;

    public DesignFileServiceImpl(SvgFileRepository svgFileRepository,
                                 SvgFilePartRepository svgFilePartRepository,
                                 VehicleNodeRepository vehicleNodeRepository) {
        this.svgFileRepository = svgFileRepository;
        this.svgFilePartRepository = svgFilePartRepository;
        this.vehicleNodeRepository = vehicleNodeRepository;
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

        int partCount = file.getId() == null
                ? (file.getParts() != null ? file.getParts().size() : 0)
                : (int) svgFilePartRepository.countBySvgFileId(file.getId());

        return new DesignFileDto(
                file.getFileKey(),
                file.getDisplayName() != null ? file.getDisplayName() : file.getOriginalFilename(),
                categoryDto,
                file.getModelYear(),
                partCount,
                file.getFilmUsage(),
                file.getNote(),
                file.getUpdatedAt() != null ? file.getUpdatedAt() : file.getCreatedAt(),
                path);
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
    public List<PartDto> getFileParts(String fileKey) {
        SvgFile file = svgFileRepository.findByFileKey(fileKey)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy file thiết kế.", ErrorCodes.FILE_NOT_FOUND));
        return svgFilePartRepository.findBySvgFileIdOrderByDisplayOrderAscIdAsc(file.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DesignFileGeometryDto getFileGeometry(String fileKey) {
        SvgFile file = svgFileRepository.findByFileKey(fileKey)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy file thiết kế.", ErrorCodes.FILE_NOT_FOUND));
        List<PartOutlineDto> parts = svgFilePartRepository
                .findBySvgFileIdOrderByDisplayOrderAscIdAsc(file.getId())
                .stream()
                .map(part -> toOutlineDto(file.getFileKey(), part))
                .toList();
        long estimatedBytes = parts.stream()
                .mapToLong(p -> p.getPathData() != null ? p.getPathData().length() : 0)
                .sum();
        if (estimatedBytes > GEOMETRY_WARN_BYTES) {
            log.warn("Geometry của file '{}' ước tính {} byte, vượt ngưỡng {} — cân nhắc streaming khi có file thật",
                    fileKey, estimatedBytes, GEOMETRY_WARN_BYTES);
        }
        return new DesignFileGeometryDto(
                file.getFileKey(),
                file.getDisplayName() != null ? file.getDisplayName() : file.getOriginalFilename(),
                parts);
    }

    /** partId của hợp đồng là ghép {@code <fileId>--<partKey>} — khớp fixture 09. */
    private PartOutlineDto toOutlineDto(String fileKey, SvgFilePart part) {
        return new PartOutlineDto(
                fileKey + "--" + part.getPartKey(),
                part.getName(),
                // Thiếu hình học (chưa nạp) → giá trị an toàn, không ném
                part.getPathData() != null ? part.getPathData() : "",
                part.getWidthMm() != null ? part.getWidthMm() : 0.0,
                part.getHeightMm() != null ? part.getHeightMm() : 0.0,
                part.getXMm() != null ? part.getXMm() : 0.0,
                part.getYMm() != null ? part.getYMm() : 0.0,
                part.getNodeCount() != null ? part.getNodeCount() : 0,
                part.getHoleCount() != null ? part.getHoleCount() : 0);
    }

    private PartDto toDto(SvgFilePart part) {
        return new PartDto(part.getPartKey(), part.getName(), part.getZone(),
                part.getFilmUsage(), part.getNote());
    }

}
