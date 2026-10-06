package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.UserSavedFileResponse;
import com.example.svgmanager.entity.*;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.repository.DealerRepository;
import com.example.svgmanager.repository.FileCategoryRepository;
import com.example.svgmanager.repository.UserSvgFileRepository;
import com.example.svgmanager.repository.UserSvgFileSpecification;
import com.example.svgmanager.repository.VehicleNodeRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.FileStorageService;
import com.example.svgmanager.service.SvgSanitizerService;
import com.example.svgmanager.service.UserSvgFileService;
import com.example.svgmanager.service.UserSvgFileShareService;
import com.example.svgmanager.util.ChecksumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserSvgFileServiceImpl implements UserSvgFileService {

    private static final Logger log = LoggerFactory.getLogger(UserSvgFileServiceImpl.class);

    private final UserSvgFileRepository userSvgFileRepository;
    private final FileCategoryRepository fileCategoryRepository;
    private final VehicleNodeRepository vehicleNodeRepository;
    private final SvgFileRepository svgFileRepository;
    private final FileStorageService fileStorageService;
    private final SvgSanitizerService svgSanitizerService;
    private final AuditLogService auditLogService;
    private final UserSvgFileShareService userSvgFileShareService;
    private final DealerRepository dealerRepository;

    public UserSvgFileServiceImpl(UserSvgFileRepository userSvgFileRepository,
                                  FileCategoryRepository fileCategoryRepository,
                                  VehicleNodeRepository vehicleNodeRepository,
                                  SvgFileRepository svgFileRepository,
                                  FileStorageService fileStorageService,
                                  SvgSanitizerService svgSanitizerService,
                                  AuditLogService auditLogService,
                                  @Lazy UserSvgFileShareService userSvgFileShareService,
                                  DealerRepository dealerRepository) {
        this.userSvgFileRepository = userSvgFileRepository;
        this.fileCategoryRepository = fileCategoryRepository;
        this.vehicleNodeRepository = vehicleNodeRepository;
        this.svgFileRepository = svgFileRepository;
        this.fileStorageService = fileStorageService;
        this.svgSanitizerService = svgSanitizerService;
        this.auditLogService = auditLogService;
        this.userSvgFileShareService = userSvgFileShareService;
        this.dealerRepository = dealerRepository;
    }

    @Override
    @Transactional
    public UserSavedFileResponse saveUserFile(
            User currentUser,
            byte[] fileBytes,
            String originalFilename,
            String customFileName,
            Long categoryId,
            Long vehicleNodeId,
            String brandName,
            String modelName,
            Integer yearFrom,
            Integer yearTo,
            String generationCode,
            String productGroup,
            String productGroupName,
            Double filmWidth,
            String filmWidthUnit,
            Double rollLength,
            String rollLengthUnit,
            Double axisX,
            Double axisY,
            String sourceFileKey,
            String description
    ) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new BadRequestException("Nội dung file SVG không được để trống", ErrorCodes.FILE_REQUIRED);
        }

        // 1. Sanitize & validate SVG
        byte[] sanitizedBytes = svgSanitizerService.sanitizeAndValidateSvg(fileBytes);
        String checksum = "sha256:" + ChecksumUtils.calculateSha256(sanitizedBytes);

        // 2. Store file physically
        String storedFilename = UUID.randomUUID().toString() + ".svg";
        String filePath = fileStorageService.storeFile(sanitizedBytes, storedFilename);

        // 3. Resolve display filename
        String finalOriginal = StringUtils.hasText(originalFilename) ? originalFilename.trim() : "file.svg";
        String finalDisplay = StringUtils.hasText(customFileName) ? customFileName.trim() : finalOriginal;
        if (!finalDisplay.toLowerCase().endsWith(".svg")) {
            finalDisplay += ".svg";
        }

        // 4. Resolve category
        FileCategory category = null;
        if (categoryId != null) {
            category = fileCategoryRepository.findById(categoryId).orElse(null);
        }

        // 5. Resolve vehicle node & hierarchy
        VehicleNode vehicleNode = null;
        String resolvedBrand = brandName;
        String resolvedModel = modelName;
        if (vehicleNodeId != null) {
            vehicleNode = vehicleNodeRepository.findById(vehicleNodeId).orElse(null);
            if (vehicleNode != null) {
                if (!StringUtils.hasText(resolvedModel)) {
                    resolvedModel = resolveModelName(vehicleNode);
                }
                if (!StringUtils.hasText(resolvedBrand)) {
                    resolvedBrand = resolveBrandName(vehicleNode);
                }
            }
        }

        // 6. Build entity
        UserSvgFile entity = UserSvgFile.builder()
                .fileName(finalDisplay)
                .originalFileName(finalOriginal)
                .storedFileName(storedFilename)
                .filePath(filePath)
                .fileSize((long) sanitizedBytes.length)
                .mimeType("image/svg+xml")
                .checksum(checksum)
                .category(category)
                .vehicleNode(vehicleNode)
                .productGroup(productGroup)
                .productGroupName(productGroupName)
                .brandName(resolvedBrand)
                .modelName(resolvedModel)
                .yearFrom(yearFrom)
                .yearTo(yearTo)
                .generationCode(generationCode)
                .filmWidth(filmWidth != null ? filmWidth : axisY)
                .filmWidthUnit(StringUtils.hasText(filmWidthUnit) ? filmWidthUnit : "MM")
                .rollLength(rollLength != null ? rollLength : axisX)
                .rollLengthUnit(StringUtils.hasText(rollLengthUnit) ? rollLengthUnit : "MM")
                .axisX(axisX != null ? axisX : rollLength)
                .axisY(axisY != null ? axisY : filmWidth)
                .sourceFileKey(StringUtils.hasText(sourceFileKey) ? sourceFileKey.trim() : null)
                .description(description)
                .status("ACTIVE")
                .user(currentUser)
                .dealer(currentUser != null && currentUser.getDealer() != null ? dealerRepository.findById(currentUser.getDealer().getId()).orElse(null) : null)
                .build();

        UserSvgFile saved = userSvgFileRepository.save(entity);
        log.info("[USER_SAVED_SVG_CREATED] id={}, fileName='{}', userId={}",
                saved.getId(), saved.getFileName(), currentUser != null ? currentUser.getId() : null);

        if (currentUser != null) {
            auditLogService.log(
                    currentUser.getUsername(),
                    currentUser.getRole() != null ? currentUser.getRole().name() : "USER",
                    "USER_SVG_CREATE",
                    "UserSvgFile",
                    saved.getId(),
                    "Tạo bản lưu SVG: " + saved.getFileName()
            );
        }

        return toResponse(saved, currentUser);
    }

    @Override
    @Transactional
    public UserSavedFileResponse updateUserFile(
            User currentUser,
            Long id,
            byte[] fileBytes,
            String originalFilename,
            String customFileName,
            Long categoryId,
            Long vehicleNodeId,
            String brandName,
            String modelName,
            Integer yearFrom,
            Integer yearTo,
            String generationCode,
            String productGroup,
            String productGroupName,
            Double filmWidth,
            String filmWidthUnit,
            Double rollLength,
            String rollLengthUnit,
            Double axisX,
            Double axisY,
            String sourceFileKey,
            String description
    ) {
        if (currentUser == null) {
            throw new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND);
        }

        UserSvgFile existing = userSvgFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND));

        if ("DELETED".equalsIgnoreCase(existing.getStatus())) {
            throw new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND);
        }

        // Section 14: USER không được sửa file được share. Chỉ Owner mới được sửa.
        if (!existing.getUser().getId().equals(currentUser.getId())) {
            if (userSvgFileShareService.canAccess(existing, currentUser)) {
                throw new ForbiddenException("Chỉ người sở hữu file mới có quyền chỉnh sửa file này", ErrorCodes.USER_FILE_SHARE_FORBIDDEN);
            }
            throw new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND);
        }

        if (fileBytes == null || fileBytes.length == 0) {
            throw new BadRequestException("Nội dung file SVG không được để trống", ErrorCodes.FILE_REQUIRED);
        }

        // 1. Sanitize & validate SVG
        byte[] sanitizedBytes = svgSanitizerService.sanitizeAndValidateSvg(fileBytes);
        String checksum = "sha256:" + ChecksumUtils.calculateSha256(sanitizedBytes);

        // 2. Store new file physically & delete old file
        String oldFilePath = existing.getFilePath();
        String storedFilename = UUID.randomUUID().toString() + ".svg";
        String filePath = fileStorageService.storeFile(sanitizedBytes, storedFilename);

        if (StringUtils.hasText(oldFilePath)) {
            try {
                fileStorageService.deleteFile(oldFilePath);
            } catch (Exception e) {
                log.warn("[USER_SAVED_SVG_CLEANUP_WARN] Không thể xoá file cũ {}: {}", oldFilePath, e.getMessage());
            }
        }

        // 3. Update file content & metadata
        existing.setFilePath(filePath);
        existing.setStoredFileName(storedFilename);
        existing.setFileSize((long) sanitizedBytes.length);
        existing.setChecksum(checksum);
        existing.setMimeType("image/svg+xml");

        // 4. Update display filename / original filename if provided
        if (StringUtils.hasText(originalFilename)) {
            existing.setOriginalFileName(originalFilename.trim());
        }
        if (StringUtils.hasText(customFileName)) {
            String finalDisplay = customFileName.trim();
            if (!finalDisplay.toLowerCase().endsWith(".svg")) {
                finalDisplay += ".svg";
            }
            existing.setFileName(finalDisplay);
        } else if (StringUtils.hasText(originalFilename) && !StringUtils.hasText(existing.getFileName())) {
            String finalDisplay = originalFilename.trim();
            if (!finalDisplay.toLowerCase().endsWith(".svg")) {
                finalDisplay += ".svg";
            }
            existing.setFileName(finalDisplay);
        }

        // 5. Update category if provided
        if (categoryId != null) {
            FileCategory category = fileCategoryRepository.findById(categoryId).orElse(null);
            existing.setCategory(category);
        }

        // 6. Update vehicle node & hierarchy if provided
        if (vehicleNodeId != null) {
            VehicleNode vehicleNode = vehicleNodeRepository.findById(vehicleNodeId).orElse(null);
            existing.setVehicleNode(vehicleNode);
            if (vehicleNode != null) {
                if (!StringUtils.hasText(brandName)) {
                    brandName = resolveBrandName(vehicleNode);
                }
                if (!StringUtils.hasText(modelName)) {
                    modelName = resolveModelName(vehicleNode);
                }
            }
        }

        if (StringUtils.hasText(brandName)) {
            existing.setBrandName(brandName);
        }
        if (StringUtils.hasText(modelName)) {
            existing.setModelName(modelName);
        }
        if (yearFrom != null) {
            existing.setYearFrom(yearFrom);
        }
        if (yearTo != null) {
            existing.setYearTo(yearTo);
        }
        if (StringUtils.hasText(generationCode)) {
            existing.setGenerationCode(generationCode);
        }
        if (StringUtils.hasText(productGroup)) {
            existing.setProductGroup(productGroup);
        }
        if (StringUtils.hasText(productGroupName)) {
            existing.setProductGroupName(productGroupName);
        }

        // 7. Update dimensions if provided
        if (filmWidth != null) {
            existing.setFilmWidth(filmWidth);
        }
        if (StringUtils.hasText(filmWidthUnit)) {
            existing.setFilmWidthUnit(filmWidthUnit);
        }
        if (rollLength != null) {
            existing.setRollLength(rollLength);
        }
        if (StringUtils.hasText(rollLengthUnit)) {
            existing.setRollLengthUnit(rollLengthUnit);
        }
        if (axisX != null) {
            existing.setAxisX(axisX);
        }
        if (axisY != null) {
            existing.setAxisY(axisY);
        }

        // 8. Update sourceFileKey if provided
        if (StringUtils.hasText(sourceFileKey)) {
            existing.setSourceFileKey(sourceFileKey.trim());
        }

        // 9. Update description if provided
        if (description != null) {
            existing.setDescription(description);
        }

        existing.setUpdatedAt(LocalDateTime.now());
        UserSvgFile updated = userSvgFileRepository.save(existing);

        log.info("[USER_SAVED_SVG_UPDATED] id={}, fileName='{}', userId={}",
                updated.getId(), updated.getFileName(), currentUser.getId());

        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole() != null ? currentUser.getRole().name() : "USER",
                "USER_SVG_UPDATE",
                "UserSvgFile",
                updated.getId(),
                "Cập nhật bản lưu SVG: " + updated.getFileName()
        );

        return toResponse(updated, currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSavedFileResponse> listAdminFiles(
            String keyword,
            Long categoryId,
            Long vehicleNodeId,
            Long brandId,
            Long modelId,
            Long dealerId,
            Long userId,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            String status,
            Pageable pageable
    ) {
        Specification<UserSvgFile> spec = UserSvgFileSpecification.filter(
                keyword, categoryId, vehicleNodeId, brandId, modelId, dealerId, userId, createdFrom, createdTo, status
        );

        Page<UserSvgFile> page = userSvgFileRepository.findAll(spec, pageable);
        return PageResponse.<UserSavedFileResponse>builder()
                .content(page.getContent().stream().map(f -> toResponse(f, null)).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSavedFileResponse> listUserFiles(
            User currentUser,
            String keyword,
            Long categoryId,
            String status,
            Pageable pageable
    ) {
        // Section 13: Lấy cả file sở hữu và file được share ACTIVE
        Specification<UserSvgFile> spec = UserSvgFileSpecification.filterAccessibleByUser(
                currentUser, keyword, categoryId, status
        );

        Page<UserSvgFile> page = userSvgFileRepository.findAll(spec, pageable);
        return PageResponse.<UserSavedFileResponse>builder()
                .content(page.getContent().stream().map(f -> toResponse(f, currentUser)).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserSavedFileResponse getAdminFile(Long id) {
        UserSvgFile file = userSvgFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND));
        return toResponse(file, null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSavedFileResponse getUserFile(User currentUser, Long id) {
        if (currentUser == null) {
            throw new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND);
        }
        UserSvgFile file = userSvgFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND));
        if ("DELETED".equalsIgnoreCase(file.getStatus())) {
            throw new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND);
        }
        if (!userSvgFileShareService.canAccess(file, currentUser)) {
            throw new ForbiddenException("Bạn không có quyền truy cập file này", ErrorCodes.USER_FILE_SHARE_FORBIDDEN);
        }
        return toResponse(file, currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadAdminFile(Long id) {
        UserSvgFile file = userSvgFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND));
        return fileStorageService.loadFileAsResource(file.getFilePath());
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadUserFile(User currentUser, Long id) {
        if (currentUser == null) {
            throw new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND);
        }
        UserSvgFile file = userSvgFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND));
        if ("DELETED".equalsIgnoreCase(file.getStatus())) {
            throw new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND);
        }
        if (!userSvgFileShareService.canAccess(file, currentUser)) {
            throw new ForbiddenException("Bạn không có quyền tải file này", ErrorCodes.USER_FILE_SHARE_FORBIDDEN);
        }
        return fileStorageService.loadFileAsResource(file.getFilePath());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getUserFileBytes(User currentUser, Long id) {
        if (currentUser == null) {
            throw new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND);
        }
        UserSvgFile file = userSvgFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND));
        if ("DELETED".equalsIgnoreCase(file.getStatus())) {
            throw new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND);
        }
        if (!userSvgFileShareService.canAccess(file, currentUser)) {
            throw new ForbiddenException("Bạn không có quyền xem trước file này", ErrorCodes.USER_FILE_SHARE_FORBIDDEN);
        }
        return fileStorageService.loadFileAsBytes(file.getFilePath());
    }

    @Override
    @Transactional
    public void deleteUserFile(User currentUser, Long id) {
        if (currentUser == null) {
            throw new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND);
        }
        UserSvgFile file = userSvgFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND));

        if ("DELETED".equalsIgnoreCase(file.getStatus())) {
            throw new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND);
        }

        // Section 14: USER không được xoá file được share. Chỉ Owner mới được xoá.
        if (!file.getUser().getId().equals(currentUser.getId())) {
            if (userSvgFileShareService.canAccess(file, currentUser)) {
                throw new ForbiddenException("Chỉ người sở hữu file mới có quyền xoá file này", ErrorCodes.USER_FILE_SHARE_FORBIDDEN);
            }
            throw new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND);
        }

        // Xoá mềm: cập nhật status = DELETED, file vật lý trên đĩa giữ nguyên
        file.setStatus("DELETED");
        file.setUpdatedAt(LocalDateTime.now());
        userSvgFileRepository.save(file);

        log.info("[USER_SAVED_SVG_DELETED] id={}, fileName='{}', userId={}",
                file.getId(), file.getFileName(), currentUser.getId());

        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole() != null ? currentUser.getRole().name() : "USER",
                "USER_SVG_DELETE",
                "UserSvgFile",
                file.getId(),
                "Xoá mềm bản lưu SVG: " + file.getFileName()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public String getOriginalFilename(Long id) {
        return userSvgFileRepository.findById(id)
                .map(UserSvgFile::getOriginalFileName)
                .orElse("file.svg");
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getAdminFileBytes(Long id) {
        UserSvgFile file = userSvgFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND));
        return fileStorageService.loadFileAsBytes(file.getFilePath());
    }

    private UserSavedFileResponse toResponse(UserSvgFile entity, User currentUser) {
        UserSavedFileResponse resp = new UserSavedFileResponse();
        resp.setId(entity.getId());
        resp.setFileName(entity.getFileName());
        resp.setOriginalFileName(entity.getOriginalFileName());
        resp.setDescription(entity.getDescription());
        resp.setFileSize(entity.getFileSize());
        resp.setMimeType(entity.getMimeType());
        resp.setChecksum(entity.getChecksum());
        resp.setStatus(entity.getStatus());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());

        // Category
        if (entity.getCategory() != null) {
            resp.setCategory(new UserSavedFileResponse.CategoryDto(
                    entity.getCategory().getId(),
                    entity.getCategory().getName()
            ));
        }

        // Vehicle Configuration
        boolean hasVehicle = entity.getVehicleNode() != null
                || StringUtils.hasText(entity.getBrandName())
                || StringUtils.hasText(entity.getModelName())
                || StringUtils.hasText(entity.getProductGroup())
                || entity.getYearFrom() != null;

        if (hasVehicle) {
            Long vId = entity.getVehicleNode() != null ? entity.getVehicleNode().getId() : null;
            resp.setVehicleConfiguration(new UserSavedFileResponse.VehicleConfigurationDto(
                    vId,
                    entity.getProductGroup(),
                    entity.getProductGroupName(),
                    entity.getBrandName(),
                    entity.getModelName(),
                    entity.getYearFrom(),
                    entity.getYearTo(),
                    entity.getGenerationCode()
            ));
        }

        // Cut Size
        Double width = entity.getFilmWidth() != null ? entity.getFilmWidth() : entity.getAxisY();
        Double length = entity.getRollLength() != null ? entity.getRollLength() : entity.getAxisX();
        if (width != null || length != null || entity.getAxisX() != null || entity.getAxisY() != null) {
            resp.setCutSize(new UserSavedFileResponse.CutSizeDto(
                    width,
                    entity.getFilmWidthUnit(),
                    length,
                    entity.getRollLengthUnit(),
                    entity.getAxisX() != null ? entity.getAxisX() : length,
                    entity.getAxisY() != null ? entity.getAxisY() : width
            ));
        }

        // User / Creator
        if (entity.getUser() != null) {
            String displayName = StringUtils.hasText(entity.getUser().getFullName())
                    ? entity.getUser().getFullName()
                    : entity.getUser().getUsername();
            resp.setCreatedBy(new UserSavedFileResponse.UserSummaryDto(
                    entity.getUser().getId(),
                    entity.getUser().getUsername(),
                    displayName
            ));
        }

        // Dealer
        if (entity.getDealer() != null) {
            String dealerName = null;
            try {
                dealerName = entity.getDealer().getName();
            } catch (Exception ex) {
                dealerName = dealerRepository.findById(entity.getDealer().getId())
                        .map(Dealer::getName)
                        .orElse(null);
            }
            resp.setDealer(new UserSavedFileResponse.DealerSummaryDto(
                    entity.getDealer().getId(),
                    dealerName
            ));
        }

        // Source file key and name
        if (StringUtils.hasText(entity.getSourceFileKey())) {
            resp.setSourceFileKey(entity.getSourceFileKey());
            svgFileRepository.findByFileKey(entity.getSourceFileKey())
                    .ifPresent(sf -> {
                        String name = StringUtils.hasText(sf.getDisplayName())
                                ? sf.getDisplayName()
                                : sf.getOriginalFilename();
                        resp.setSourceFileName(name);
                    });
        }

        // Access Type (OWNER vs SHARED)
        if (currentUser != null && entity.getUser() != null) {
            resp.setAccessType(entity.getUser().getId().equals(currentUser.getId()) ? "OWNER" : "SHARED");
        } else {
            resp.setAccessType("OWNER");
        }

        return resp;
    }

    private String resolveBrandName(VehicleNode node) {
        VehicleNode curr = node;
        while (curr != null) {
            if (curr.getLevel() == VehicleNodeLevel.BRAND) {
                return curr.getName();
            }
            curr = curr.getParent();
        }
        return null;
    }

    private String resolveModelName(VehicleNode node) {
        if (node.getLevel() == VehicleNodeLevel.MODEL) {
            return node.getName();
        }
        if (node.getParent() != null && node.getParent().getLevel() == VehicleNodeLevel.MODEL) {
            return node.getParent().getName();
        }
        return null;
    }
}
