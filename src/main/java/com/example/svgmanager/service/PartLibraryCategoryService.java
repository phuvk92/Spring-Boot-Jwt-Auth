package com.example.svgmanager.service;

import com.example.svgmanager.dto.request.PartLibraryCategoryCreateRequest;
import com.example.svgmanager.dto.request.PartLibraryCategoryUpdateRequest;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.PartLibraryCategoryResponse;

import java.util.List;

public interface PartLibraryCategoryService {

    PageResponse<PartLibraryCategoryResponse> getCategories(String status, String search, int page, int size, String sortBy, String sortDirection);

    List<PartLibraryCategoryResponse> getActiveCategories();

    PartLibraryCategoryResponse getCategoryById(Long id);

    PartLibraryCategoryResponse createCategory(PartLibraryCategoryCreateRequest request);

    PartLibraryCategoryResponse updateCategory(Long id, PartLibraryCategoryUpdateRequest request);

    void deleteCategory(Long id);
}
