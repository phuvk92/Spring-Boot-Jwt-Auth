package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.request.CreateVehicleConfigurationRequest;
import com.example.svgmanager.dto.request.UpdateVehicleConfigurationRequest;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.VehicleConfigurationResponse;
import com.example.svgmanager.entity.*;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ConflictException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.mapper.VehicleConfigurationMapper;
import com.example.svgmanager.repository.CategoryRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.VehicleConfigurationRepository;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.CarBrandService;
import com.example.svgmanager.service.CarModelService;
import com.example.svgmanager.service.VehicleConfigurationService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehicleConfigurationServiceImpl implements VehicleConfigurationService {

    private final VehicleConfigurationRepository configurationRepository;
    private final CategoryRepository categoryRepository;
    private final CarBrandService carBrandService;
    private final CarModelService carModelService;
    private final SvgFileRepository svgFileRepository;
    private final VehicleConfigurationMapper mapper;
    private final AuditLogService auditLogService;

    public VehicleConfigurationServiceImpl(
            VehicleConfigurationRepository configurationRepository,
            CategoryRepository categoryRepository,
            CarBrandService carBrandService,
            CarModelService carModelService,
            SvgFileRepository svgFileRepository,
            VehicleConfigurationMapper mapper,
            AuditLogService auditLogService
    ) {
        this.configurationRepository = configurationRepository;
        this.categoryRepository = categoryRepository;
        this.carBrandService = carBrandService;
        this.carModelService = carModelService;
        this.svgFileRepository = svgFileRepository;
        this.mapper = mapper;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VehicleConfigurationResponse> getConfigurations(
            Long categoryId,
            String productGroup,
            Long brandId,
            Long modelId,
            Integer year,
            String status,
            String search,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, sortField));

        Specification<VehicleConfiguration> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Only active, non-deleted records
            predicates.add(cb.isFalse(root.get("deleted")));

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (productGroup != null && !productGroup.isBlank()) {
                try {
                    ProductGroup pg = ProductGroup.valueOf(productGroup.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("productGroup"), pg));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (brandId != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), brandId));
            }

            if (modelId != null) {
                predicates.add(cb.equal(root.get("model").get("id"), modelId));
            }

            if (year != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("yearFrom"), year));
                predicates.add(cb.greaterThanOrEqualTo(root.get("yearTo"), year));
            }

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate searchGenCode = cb.like(cb.lower(root.get("generationCode")), pattern);
                Predicate searchBrand = cb.like(cb.lower(root.get("brand").get("name")), pattern);
                Predicate searchModel = cb.like(cb.lower(root.get("model").get("name")), pattern);
                predicates.add(cb.or(searchGenCode, searchBrand, searchModel));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<VehicleConfiguration> pagedData = configurationRepository.findAll(spec, pageable);
        List<VehicleConfigurationResponse> content = pagedData.getContent().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<VehicleConfigurationResponse>builder()
                .content(content)
                .page(pagedData.getNumber())
                .size(pagedData.getSize())
                .totalElements(pagedData.getTotalElements())
                .totalPages(pagedData.getTotalPages())
                .first(pagedData.isFirst())
                .last(pagedData.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleConfigurationResponse getConfigurationById(Long id) {
        VehicleConfiguration config = configurationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Cấu hình xe với ID: " + id));
        return mapper.toResponse(config);
    }

    @Override
    @Transactional
    public VehicleConfigurationResponse createConfiguration(
            CreateVehicleConfigurationRequest request,
            String actorUsername,
            String actorRole
    ) {
        validateYearRange(request.yearFrom(), request.yearTo());

        CarBrand brand = carBrandService.getEntityById(request.brandId());
        CarModel model = carModelService.getEntityById(request.modelId());
        validateBrandModelRelationship(brand, model);

        CategoryResolved resolvedCategory = resolveCategoryAndProductGroup(request.categoryId(), request.productGroup());

        String cleanGenCode = request.generationCode().trim().toUpperCase();

        // Check duplicate
        boolean isDuplicate = configurationRepository
                .existsByCategoryIdAndBrandIdAndModelIdAndYearFromAndYearToAndGenerationCodeIgnoreCaseAndDeletedFalse(
                        resolvedCategory.category.getId(),
                        brand.getId(),
                        model.getId(),
                        request.yearFrom(),
                        request.yearTo(),
                        cleanGenCode
                );

        if (isDuplicate) {
            throw new ConflictException(
                    "VEHICLE_CONFIGURATION_ALREADY_EXISTS: Cấu hình xe đã tồn tại cho nhóm '"
                            + resolvedCategory.productGroup.getDisplayName() + "', hãng '"
                            + brand.getName() + "', dòng '" + model.getName() + "' ("
                            + request.yearFrom() + "-" + request.yearTo() + ", mã " + cleanGenCode + ")"
            );
        }

        VehicleConfiguration config = VehicleConfiguration.builder()
                .category(resolvedCategory.category)
                .productGroup(resolvedCategory.productGroup)
                .brand(brand)
                .model(model)
                .yearFrom(request.yearFrom())
                .yearTo(request.yearTo())
                .generationCode(cleanGenCode)
                .status(request.status() != null && !request.status().isBlank() ? request.status().trim().toUpperCase() : "ACTIVE")
                .deleted(false)
                .build();

        VehicleConfiguration saved = configurationRepository.save(config);

        auditLogService.log(
                actorUsername,
                actorRole,
                "CREATE_VEHICLE_CONFIGURATION",
                "VehicleConfiguration",
                saved.getId(),
                "Tạo mới cấu hình xe: " + brand.getName() + " " + model.getName() + " (" + cleanGenCode + ") - "
                        + resolvedCategory.productGroup.getDisplayName()
        );

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public VehicleConfigurationResponse updateConfiguration(
            Long id,
            UpdateVehicleConfigurationRequest request,
            String actorUsername,
            String actorRole
    ) {
        VehicleConfiguration config = configurationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Cấu hình xe với ID: " + id));

        validateYearRange(request.yearFrom(), request.yearTo());

        CarBrand brand = carBrandService.getEntityById(request.brandId());
        CarModel model = carModelService.getEntityById(request.modelId());
        validateBrandModelRelationship(brand, model);

        CategoryResolved resolvedCategory = resolveCategoryAndProductGroup(request.categoryId(), request.productGroup());

        String cleanGenCode = request.generationCode().trim().toUpperCase();

        // Check duplicate excluding this ID
        boolean isDuplicate = configurationRepository.existsDuplicateExcludingId(
                resolvedCategory.category.getId(),
                brand.getId(),
                model.getId(),
                request.yearFrom(),
                request.yearTo(),
                cleanGenCode,
                id
        );

        if (isDuplicate) {
            throw new ConflictException(
                    "VEHICLE_CONFIGURATION_ALREADY_EXISTS: Cấu hình xe đã tồn tại cho nhóm '"
                            + resolvedCategory.productGroup.getDisplayName() + "', hãng '"
                            + brand.getName() + "', dòng '" + model.getName() + "' ("
                            + request.yearFrom() + "-" + request.yearTo() + ", mã " + cleanGenCode + ")"
            );
        }

        config.setCategory(resolvedCategory.category);
        config.setProductGroup(resolvedCategory.productGroup);
        config.setBrand(brand);
        config.setModel(model);
        config.setYearFrom(request.yearFrom());
        config.setYearTo(request.yearTo());
        config.setGenerationCode(cleanGenCode);
        if (request.status() != null && !request.status().isBlank()) {
            config.setStatus(request.status().trim().toUpperCase());
        }

        VehicleConfiguration updated = configurationRepository.save(config);

        auditLogService.log(
                actorUsername,
                actorRole,
                "UPDATE_VEHICLE_CONFIGURATION",
                "VehicleConfiguration",
                updated.getId(),
                "Cập nhật cấu hình xe ID: " + id + " (" + brand.getName() + " " + model.getName() + ")"
        );

        return mapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteConfiguration(Long id, String actorUsername, String actorRole) {
        VehicleConfiguration config = configurationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Cấu hình xe với ID: " + id));

        // Check if referenced by SVG files
        boolean inUse = svgFileRepository.existsByVehicleConfigurationId(id);
        if (inUse) {
            throw new ConflictException("VEHICLE_CONFIGURATION_IN_USE: Cấu hình xe đang được liên kết với dữ liệu file SVG, không thể xóa");
        }

        // Soft delete to preserve referential integrity and audit history
        config.setDeleted(true);
        config.setStatus("INACTIVE");
        configurationRepository.save(config);

        auditLogService.log(
                actorUsername,
                actorRole,
                "DELETE_VEHICLE_CONFIGURATION",
                "VehicleConfiguration",
                id,
                "Xóa cấu hình xe: " + config.getBrand().getName() + " " + config.getModel().getName() + " (" + config.getGenerationCode() + ")"
        );
    }

    private void validateYearRange(Integer yearFrom, Integer yearTo) {
        if (yearFrom != null && yearTo != null && yearFrom > yearTo) {
            throw new BadRequestException("Năm sản xuất từ (" + yearFrom + ") không được lớn hơn năm đến (" + yearTo + ")");
        }
    }

    private void validateBrandModelRelationship(CarBrand brand, CarModel model) {
        if (model.getBrand() == null || !model.getBrand().getId().equals(brand.getId())) {
            throw new BadRequestException("Dòng xe '" + model.getName() + "' (ID: " + model.getId()
                    + ") không thuộc Hãng xe '" + brand.getName() + "' (ID: " + brand.getId() + ")");
        }
    }

    private record CategoryResolved(Category category, ProductGroup productGroup) {}

    private CategoryResolved resolveCategoryAndProductGroup(Long categoryId, String productGroupStr) {
        ProductGroup pg = null;
        if (productGroupStr != null && !productGroupStr.isBlank()) {
            try {
                pg = ProductGroup.valueOf(productGroupStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Danh mục (Category) với ID: " + categoryId));
            if (pg == null) {
                pg = ProductGroup.fromCategoryId(category.getId());
            }
        } else if (pg != null) {
            category = categoryRepository.findById(pg.getDefaultCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Danh mục mặc định cho nhóm " + productGroupStr));
        }

        if (category == null) {
            // Default to PPF Exterior category (id: 1)
            category = categoryRepository.findById(1L)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục nhóm sản phẩm"));
        }
        if (pg == null) {
            pg = ProductGroup.fromCategoryId(category.getId());
            if (pg == null) {
                pg = ProductGroup.PPF_EXTERIOR;
            }
        }

        return new CategoryResolved(category, pg);
    }
}
