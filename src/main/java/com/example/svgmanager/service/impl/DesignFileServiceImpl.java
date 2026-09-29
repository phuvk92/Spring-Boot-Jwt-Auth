package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.response.CatalogOptionDto;
import com.example.svgmanager.dto.response.DesignFileDto;
import com.example.svgmanager.dto.response.DesignFileGeometryDto;
import com.example.svgmanager.dto.response.PartDto;
import com.example.svgmanager.dto.response.PartOutlineDto;
import com.example.svgmanager.entity.FileCategory;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.SvgFilePart;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.repository.SvgFilePartRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.VehicleNodeRepository;
import com.example.svgmanager.service.DesignFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    public List<DesignFileDto> getFiles(String categoryId, String modelId, String subtypeId, Integer year) {
        Long category = requireId(categoryId, "categoryId");
        Long model = requireId(modelId, "modelId");
        Long subtype = parseId(subtypeId, "subtypeId");

        // SA §3.3: có subtype → khớp subtype hoặc model cha; không → model + mọi phiên bản
        List<Long> nodeIds = new ArrayList<>();
        nodeIds.add(model);
        if (subtype != null) {
            nodeIds.add(subtype);
        } else {
            vehicleNodeRepository.findByParentIdOrderByDisplayOrderAscIdAsc(model)
                    .forEach(child -> nodeIds.add(child.getId()));
        }

        return svgFileRepository.findCatalogFiles(category, nodeIds, year)
                .stream().map(this::toDesignFileDto).toList();
    }

    private DesignFileDto toDesignFileDto(SvgFile file) {
        FileCategory category = file.getFileCategory();
        CatalogOptionDto categoryDto = category != null
                ? CatalogOptionDto.of(category.getId(), category.getName())
                : new CatalogOptionDto("", "");
        return new DesignFileDto(
                file.getFileKey(),
                file.getDisplayName() != null ? file.getDisplayName() : file.getOriginalFilename(),
                categoryDto,
                file.getModelYear(),
                file.getParts() != null ? file.getParts().size() : 0,
                file.getFilmUsage(),
                file.getNote(),
                file.getUpdatedAt() != null ? file.getUpdatedAt() : file.getCreatedAt());
    }

    private Long requireId(String raw, String param) {
        Long id = parseId(raw, param);
        if (id == null) {
            throw new BadRequestException("Thiếu tham số bắt buộc '" + param + "'");
        }
        return id;
    }

    private Long parseId(String raw, String param) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("Tham số '" + param + "' phải là id số, nhận: " + raw);
        }
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
