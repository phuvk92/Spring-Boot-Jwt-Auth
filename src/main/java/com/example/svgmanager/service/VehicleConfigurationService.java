package com.example.svgmanager.service;

import com.example.svgmanager.dto.request.CreateVehicleConfigurationRequest;
import com.example.svgmanager.dto.request.UpdateVehicleConfigurationRequest;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.VehicleConfigurationResponse;

public interface VehicleConfigurationService {

    PageResponse<VehicleConfigurationResponse> getConfigurations(
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
    );

    VehicleConfigurationResponse getConfigurationById(Long id);

    VehicleConfigurationResponse createConfiguration(
            CreateVehicleConfigurationRequest request,
            String actorUsername,
            String actorRole
    );

    VehicleConfigurationResponse updateConfiguration(
            Long id,
            UpdateVehicleConfigurationRequest request,
            String actorUsername,
            String actorRole
    );

    void deleteConfiguration(
            Long id,
            String actorUsername,
            String actorRole
    );
}
