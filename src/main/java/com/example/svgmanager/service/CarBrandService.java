package com.example.svgmanager.service;

import com.example.svgmanager.dto.request.CreateCarBrandRequest;
import com.example.svgmanager.dto.response.CarBrandResponse;
import com.example.svgmanager.entity.CarBrand;

import java.util.List;

public interface CarBrandService {
    List<CarBrandResponse> getAllBrands(String status);
    CarBrandResponse getBrandById(Long id);
    CarBrandResponse createBrand(CreateCarBrandRequest request, String actorUsername, String actorRole);
    CarBrand getEntityById(Long id);
}
