package com.example.svgmanager.controller;

import com.example.svgmanager.dto.request.CreateCategoryRequest;
import com.example.svgmanager.dto.request.UpdateCategoryRequest;
import com.example.svgmanager.dto.response.CatalogOptionDto;
import com.example.svgmanager.dto.response.CategoryResponse;
import com.example.svgmanager.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Category Management", description = "Endpoints for vehicle catalog and category hierarchy management")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/api/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Get Category Catalog", description = "Returns full hierarchical tree of categories or level options if level param is provided.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categories catalog retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CategoryResponse.class))))
    })
    public ResponseEntity<?> getCategories(
            @Parameter(description = "Optional filter level: category, brand, model, variant, year, submodel")
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String variant,
            @RequestParam(required = false) String year
    ) {
        if (level != null && !level.isBlank()) {
            List<CatalogOptionDto> options = categoryService.getCatalogByLevel(level, category, brand, model, variant, year);
            return ResponseEntity.ok(options);
        }
        List<CategoryResponse> catalog = categoryService.getCategoryCatalog();
        return ResponseEntity.ok(catalog);
    }

    @GetMapping("/api/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Get Category by ID", description = "Returns single category details by its ID.")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        CategoryResponse category = categoryService.getCategoryResponseById(id);
        return ResponseEntity.ok(category);
    }

    @PostMapping("/api/categories")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Category", description = "ADMIN only: Create a new category node in the catalog hierarchy.")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse created = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Category", description = "ADMIN only: Update category details, parent, or display order.")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        CategoryResponse updated = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/api/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete Category", description = "ADMIN only: Delete a category if it has no children and is not used by any SVG files.")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/catalog/{level}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Get catalog by level", description = "Contract endpoint matching 06-catalog-level.json specifications.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Catalog options for specified level",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CatalogOptionDto.class))))
    })
    public ResponseEntity<List<CatalogOptionDto>> getCatalogByLevel(
            @PathVariable String level,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String variant,
            @RequestParam(required = false) String year
    ) {
        List<CatalogOptionDto> options = categoryService.getCatalogByLevel(level, category, brand, model, variant, year);
        return ResponseEntity.ok(options);
    }
}
