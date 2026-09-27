package com.example.svgmanager.service;

import com.example.svgmanager.dto.request.SvgFileDealerPermissionRequest;
import com.example.svgmanager.dto.response.BatchSvgUploadResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.SvgFileDealerPermissionResponse;
import com.example.svgmanager.dto.response.SvgResponse;
import com.example.svgmanager.dto.response.VehicleConfigurationResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SvgService {

    SvgResponse uploadSvg(MultipartFile file, Long categoryId);

    BatchSvgUploadResponse batchUploadSvg(
            List<MultipartFile> files,
            List<Long> vehicleConfigurationIds,
            List<SvgFileDealerPermissionRequest> dealerPermissions
    );

    PageResponse<SvgResponse> getSvgFiles(
            String keyword,
            String productGroup,
            Long brandId,
            Long modelId,
            Integer year,
            String generationCode,
            Long dealerId,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    SvgResponse getSvgFileById(Long id);

    SvgResponse updateSvg(Long id, String status);

    List<SvgFileDealerPermissionResponse> getDealerPermissions(Long svgFileId);

    List<SvgFileDealerPermissionResponse> updateDealerPermissions(Long svgFileId, List<SvgFileDealerPermissionRequest> requests);

    List<VehicleConfigurationResponse> getVehicleConfigurations(Long svgFileId);

    List<VehicleConfigurationResponse> updateVehicleConfigurations(Long svgFileId, List<Long> vehicleConfigurationIds);

    Resource previewSvg(Long id);

    Resource downloadSvg(Long id);

    String getOriginalFilename(Long id);

    void deleteSvg(Long id);
}
