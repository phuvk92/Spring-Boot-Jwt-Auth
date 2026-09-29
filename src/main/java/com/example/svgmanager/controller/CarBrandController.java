package com.example.svgmanager.controller;

import com.example.svgmanager.dto.request.CreateCarBrandRequest;
import com.example.svgmanager.dto.response.CarBrandResponse;
import com.example.svgmanager.service.CarBrandService;
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
@RequestMapping({"/api/car-brands", "/api/vehicle-configurations/brands"})
@Tag(name = "Car Brands", description = "Hãng xe")
public class CarBrandController {

    private final CarBrandService carBrandService;

    public CarBrandController(CarBrandService carBrandService) {
        this.carBrandService = carBrandService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách hãng xe")
    public ResponseEntity<List<CarBrandResponse>> getBrands(
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(carBrandService.getAllBrands(status));
    }

    @GetMapping("/{id:[0-9]+}")
    @Operation(summary = "Chi tiết hãng xe")
    public ResponseEntity<CarBrandResponse> getBrandById(@PathVariable Long id) {
        return ResponseEntity.ok(carBrandService.getBrandById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo mới hãng xe (ADMIN)")
    public ResponseEntity<CarBrandResponse> createBrand(@Valid @RequestBody CreateCarBrandRequest request) {
        String actor = SecurityUtils.getCurrentUsername();
        String role = SecurityUtils.isAdmin() ? "ADMIN" : "USER";
        return ResponseEntity.status(HttpStatus.CREATED).body(carBrandService.createBrand(request, actor, role));
    }
}
