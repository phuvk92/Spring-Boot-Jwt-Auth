package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.request.CreateCarBrandRequest;
import com.example.svgmanager.dto.response.CarBrandResponse;
import com.example.svgmanager.entity.CarBrand;
import com.example.svgmanager.exception.ConflictException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.mapper.VehicleConfigurationMapper;
import com.example.svgmanager.repository.CarBrandRepository;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.CarBrandService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CarBrandServiceImpl implements CarBrandService {

    private final CarBrandRepository carBrandRepository;
    private final VehicleConfigurationMapper mapper;
    private final AuditLogService auditLogService;

    public CarBrandServiceImpl(CarBrandRepository carBrandRepository,
                               VehicleConfigurationMapper mapper,
                               AuditLogService auditLogService) {
        this.carBrandRepository = carBrandRepository;
        this.mapper = mapper;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarBrandResponse> getAllBrands(String status) {
        List<CarBrand> brands;
        if (status != null && !status.isBlank()) {
            brands = carBrandRepository.findByStatusOrderByDisplayOrderAscNameAsc(status);
        } else {
            brands = carBrandRepository.findAll(Sort.by(Sort.Direction.ASC, "displayOrder").and(Sort.by(Sort.Direction.ASC, "name")));
        }
        return brands.stream().map(mapper::toBrandResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CarBrandResponse getBrandById(Long id) {
        return mapper.toBrandResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CarBrand getEntityById(Long id) {
        return carBrandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Hãng xe với ID: " + id));
    }

    @Override
    @Transactional
    public CarBrandResponse createBrand(CreateCarBrandRequest request, String actorUsername, String actorRole) {
        String cleanCode = request.code().trim().toUpperCase();
        if (carBrandRepository.existsByCodeIgnoreCase(cleanCode)) {
            throw new ConflictException("Mã hãng xe '" + cleanCode + "' đã tồn tại trong hệ thống");
        }

        CarBrand brand = CarBrand.builder()
                .code(cleanCode)
                .name(request.name().trim())
                .status(request.status() != null ? request.status().trim().toUpperCase() : "ACTIVE")
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .build();

        CarBrand saved = carBrandRepository.save(brand);
        auditLogService.log(actorUsername, actorRole, "CREATE_CAR_BRAND", "CarBrand", saved.getId(),
                "Tạo mới hãng xe: " + saved.getName() + " (" + saved.getCode() + ")");

        return mapper.toBrandResponse(saved);
    }
}
