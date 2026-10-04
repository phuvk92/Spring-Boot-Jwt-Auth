package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.request.PartLibraryCategoryCreateRequest;
import com.example.svgmanager.dto.request.PartLibraryCategoryUpdateRequest;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.PartLibraryCategoryResponse;
import com.example.svgmanager.entity.PartLibraryCategory;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.repository.PartLibraryCategoryRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.PartLibraryCategoryService;
import jakarta.persistence.criteria.Predicate;
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
import java.util.stream.Collectors;

@Service
public class PartLibraryCategoryServiceImpl implements PartLibraryCategoryService {

    private static final Logger log = LoggerFactory.getLogger(PartLibraryCategoryServiceImpl.class);

    private final PartLibraryCategoryRepository categoryRepository;
    private final SvgFileRepository svgFileRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public PartLibraryCategoryServiceImpl(PartLibraryCategoryRepository categoryRepository,
                                          SvgFileRepository svgFileRepository,
                                          CurrentUserService currentUserService,
                                          AuditLogService auditLogService) {
        this.categoryRepository = categoryRepository;
        this.svgFileRepository = svgFileRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PartLibraryCategoryResponse> getCategories(String status, String search,
                                                                  int page, int size,
                                                                  String sortBy, String sortDirection) {
        String effectiveSortBy = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, effectiveSortBy));

        Specification<PartLibraryCategory> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }

            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate codeMatch = cb.like(cb.lower(root.get("code")), searchPattern);
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), searchPattern);
                predicates.add(cb.or(codeMatch, nameMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<PartLibraryCategory> pageResult = categoryRepository.findAll(spec, pageable);

        List<PartLibraryCategoryResponse> content = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<PartLibraryCategoryResponse>builder()
                .content(content)
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartLibraryCategoryResponse> getActiveCategories() {
        return categoryRepository.findByStatusOrderByNameAsc("ACTIVE").stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PartLibraryCategoryResponse getCategoryById(Long id) {
        PartLibraryCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục kho mẫu & part với ID: " + id));
        return mapToResponse(category);
    }

    @Override
    @Transactional
    public PartLibraryCategoryResponse createCategory(PartLibraryCategoryCreateRequest request) {
        String cleanCode = request.code().trim().toUpperCase();
        if (categoryRepository.existsByCode(cleanCode)) {
            throw new BadRequestException("Mã danh mục kho mẫu & part đã tồn tại: " + cleanCode);
        }

        String cleanName = request.name().trim();
        String status = StringUtils.hasText(request.status()) ? request.status().trim().toUpperCase() : "ACTIVE";

        PartLibraryCategory category = new PartLibraryCategory();
        category.setCode(cleanCode);
        category.setName(cleanName);
        category.setStatus(status);

        String actor = getCurrentActorUsername();
        category.setCreatedBy(actor);
        category.setUpdatedBy(actor);

        PartLibraryCategory saved = categoryRepository.save(category);

        auditLogService.log(actor, getCurrentActorRole(), "PART_LIBRARY_CATEGORY_CREATED",
                "PartLibraryCategory", saved.getId(),
                "code=" + cleanCode + ", name=" + cleanName + ", status=" + status);

        log.info("[PART_LIBRARY_CATEGORY_CREATED] Category id={}, code='{}', name='{}' created by {}",
                saved.getId(), cleanCode, cleanName, actor);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public PartLibraryCategoryResponse updateCategory(Long id, PartLibraryCategoryUpdateRequest request) {
        PartLibraryCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục kho mẫu & part với ID: " + id));

        String actor = getCurrentActorUsername();
        String actorRole = getCurrentActorRole();

        if (StringUtils.hasText(request.code())) {
            String newCode = request.code().trim().toUpperCase();
            if (!newCode.equals(category.getCode())) {
                if (categoryRepository.existsByCodeAndIdNot(newCode, id)) {
                    throw new BadRequestException("Mã danh mục kho mẫu & part đã tồn tại: " + newCode);
                }
                category.setCode(newCode);
            }
        }

        if (StringUtils.hasText(request.name())) {
            category.setName(request.name().trim());
        }

        if (StringUtils.hasText(request.status())) {
            String newStatus = request.status().trim().toUpperCase();
            if (!newStatus.equals(category.getStatus())) {
                category.setStatus(newStatus);
                if ("ACTIVE".equalsIgnoreCase(newStatus)) {
                    auditLogService.log(actor, actorRole, "PART_LIBRARY_CATEGORY_ACTIVATED",
                            "PartLibraryCategory", id, "status changed to ACTIVE");
                } else if ("INACTIVE".equalsIgnoreCase(newStatus)) {
                    auditLogService.log(actor, actorRole, "PART_LIBRARY_CATEGORY_DEACTIVATED",
                            "PartLibraryCategory", id, "status changed to INACTIVE");
                }
            }
        }

        category.setUpdatedBy(actor);
        PartLibraryCategory updated = categoryRepository.save(category);

        auditLogService.log(actor, actorRole, "PART_LIBRARY_CATEGORY_UPDATED",
                "PartLibraryCategory", id,
                "code=" + updated.getCode() + ", name=" + updated.getName() + ", status=" + updated.getStatus());

        log.info("[PART_LIBRARY_CATEGORY_UPDATED] Category id={} updated by {}", id, actor);

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        PartLibraryCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục kho mẫu & part với ID: " + id));

        long usageCount = svgFileRepository.countByPartLibraryCategory_Id(id);
        if (usageCount > 0) {
            throw new BadRequestException("Không thể xóa danh mục đang có " + usageCount + " part file sử dụng. Vui lòng chuyển trạng thái sang INACTIVE.");
        }

        String actor = getCurrentActorUsername();
        String actorRole = getCurrentActorRole();

        categoryRepository.delete(category);

        auditLogService.log(actor, actorRole, "PART_LIBRARY_CATEGORY_DELETED",
                "PartLibraryCategory", id,
                "code=" + category.getCode() + ", name=" + category.getName());

        log.info("[PART_LIBRARY_CATEGORY_DELETED] Category id={}, code='{}' deleted by {}",
                id, category.getCode(), actor);
    }

    private PartLibraryCategoryResponse mapToResponse(PartLibraryCategory category) {
        long usageCount = svgFileRepository.countByPartLibraryCategory_Id(category.getId());
        return new PartLibraryCategoryResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getStatus(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                category.getCreatedBy(),
                category.getUpdatedBy(),
                usageCount
        );
    }

    private String getCurrentActorUsername() {
        try {
            User user = currentUserService.getCurrentUser();
            return user != null ? user.getUsername() : "SYSTEM";
        } catch (Exception e) {
            return "SYSTEM";
        }
    }

    private String getCurrentActorRole() {
        try {
            User user = currentUserService.getCurrentUser();
            return (user != null && user.getRole() != null) ? user.getRole().name() : "ADMIN";
        } catch (Exception e) {
            return "ADMIN";
        }
    }
}
