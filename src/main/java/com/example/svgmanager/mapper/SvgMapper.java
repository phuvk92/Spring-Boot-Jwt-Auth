package com.example.svgmanager.mapper;

import com.example.svgmanager.dto.response.SvgFileDealerPermissionResponse;
import com.example.svgmanager.dto.response.SvgResponse;
import com.example.svgmanager.dto.response.VehicleConfigurationResponse;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.SvgFileDealerPermission;
import com.example.svgmanager.entity.SvgFileVehicleConfiguration;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.service.CategoryService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class SvgMapper {

    private final UserMapper userMapper;
    private final CategoryService categoryService;
    private final VehicleConfigurationMapper vehicleConfigurationMapper;

    public SvgMapper(UserMapper userMapper, CategoryService categoryService, VehicleConfigurationMapper vehicleConfigurationMapper) {
        this.userMapper = userMapper;
        this.categoryService = categoryService;
        this.vehicleConfigurationMapper = vehicleConfigurationMapper;
    }

    public SvgResponse toSvgResponse(SvgFile svgFile) {
        return toSvgResponse(svgFile, null, true);
    }

    public SvgResponse toSvgResponse(SvgFile svgFile, User currentUser, boolean isAdmin) {
        if (svgFile == null) {
            return null;
        }

        Long agentId = svgFile.getAgent() != null ? svgFile.getAgent().getId() : null;

        // Map vehicle configurations
        List<VehicleConfigurationResponse> configs = new ArrayList<>();
        if (svgFile.getVehicleConfigurations() != null) {
            for (SvgFileVehicleConfiguration svc : svgFile.getVehicleConfigurations()) {
                if (svc.getVehicleConfiguration() != null && !svc.getVehicleConfiguration().isDeleted()) {
                    configs.add(vehicleConfigurationMapper.toResponse(svc.getVehicleConfiguration()));
                }
            }
        }
        // Fallback for legacy single vehicleConfiguration
        if (configs.isEmpty() && svgFile.getVehicleConfiguration() != null && !svgFile.getVehicleConfiguration().isDeleted()) {
            configs.add(vehicleConfigurationMapper.toResponse(svgFile.getVehicleConfiguration()));
        }

        // Map dealer permissions
        int dealerPermissionCount = 0;
        List<SvgFileDealerPermissionResponse> dealerResponses = null;

        boolean canView = isAdmin;
        boolean canDownload = isAdmin;

        if (svgFile.getDealerPermissions() != null) {
            for (SvgFileDealerPermission dp : svgFile.getDealerPermissions()) {
                if (dp.isCanView() && dp.getDealer() != null && !dp.getDealer().isDeleted()) {
                    dealerPermissionCount++;
                }
            }

            // Only expose full dealer permissions list to ADMIN
            if (isAdmin) {
                dealerResponses = new ArrayList<>();
                for (SvgFileDealerPermission dp : svgFile.getDealerPermissions()) {
                    if (dp.getDealer() != null && !dp.getDealer().isDeleted()) {
                        dealerResponses.add(new SvgFileDealerPermissionResponse(
                                dp.getId(),
                                dp.getDealer().getId(),
                                dp.getDealer().getCode(),
                                dp.getDealer().getName(),
                                dp.isCanView(),
                                dp.isCanDownload(),
                                dp.getCreatedAt(),
                                dp.getUpdatedAt()
                        ));
                    }
                }
            } else if (currentUser != null && currentUser.getDealer() != null) {
                Long userDealerId = currentUser.getDealer().getId();
                Optional<SvgFileDealerPermission> permOpt = svgFile.getDealerPermissions().stream()
                        .filter(dp -> dp.getDealer() != null && dp.getDealer().getId().equals(userDealerId))
                        .findFirst();
                if (permOpt.isPresent()) {
                    canView = permOpt.get().isCanView();
                    canDownload = permOpt.get().isCanDownload();
                } else {
                    canView = false;
                    canDownload = false;
                }
            }
        }

        return SvgResponse.builder()
                .id(svgFile.getId())
                .originalFilename(svgFile.getOriginalFilename())
                .storedFilename(svgFile.getStoredFilename())
                .fileSize(svgFile.getFileSize())
                .contentType(svgFile.getContentType())
                .checksum(svgFile.getChecksum())
                .status(svgFile.getStatus())
                .category(svgFile.getCategory() != null ? categoryService.toSummary(svgFile.getCategory()) : null)
                .vehicleConfigurations(configs)
                .dealerPermissionCount(dealerPermissionCount)
                .dealerPermissions(dealerResponses)
                .canView(canView)
                .canDownload(canDownload)
                .uploadedBy(userMapper.toUserSummaryResponse(svgFile.getUploadedBy()))
                .agentId(agentId)
                .createdAt(svgFile.getCreatedAt())
                .updatedAt(svgFile.getUpdatedAt())
                .build();
    }
}
