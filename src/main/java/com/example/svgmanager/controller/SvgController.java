package com.example.svgmanager.controller;

import com.example.svgmanager.dto.request.AssignDealersRequest;
import com.example.svgmanager.dto.request.AssignVehicleConfigurationsRequest;
import com.example.svgmanager.dto.request.SvgFileDealerPermissionRequest;
import com.example.svgmanager.dto.response.BatchSvgUploadResponse;
import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.SvgFileDealerPermissionResponse;
import com.example.svgmanager.dto.response.SvgResponse;
import com.example.svgmanager.dto.response.VehicleConfigurationResponse;
import com.example.svgmanager.service.SvgService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/svg")
@Tag(name = "SVG Patterns & Part Files", description = "Endpoints for managing SVG pattern and part files, batch upload, and dealer permissions")
@SecurityRequirement(name = "Bearer Authentication")
public class SvgController {

    private final SvgService svgService;
    private final ObjectMapper objectMapper;

    public SvgController(SvgService svgService, ObjectMapper objectMapper) {
        this.svgService = svgService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = {"", "/upload"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Upload single SVG file (legacy)", description = "Uploads a new SVG file. ADMIN or AGENT.")
    public ResponseEntity<SvgResponse> uploadSvg(
            @Parameter(description = "SVG file to upload", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Category ID from catalog (optional)")
            @RequestParam(value = "categoryId", required = false) Long categoryId
    ) {
        SvgResponse response = svgService.uploadSvg(file, categoryId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Batch upload SVG files (up to 10 files)", description = "Uploads up to 10 SVG files with optional vehicle configuration and dealer permissions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "SVG files uploaded successfully",
                    content = @Content(schema = @Schema(implementation = BatchSvgUploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid files or configuration",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BatchSvgUploadResponse> batchUploadSvg(
            @Parameter(description = "List of SVG files (1 to 10 files)", required = true)
            @RequestPart("files") List<MultipartFile> files,

            @Parameter(description = "List of vehicle configuration IDs to assign to all files")
            @RequestParam(value = "vehicleConfigurationIds", required = false) List<Long> vehicleConfigurationIds,

            @Parameter(description = "JSON list of dealer permissions: [{\"dealerId\":1,\"canView\":true,\"canDownload\":false}]")
            @RequestParam(value = "dealerPermissions", required = false) String dealerPermissionsJson
    ) {
        List<SvgFileDealerPermissionRequest> dealerPermissions = Collections.emptyList();
        if (dealerPermissionsJson != null && !dealerPermissionsJson.isBlank()) {
            try {
                dealerPermissions = objectMapper.readValue(dealerPermissionsJson, new TypeReference<>() {});
            } catch (Exception e) {
                throw new com.example.svgmanager.exception.BadRequestException("dealerPermissions định dạng JSON không hợp lệ: " + e.getMessage());
            }
        }

        BatchSvgUploadResponse response = svgService.batchUploadSvg(files, vehicleConfigurationIds, dealerPermissions);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "List SVG files with pagination and multi-criteria filters", description = "Returns paginated list of SVG files scoped by role and dealer permission.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SVG files retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<SvgResponse>> getSvgFiles(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "productGroup", required = false) String productGroup,
            @RequestParam(value = "brandId", required = false) Long brandId,
            @RequestParam(value = "modelId", required = false) Long modelId,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "generationCode", required = false) String generationCode,
            @RequestParam(value = "dealerId", required = false) Long dealerId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "DESC") String sortDirection
    ) {
        PageResponse<SvgResponse> response = svgService.getSvgFiles(
                keyword, productGroup, brandId, modelId, year, generationCode, dealerId, status, page, size, sortBy, sortDirection
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Get SVG file metadata by ID", description = "Retrieves SVG file details.")
    public ResponseEntity<SvgResponse> getSvgById(@PathVariable Long id) {
        SvgResponse response = svgService.getSvgFileById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update SVG metadata", description = "Updates status for an SVG file. ADMIN only.")
    public ResponseEntity<SvgResponse> updateSvg(
            @PathVariable Long id,
            @Parameter(description = "New Status") @RequestParam("status") String status
    ) {
        SvgResponse response = svgService.updateSvg(id, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/dealers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get dealer permissions for SVG file", description = "Returns list of dealer permissions. ADMIN only.")
    public ResponseEntity<List<SvgFileDealerPermissionResponse>> getDealerPermissions(@PathVariable Long id) {
        List<SvgFileDealerPermissionResponse> response = svgService.getDealerPermissions(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/dealers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update dealer permissions for SVG file", description = "Replaces dealer permissions for an SVG file. ADMIN only.")
    public ResponseEntity<List<SvgFileDealerPermissionResponse>> updateDealerPermissions(
            @PathVariable Long id,
            @Valid @RequestBody AssignDealersRequest request
    ) {
        List<SvgFileDealerPermissionResponse> response = svgService.updateDealerPermissions(id, request.dealers());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/vehicle-configurations")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @Operation(summary = "Get assigned vehicle configurations for SVG file", description = "Returns assigned vehicle configurations.")
    public ResponseEntity<List<VehicleConfigurationResponse>> getVehicleConfigurations(@PathVariable Long id) {
        List<VehicleConfigurationResponse> response = svgService.getVehicleConfigurations(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/vehicle-configurations")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update assigned vehicle configurations for SVG file", description = "Replaces assigned vehicle configurations. ADMIN only.")
    public ResponseEntity<List<VehicleConfigurationResponse>> updateVehicleConfigurations(
            @PathVariable Long id,
            @Valid @RequestBody AssignVehicleConfigurationsRequest request
    ) {
        List<VehicleConfigurationResponse> response = svgService.updateVehicleConfigurations(id, request.vehicleConfigurationIds());
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = {"/{id}/preview", "/{id}/content"})
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Preview SVG file content", description = "Streams SVG content for browser inline preview.")
    public ResponseEntity<Resource> previewSvg(@PathVariable Long id) {
        Resource resource = svgService.previewSvg(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/svg+xml"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Download SVG file", description = "Downloads SVG file as attachment with dealer permission check.")
    public ResponseEntity<Resource> downloadSvg(@PathVariable Long id) {
        Resource resource = svgService.downloadSvg(id);
        String originalFilename = svgService.getOriginalFilename(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFilename + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Delete SVG file", description = "Deletes SVG record and underlying file from disk. ADMIN or AGENT owning file.")
    public ResponseEntity<Void> deleteSvg(@PathVariable Long id) {
        svgService.deleteSvg(id);
        return ResponseEntity.noContent().build();
    }
}
