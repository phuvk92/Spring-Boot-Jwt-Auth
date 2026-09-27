package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.request.CreateCarModelRequest;
import com.example.svgmanager.dto.response.CarModelResponse;
import com.example.svgmanager.entity.CarBrand;
import com.example.svgmanager.entity.CarModel;
import com.example.svgmanager.exception.ConflictException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.mapper.VehicleConfigurationMapper;
import com.example.svgmanager.repository.CarModelRepository;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.CarBrandService;
import com.example.svgmanager.service.CarModelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CarModelServiceImpl implements CarModelService {

    private final CarModelRepository carModelRepository;
    private final CarBrandService carBrandService;
    private final VehicleConfigurationMapper mapper;
    private final AuditLogService auditLogService;

    public CarModelServiceImpl(CarModelRepository carModelRepository,
                               CarBrandService carBrandService,
                               VehicleConfigurationMapper mapper,
                               AuditLogService auditLogService) {
        this.carModelRepository = carModelRepository;
        this.carBrandService = carBrandService;
        this.mapper = mapper;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarModelResponse> getModelsByBrand(Long brandId, String status) {
        // Ensure brand exists
        carBrandService.getEntityById(brandId);

        List<CarModel> models;
        if (status != null && !status.isBlank()) {
            models = carModelRepository.findByBrandIdAndStatusOrderByDisplayOrderAscNameAsc(brandId, status);
        } else {
            models = carModelRepository.findByBrandIdOrderByDisplayOrderAscNameAsc(brandId);
        }
        return models.stream().map(mapper::toModelResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CarModelResponse getModelById(Long id) {
        return mapper.toModelResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CarModel getEntityById(Long id) {
        return carModelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Dòng xe với ID: " + id));
    }

    @Override
    @Transactional
    public CarModelResponse createModel(CreateCarModelRequest request, String actorUsername, String actorRole) {
        CarBrand brand = carBrandService.getEntityById(request.brandId());
        String cleanCode = request.code().trim().toUpperCase();

        if (carModelRepository.existsByBrandIdAndCodeIgnoreCase(brand.getId(), cleanCode)) {
            throw new ConflictException("Dòng xe với mã '" + cleanCode + "' đã tồn tại cho hãng " + brand.getName());
        }

        CarModel model = CarModel.builder()
                .brand(brand)
                .code(cleanCode)
                .name(request.name().trim())
                .status(request.status() != null ? request.status().trim().toUpperCase() : "ACTIVE")
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .build();

        CarModel saved = carModelRepository.save(model);
        auditLogService.log(actorUsername, actorRole, "CREATE_CAR_MODEL", "CarModel", saved.getId(),
                "Tạo mới dòng xe: " + saved.getName() + " thuộc hãng " + brand.getName());

        return mapper.toModelResponse(saved);
    }
}
