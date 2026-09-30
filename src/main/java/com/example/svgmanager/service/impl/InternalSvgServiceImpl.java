package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.internal.InternalSvgDetailResponse;
import com.example.svgmanager.dto.internal.InternalSvgPermissionResponse;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.SvgFileVehicleNode;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.VehicleNode;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.SvgFileVehicleNodeRepository;
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

/**
 * Data Center v2 (chốt 29/09, Q6): bỏ phân quyền đại lý theo file — phiên hợp lệ
 * là xem/tải được mọi file còn hiệu lực. Chỉ còn kiểm phiên (thiết bị kiểm ở
 * DeviceSessionFilter, ngoài service này).
 */
@Service
public class InternalSvgServiceImpl implements InternalSvgService {

    private static final Logger log = LoggerFactory.getLogger(InternalSvgServiceImpl.class);

    private final SvgFileRepository svgFileRepository;
    private final SvgFileVehicleNodeRepository vehicleNodeLinkRepository;
    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public InternalSvgServiceImpl(
            SvgFileRepository svgFileRepository,
            SvgFileVehicleNodeRepository vehicleNodeLinkRepository,
            FileStorageService fileStorageService,
            CurrentUserService currentUserService,
            AuditLogService auditLogService
    ) {
        this.svgFileRepository = svgFileRepository;
        this.vehicleNodeLinkRepository = vehicleNodeLinkRepository;
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
        SvgFile svgFile = findAuthorizedSvg(svgFileId);

        List<String> vehicles = new ArrayList<>();
        for (SvgFileVehicleNode link : vehicleNodeLinkRepository.findBySvgFileId(svgFileId)) {
            vehicles.add(nodePath(link.getVehicleNode()));
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
                vehicles,
                new InternalSvgPermissionResponse(true, true),
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
        SvgFile svgFile = findAuthorizedSvg(svgFileId);

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
        return findAuthorizedSvg(svgFileId).getOriginalFilename();
    }

    private SvgFile findAuthorizedSvg(Long svgFileId) {
        SvgFile svgFile = svgFileRepository.findById(svgFileId)
                .orElseThrow(() -> new ResourceNotFoundException("SVG file not found with id: " + svgFileId));

        if ("DELETED".equalsIgnoreCase(svgFile.getStatus())) {
            throw new ResourceNotFoundException("SVG file not found with id: " + svgFileId);
        }

        return svgFile;
    }

    /** "Toyota › Camry › Camry 2.5Q" — đi ngược lên gốc rồi ghép xuôi. */
    private String nodePath(VehicleNode node) {
        List<String> names = new ArrayList<>();
        for (VehicleNode n = node; n != null; n = n.getParent()) {
            names.add(0, n.getName());
        }
        return String.join(" › ", names);
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
