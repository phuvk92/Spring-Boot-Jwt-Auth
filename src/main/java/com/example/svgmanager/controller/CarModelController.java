package com.example.svgmanager.controller;

import com.example.svgmanager.dto.request.CreateCarModelRequest;
import com.example.svgmanager.dto.response.CarModelResponse;
import com.example.svgmanager.service.CarModelService;
import com.example.svgmanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/car-models", "/api/vehicle-configurations/models"})
@Tag(name = "Car Models", description = "Dòng xe theo hãng")
public class CarModelController {

    private final CarModelService carModelService;

    public CarModelController(CarModelService carModelService) {
        this.carModelService = carModelService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách dòng xe theo hãng")
    public ResponseEntity<List<CarModelResponse>> getModels(
            @RequestParam Long brandId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(carModelService.getModelsByBrand(brandId, status));
    }

    @GetMapping("/{id:[0-9]+}")
    @Operation(summary = "Chi tiết dòng xe")
    public ResponseEntity<CarModelResponse> getModelById(@PathVariable Long id) {
        return ResponseEntity.ok(carModelService.getModelById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo mới dòng xe (ADMIN)")
    public ResponseEntity<CarModelResponse> createModel(@Valid @RequestBody CreateCarModelRequest request) {
        String actor = SecurityUtils.getCurrentUsername();
        String role = SecurityUtils.isAdmin() ? "ADMIN" : "USER";
        return ResponseEntity.status(HttpStatus.CREATED).body(carModelService.createModel(request, actor, role));
    }
}
