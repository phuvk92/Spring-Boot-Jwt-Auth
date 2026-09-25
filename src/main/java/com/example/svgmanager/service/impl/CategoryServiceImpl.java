package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.request.CreateCategoryRequest;
import com.example.svgmanager.dto.request.UpdateCategoryRequest;
import com.example.svgmanager.dto.response.CatalogOptionDto;
import com.example.svgmanager.dto.response.CategoryResponse;
import com.example.svgmanager.dto.response.CategorySummaryResponse;
import com.example.svgmanager.entity.Category;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ConflictException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.repository.CategoryRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;
    private final SvgFileRepository svgFileRepository;
    private final CurrentUserService currentUserService;

    public CategoryServiceImpl(
            CategoryRepository categoryRepository,
            SvgFileRepository svgFileRepository,
            CurrentUserService currentUserService
    ) {
        this.categoryRepository = categoryRepository;
        this.svgFileRepository = svgFileRepository;
        this.currentUserService = currentUserService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryCatalog() {
        List<Category> rootCategories = categoryRepository.findByParentIsNullOrderByDisplayOrderAscIdAsc();
        return rootCategories.stream()
                .map(this::mapToTreeResponse)
                .collect(Collectors.toList());
    }

    private CategoryResponse mapToTreeResponse(Category category) {
        List<CategoryResponse> childResponses = category.getChildren() != null
                ? category.getChildren().stream()
                .map(this::mapToTreeResponse)
                .collect(Collectors.toList())
                : new ArrayList<>();

        return new CategoryResponse(
                category.getId(),
                category.getValue(),
                category.getLabel(),
                category.getLevel(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getDisplayOrder(),
                childResponses
        );
    }

    private CategoryResponse mapToSingleResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getValue(),
                category.getLabel(),
                category.getLevel(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getDisplayOrder(),
                category.getChildren() != null
                        ? category.getChildren().stream().map(this::mapToTreeResponse).collect(Collectors.toList())
                        : new ArrayList<>()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogOptionDto> getCatalogByLevel(
            String level,
            String category,
            String brand,
            String model,
            String variant,
            String year
    ) {
        if ("category".equalsIgnoreCase(level)) {
            return categoryRepository.findByLevelOrderByDisplayOrderAscIdAsc("category").stream()
                    .map(c -> new CatalogOptionDto(c.getValue(), c.getLabel()))
                    .collect(Collectors.toList());
        }

        if ("brand".equalsIgnoreCase(level)) {
            String parentValue = category != null ? category : "Ngoại thất";
            return categoryRepository.findByLevelAndParentValue("brand", parentValue).stream()
                    .map(c -> new CatalogOptionDto(c.getValue(), c.getLabel()))
                    .collect(Collectors.toList());
        }

        if ("model".equalsIgnoreCase(level)) {
            String parentValue = brand != null ? brand : "Abarth";
            return categoryRepository.findByLevelAndParentValue("model", parentValue).stream()
                    .map(c -> new CatalogOptionDto(c.getValue(), c.getLabel()))
                    .collect(Collectors.toList());
        }

        if ("variant".equalsIgnoreCase(level)) {
            String parentValue = model != null ? model : "695";
            return categoryRepository.findByLevelAndParentValue("variant", parentValue).stream()
                    .map(c -> new CatalogOptionDto(c.getValue(), c.getLabel()))
                    .collect(Collectors.toList());
        }

        if ("year".equalsIgnoreCase(level)) {
            String parentValue = variant != null ? variant : "695";
            return categoryRepository.findByLevelAndParentValue("year", parentValue).stream()
                    .map(c -> new CatalogOptionDto(c.getValue(), c.getLabel()))
                    .collect(Collectors.toList());
        }

        if ("submodel".equalsIgnoreCase(level)) {
            String parentValue = year != null ? year : "2024";
            return categoryRepository.findByLevelAndParentValue("submodel", parentValue).stream()
                    .map(c -> new CatalogOptionDto(c.getValue(), c.getLabel()))
                    .collect(Collectors.toList());
        }

        return categoryRepository.findByLevelOrderByDisplayOrderAscIdAsc(level).stream()
                .map(c -> new CatalogOptionDto(c.getValue(), c.getLabel()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryResponseById(Long id) {
        Category category = getCategoryById(id);
        return mapToSingleResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (request.getValue() == null || request.getValue().trim().isEmpty()) {
            throw new BadRequestException("Category value/code is required");
        }
        String value = request.getValue().trim();
        String label = (request.getLabel() != null && !request.getLabel().trim().isEmpty())
                ? request.getLabel().trim()
                : value;

        Category parent = null;
        String level;

        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BadRequestException("Parent category not found with id: " + request.getParentId()));

            level = calculateNextLevel(parent.getLevel());

            if (categoryRepository.existsByValueAndParentId(value, parent.getId())) {
                throw new ConflictException("Category with value '" + value + "' already exists under parent '" + parent.getLabel() + "'");
            }
        } else {
            level = "category";
            if (categoryRepository.existsByValueAndParentIsNull(value)) {
                throw new ConflictException("Root category with value '" + value + "' already exists");
            }
        }

        Category category = Category.builder()
                .value(value)
                .label(label)
                .level(level)
                .parent(parent)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();

        Category saved = categoryRepository.save(category);

        String actor = currentUserService.getCurrentJwt()
                .map(j -> j.getClaimAsString("preferred_username"))
                .orElse("system");

        log.info("[CATEGORY_CREATED] Category created: id={}, value='{}', label='{}', level='{}', parentId={}, actor='{}'",
                saved.getId(), saved.getValue(), saved.getLabel(), saved.getLevel(),
                saved.getParent() != null ? saved.getParent().getId() : null, actor);

        return mapToSingleResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (request.getValue() == null || request.getValue().trim().isEmpty()) {
            throw new BadRequestException("Category value/code is required");
        }
        String value = request.getValue().trim();
        String label = (request.getLabel() != null && !request.getLabel().trim().isEmpty())
                ? request.getLabel().trim()
                : value;

        Long currentParentId = category.getParent() != null ? category.getParent().getId() : null;
        Long newParentId = request.getParentId();

        if (!Objects.equals(currentParentId, newParentId)) {
            if (newParentId != null) {
                if (newParentId.equals(id)) {
                    throw new BadRequestException("Parent category cannot be the category itself");
                }

                Category newParent = categoryRepository.findById(newParentId)
                        .orElseThrow(() -> new BadRequestException("Parent category not found with id: " + newParentId));

                // Anti-circular dependency check
                Category check = newParent;
                while (check != null) {
                    if (check.getId().equals(id)) {
                        throw new BadRequestException("Circular hierarchy detected: cannot set descendant category as parent");
                    }
                    check = check.getParent();
                }

                String newLevel = calculateNextLevel(newParent.getLevel());
                validateAndUpdateSubtreeLevels(category, newLevel);
                category.setParent(newParent);
            } else {
                // Moving to root
                String newLevel = "category";
                validateAndUpdateSubtreeLevels(category, newLevel);
                category.setParent(null);
            }
        }

        category.setValue(value);
        category.setLabel(label);
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }

        Category saved = categoryRepository.save(category);

        String actor = currentUserService.getCurrentJwt()
                .map(j -> j.getClaimAsString("preferred_username"))
                .orElse("system");

        log.info("[CATEGORY_UPDATED] Category updated: id={}, value='{}', label='{}', level='{}', parentId={}, actor='{}'",
                saved.getId(), saved.getValue(), saved.getLabel(), saved.getLevel(),
                saved.getParent() != null ? saved.getParent().getId() : null, actor);

        return mapToSingleResponse(saved);
    }

    private void validateAndUpdateSubtreeLevels(Category node, String level) {
        node.setLevel(level);
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            String nextChildLevel = calculateNextLevel(level);
            for (Category child : node.getChildren()) {
                validateAndUpdateSubtreeLevels(child, nextChildLevel);
            }
        }
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // Case 2: Category has children
        if (categoryRepository.existsByParentId(id)) {
            throw new ConflictException("CATEGORY_HAS_CHILDREN: Không thể xóa Category vì vẫn còn Category con.");
        }

        // Case 3: Category is used by SVG files
        if (svgFileRepository.existsByCategoryId(id)) {
            throw new ConflictException("CATEGORY_IN_USE: Không thể xóa Category vì đang được sử dụng bởi SVG.");
        }

        // Case 1: Safe to delete
        categoryRepository.delete(category);

        String actor = currentUserService.getCurrentJwt()
                .map(j -> j.getClaimAsString("preferred_username"))
                .orElse("system");

        log.info("[CATEGORY_DELETED] Category deleted: id={}, value='{}', label='{}', actor='{}'",
                id, category.getValue(), category.getLabel(), actor);
    }

    public static String calculateNextLevel(String parentLevel) {
        if (parentLevel == null) {
            return "category";
        }
        return switch (parentLevel.toLowerCase()) {
            case "category" -> "brand";
            case "brand" -> "model";
            case "model" -> "variant";
            case "variant" -> "year";
            case "year" -> "submodel";
            default -> throw new BadRequestException("Cannot create child category under submodel level (max depth 6 reached)");
        };
    }

    @Override
    @Transactional(readOnly = true)
    public CategorySummaryResponse toSummary(Category category) {
        if (category == null) {
            return null;
        }

        List<String> pathSegments = new ArrayList<>();
        Category curr = category;
        while (curr != null) {
            pathSegments.add(0, curr.getLabel());
            curr = curr.getParent();
        }
        String fullPath = String.join(" / ", pathSegments);

        return new CategorySummaryResponse(
                category.getId(),
                category.getLabel(),
                category.getValue(),
                category.getLevel(),
                fullPath
        );
    }
}
