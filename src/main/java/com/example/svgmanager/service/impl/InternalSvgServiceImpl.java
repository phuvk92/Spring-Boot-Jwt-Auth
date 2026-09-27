package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.internal.InternalSvgDetailResponse;
import com.example.svgmanager.dto.internal.InternalSvgPermissionResponse;
import com.example.svgmanager.dto.internal.InternalVehicleConfigurationResponse;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.SvgFileDealerPermission;
import com.example.svgmanager.entity.SvgFileVehicleConfiguration;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.VehicleConfiguration;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.repository.SvgFileDealerPermissionRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.SvgFileVehicleConfigurationRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.FileStorageService;
import com.example.svgmanager.service.InternalSvgService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class InternalSvgServiceImpl implements InternalSvgService {

    private static final Logger log = LoggerFactory.getLogger(InternalSvgServiceImpl.class);

    private final SvgFileRepository svgFileRepository;
    private final SvgFileDealerPermissionRepository dealerPermissionRepository;
    private final SvgFileVehicleConfigurationRepository vehicleConfigRepository;
    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public InternalSvgServiceImpl(
            SvgFileRepository svgFileRepository,
            SvgFileDealerPermissionRepository dealerPermissionRepository,
            SvgFileVehicleConfigurationRepository vehicleConfigRepository,
            FileStorageService fileStorageService,
            CurrentUserService currentUserService,
            AuditLogService auditLogService
    ) {
        this.svgFileRepository = svgFileRepository;
        this.dealerPermissionRepository = dealerPermissionRepository;
        this.vehicleConfigRepository = vehicleConfigRepository;
        this.fileStorageService = fileStorageService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public InternalSvgDetailResponse getSvgDetail(Long svgFileId) {
        return getSvgDetail(svgFileId, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public InternalSvgDetailResponse getSvgDetail(Long svgFileId, String device, String ipAddress) {
        SvgFile svgFile = findAuthorizedSvg(svgFileId, false);

        boolean canView = true;
        boolean canDownload = true;

        if (!currentUserService.isAdmin()) {
            User user = currentUserService.getCurrentUser();
            Long dealerId = user.getDealer() != null ? user.getDealer().getId() : null;
            if (dealerId == null) {
                throw new ResourceNotFoundException("SVG file not found with id: " + svgFileId);
            }
            SvgFileDealerPermission permission = dealerPermissionRepository
                    .findBySvgFileIdAndDealerId(svgFileId, dealerId)
                    .orElseThrow(() -> new ResourceNotFoundException("SVG file not found with id: " + svgFileId));

            canView = permission.isCanView();
            canDownload = permission.isCanDownload();
        }

        // Map assigned vehicle configurations
        List<SvgFileVehicleConfiguration> assignments = vehicleConfigRepository.findBySvgFileId(svgFileId);
        List<InternalVehicleConfigurationResponse> configs = new ArrayList<>();
        for (SvgFileVehicleConfiguration assignment : assignments) {
            VehicleConfiguration vc = assignment.getVehicleConfiguration();
            if (vc != null && !vc.isDeleted()) {
                configs.add(new InternalVehicleConfigurationResponse(
                        vc.getId(),
                        vc.getProductGroup() != null ? vc.getProductGroup().name() : null,
                        vc.getProductGroup() != null ? vc.getProductGroup().getDisplayName() : null,
                        vc.getBrand() != null ? vc.getBrand().getName() : null,
                        vc.getModel() != null ? vc.getModel().getName() : null,
                        vc.getYearFrom(),
                        vc.getYearTo(),
                        vc.getGenerationCode()
                ));
            }
        }

        LocalDateTime updatedAt = svgFile.getUpdatedAt() != null ? svgFile.getUpdatedAt() : svgFile.getCreatedAt();
        String formattedDate = updatedAt != null
                ? ZonedDateTime.of(updatedAt, ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                : null;

        String clientInfo = formatClientInfo(device, ipAddress);
        User currentUser = currentUserService.getCurrentUser();
        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole().name(),
                "SVG_VIEW",
                "SvgFile",
                svgFileId,
                "Client máy cắt xem chi tiết file SVG: " + svgFile.getOriginalFilename() + clientInfo
        );

        return new InternalSvgDetailResponse(
                svgFile.getId(),
                svgFile.getOriginalFilename(),
                svgFile.getFileSize(),
                svgFile.getContentType() != null ? svgFile.getContentType() : "image/svg+xml",
                svgFile.getChecksum(),
                svgFile.getStatus() != null ? svgFile.getStatus() : "ACTIVE",
                configs,
                new InternalSvgPermissionResponse(canView, canDownload),
                formattedDate
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadSvg(Long svgFileId) {
        return downloadSvg(svgFileId, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadSvg(Long svgFileId, String device, String ipAddress) {
        SvgFile svgFile = findAuthorizedSvg(svgFileId, true);

        Resource resource = fileStorageService.loadFileAsResource(svgFile.getFilePath());
        if (resource == null || !resource.exists() || !resource.isReadable()) {
            log.error("[INTERNAL_SVG_DOWNLOAD_ERROR] Physical file missing for SVG ID {}: {}", svgFileId, svgFile.getFilePath());
            throw new ResourceNotFoundException("File not found on storage");
        }

        String clientInfo = formatClientInfo(device, ipAddress);
        User currentUser = currentUserService.getCurrentUser();
        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole().name(),
                "SVG_DOWNLOAD",
                "SvgFile",
                svgFileId,
                "Client máy cắt tải xuống file SVG: " + svgFile.getOriginalFilename() + clientInfo
        );

        log.info("[INTERNAL_SVG_DOWNLOADED] SVG file downloaded by {}: id={}, name='{}'{}",
                currentUser.getUsername(), svgFileId, svgFile.getOriginalFilename(), clientInfo);

        return resource;
    }

    @Override
    @Transactional(readOnly = true)
    public String getOriginalFilename(Long svgFileId) {
        return findAuthorizedSvg(svgFileId, false).getOriginalFilename();
    }

    private SvgFile findAuthorizedSvg(Long svgFileId, boolean checkDownload) {
        SvgFile svgFile = svgFileRepository.findById(svgFileId)
                .orElseThrow(() -> new ResourceNotFoundException("SVG file not found with id: " + svgFileId));

        if ("DELETED".equalsIgnoreCase(svgFile.getStatus())) {
            throw new ResourceNotFoundException("SVG file not found with id: " + svgFileId);
        }

        if (currentUserService.isAdmin()) {
            return svgFile;
        }

        User user = currentUserService.getCurrentUser();
        Long dealerId = user.getDealer() != null ? user.getDealer().getId() : null;

        if (dealerId == null) {
            log.warn("[INTERNAL_SVG_ACCESS_DENIED] User '{}' has no dealer assigned; rejecting SVG ID {}", user.getUsername(), svgFileId);
            throw new ResourceNotFoundException("SVG file not found with id: " + svgFileId);
        }

        SvgFileDealerPermission permission = dealerPermissionRepository
                .findBySvgFileIdAndDealerId(svgFileId, dealerId)
                .orElse(null);

        if (permission == null || !permission.isCanView()) {
            log.warn("[INTERNAL_SVG_ACCESS_DENIED] Dealer {} has no VIEW permission for SVG ID {}", dealerId, svgFileId);
            throw new ResourceNotFoundException("SVG file not found with id: " + svgFileId);
        }

        if (checkDownload && !permission.isCanDownload()) {
            log.warn("[INTERNAL_SVG_DOWNLOAD_FORBIDDEN] Dealer {} lacks DOWNLOAD permission for SVG ID {}", dealerId, svgFileId);
            throw new ForbiddenException("You do not have permission to download this file");
        }

        return svgFile;
    }

    private String formatClientInfo(String device, String ipAddress) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(device)) {
            sb.append("Thiết bị: ").append(device.trim());
        }
        if (StringUtils.hasText(ipAddress)) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("IP: ").append(ipAddress.trim());
        }
        return sb.length() > 0 ? " [" + sb + "]" : "";
    }
}
