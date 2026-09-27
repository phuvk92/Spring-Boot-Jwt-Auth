package com.example.svgmanager.service;

import com.example.svgmanager.dto.request.CreateCarModelRequest;
import com.example.svgmanager.dto.response.CarModelResponse;
import com.example.svgmanager.entity.CarModel;

import java.util.List;

public interface CarModelService {
    List<CarModelResponse> getModelsByBrand(Long brandId, String status);
    CarModelResponse getModelById(Long id);
    CarModelResponse createModel(CreateCarModelRequest request, String actorUsername, String actorRole);
    CarModel getEntityById(Long id);
}
