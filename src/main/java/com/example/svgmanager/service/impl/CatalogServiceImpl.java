package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.response.CatalogOptionDto;
import com.example.svgmanager.entity.VehicleNode;
import com.example.svgmanager.entity.VehicleNodeLevel;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.repository.FileCategoryRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.VehicleNodeRepository;
import com.example.svgmanager.service.CatalogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Catalog 4 cấp cho app thợ (SA-DanhMucXe-v2 §3.3). Khác bản cũ ở hai chỗ:
 * lọc con theo **id** cha (hai nhánh trùng tên không còn bị trộn) và thiếu id cha
 * trả 400 thay vì tự điền giá trị mẫu.
 */
@Service
public class CatalogServiceImpl implements CatalogService {

    private final FileCategoryRepository fileCategoryRepository;
    private final VehicleNodeRepository vehicleNodeRepository;
    private final SvgFileRepository svgFileRepository;

    public CatalogServiceImpl(FileCategoryRepository fileCategoryRepository,
                              VehicleNodeRepository vehicleNodeRepository,
                              SvgFileRepository svgFileRepository) {
        this.fileCategoryRepository = fileCategoryRepository;
        this.vehicleNodeRepository = vehicleNodeRepository;
        this.svgFileRepository = svgFileRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogOptionDto> getFileCategories() {
        return fileCategoryRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc()
                .stream()
                .map(c -> CatalogOptionDto.of(c.getId(), c.getName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogOptionDto> getCatalog(String level, String brandId, String seriesId,
                                             String modelId, String subtypeId, String categoryId) {
        return switch (level) {
            case "brand" -> vehicleNodeRepository.findByParentIsNullOrderByDisplayOrderAscIdAsc()
                    .stream().map(this::toOption).toList();
            case "series" -> childrenOf(requireParent(brandId, "brandId", VehicleNodeLevel.BRAND, level));
            case "model" -> childrenOf(requireParent(seriesId, "seriesId", VehicleNodeLevel.SERIES, level));
            case "subtype" -> childrenOf(requireParent(modelId, "modelId", VehicleNodeLevel.MODEL, level));
            case "year" -> years(categoryId, brandId, seriesId, modelId, subtypeId);
            default -> throw new BadRequestException(
                    "Cấp danh mục không hợp lệ: " + level + " (brand | series | model | subtype | year)");
        };
    }

    private List<CatalogOptionDto> childrenOf(VehicleNode parent) {
        return vehicleNodeRepository.findByParentIdOrderByDisplayOrderAscIdAsc(parent.getId())
                .stream().map(this::toOption).toList();
    }

    private List<CatalogOptionDto> years(String categoryId, String brandId, String seriesId,
                                         String modelId, String subtypeId) {
        Long category = parseId(categoryId, "categoryId");
        Long brand = parseId(brandId, "brandId");
        Long series = parseId(seriesId, "seriesId");
        Long model = parseId(modelId, "modelId");
        Long subtype = parseId(subtypeId, "subtypeId");

        boolean noNodeFilter = (brand == null && series == null && model == null && subtype == null);
        List<Long> nodeIds;
        if (noNodeFilter) {
            nodeIds = List.of(-1L);
        } else {
            nodeIds = resolveFilterNodes(brand, series, model, subtype);
            if (nodeIds.isEmpty()) {
                return List.of();
            }
        }
        List<Integer> years = svgFileRepository.findCatalogYears(category, nodeIds, noNodeFilter);
        return years.stream().map(y -> new CatalogOptionDto(String.valueOf(y), String.valueOf(y))).toList();
    }

    private List<Long> resolveFilterNodes(Long brandId, Long seriesId, Long modelId, Long subtypeId) {
        if (subtypeId != null) {
            VehicleNode subtype = vehicleNodeRepository.findById(subtypeId).orElse(null);
            if (subtype == null) {
                return List.of();
            }
            List<Long> ids = new ArrayList<>();
            ids.add(subtypeId);
            if (subtype.getParent() != null) {
                ids.add(subtype.getParent().getId());
            }
            return ids;
        }
        Long nodeFilter = modelId != null ? modelId
                : seriesId != null ? seriesId
                : brandId;
        if (nodeFilter != null) {
            if (!vehicleNodeRepository.existsById(nodeFilter)) {
                return List.of();
            }
            return vehicleNodeRepository.findSubtreeIds(nodeFilter);
        }
        return List.of();
    }

    /**
     * Tham số id của cấp cha: thiếu → 400; có mà sai cấp (vd truyền id hãng vào seriesId)
     * cũng 400 — gọi sai kiểu đó là lỗi client, không phải "cây rỗng".
     */
    private VehicleNode requireParent(String rawId, String param, VehicleNodeLevel expectedLevel, String level) {
        Long id = parseId(rawId, param);
        if (id == null) {
            throw new BadRequestException("Thiếu tham số bắt buộc '" + param + "' cho cấp " + level);
        }
        VehicleNode parent = vehicleNodeRepository.findById(id).orElse(null);
        if (parent == null || parent.getLevel() != expectedLevel) {
            throw new BadRequestException(
                    "Tham số '" + param + "' không trỏ tới node cấp " + expectedLevel);
        }
        return parent;
    }

    /** Id truyền dạng chuỗi (hợp đồng CatalogOption.value là string) — không phải số là 400. */
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

    private CatalogOptionDto toOption(VehicleNode node) {
        return CatalogOptionDto.of(node.getId(), node.getName());
    }
}
