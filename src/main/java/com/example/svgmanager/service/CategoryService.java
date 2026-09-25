package com.example.svgmanager.service;

import com.example.svgmanager.dto.request.CreateCategoryRequest;
import com.example.svgmanager.dto.request.UpdateCategoryRequest;
import com.example.svgmanager.dto.response.CatalogOptionDto;
import com.example.svgmanager.dto.response.CategoryResponse;
import com.example.svgmanager.dto.response.CategorySummaryResponse;
import com.example.svgmanager.entity.Category;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getCategoryCatalog();

    List<CatalogOptionDto> getCatalogByLevel(String level, String category, String brand, String model, String variant, String year);

    Category getCategoryById(Long id);

    CategoryResponse getCategoryResponseById(Long id);

    CategoryResponse createCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(Long id, UpdateCategoryRequest request);

    void deleteCategory(Long id);

    CategorySummaryResponse toSummary(Category category);
}
