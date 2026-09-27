package com.example.svgmanager.mapper;

import com.example.svgmanager.dto.response.CarBrandResponse;
import com.example.svgmanager.dto.response.CarModelResponse;
import com.example.svgmanager.dto.response.VehicleConfigurationResponse;
import com.example.svgmanager.entity.CarBrand;
import com.example.svgmanager.entity.CarModel;
import com.example.svgmanager.entity.VehicleConfiguration;
import org.springframework.stereotype.Component;

@Component
public class VehicleConfigurationMapper {

    public VehicleConfigurationResponse toResponse(VehicleConfiguration entity) {
        if (entity == null) return null;

        VehicleConfigurationResponse.CategoryRef categoryRef = entity.getCategory() != null
                ? new VehicleConfigurationResponse.CategoryRef(entity.getCategory().getId(), entity.getCategory().getLabel())
                : null;

        VehicleConfigurationResponse.BrandRef brandRef = entity.getBrand() != null
                ? new VehicleConfigurationResponse.BrandRef(entity.getBrand().getId(), entity.getBrand().getCode(), entity.getBrand().getName())
                : null;

        VehicleConfigurationResponse.ModelRef modelRef = entity.getModel() != null
                ? new VehicleConfigurationResponse.ModelRef(entity.getModel().getId(), entity.getModel().getCode(), entity.getModel().getName())
                : null;

        String productGroupName = entity.getProductGroup() != null
                ? entity.getProductGroup().getDisplayName()
                : (entity.getCategory() != null ? entity.getCategory().getLabel() : "Chưa phân nhóm");

        return new VehicleConfigurationResponse(
                entity.getId(),
                categoryRef,
                entity.getProductGroup() != null ? entity.getProductGroup().name() : null,
                productGroupName,
                brandRef,
                modelRef,
                entity.getYearFrom(),
                entity.getYearTo(),
                entity.getGenerationCode(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public CarBrandResponse toBrandResponse(CarBrand brand) {
        if (brand == null) return null;
        int count = brand.getModels() != null ? brand.getModels().size() : 0;
        return new CarBrandResponse(
                brand.getId(),
                brand.getCode(),
                brand.getName(),
                brand.getStatus(),
                brand.getDisplayOrder(),
                count,
                brand.getCreatedAt(),
                brand.getUpdatedAt()
        );
    }

    public CarModelResponse toModelResponse(CarModel model) {
        if (model == null) return null;
        Long brandId = model.getBrand() != null ? model.getBrand().getId() : null;
        String brandName = model.getBrand() != null ? model.getBrand().getName() : null;
        return new CarModelResponse(
                model.getId(),
                brandId,
                brandName,
                model.getCode(),
                model.getName(),
                model.getStatus(),
                model.getDisplayOrder(),
                model.getCreatedAt(),
                model.getUpdatedAt()
        );
    }
}
