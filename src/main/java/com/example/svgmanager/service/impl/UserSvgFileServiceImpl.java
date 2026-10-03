package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.UserSavedFileResponse;
import com.example.svgmanager.entity.*;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.repository.FileCategoryRepository;
import com.example.svgmanager.repository.UserSvgFileRepository;
import com.example.svgmanager.repository.UserSvgFileSpecification;
import com.example.svgmanager.repository.VehicleNodeRepository;
import com.example.svgmanager.service.FileStorageService;
import com.example.svgmanager.service.SvgSanitizerService;
import com.example.svgmanager.service.UserSvgFileService;
import com.example.svgmanager.util.ChecksumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final FileStorageService fileStorageService;
    private final SvgSanitizerService svgSanitizerService;

    public UserSvgFileServiceImpl(UserSvgFileRepository userSvgFileRepository,
                                  FileCategoryRepository fileCategoryRepository,
                                  VehicleNodeRepository vehicleNodeRepository,
                                  FileStorageService fileStorageService,
                                  SvgSanitizerService svgSanitizerService) {
        this.userSvgFileRepository = userSvgFileRepository;
        this.fileCategoryRepository = fileCategoryRepository;
        this.vehicleNodeRepository = vehicleNodeRepository;
        this.fileStorageService = fileStorageService;
        this.svgSanitizerService = svgSanitizerService;
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
                .description(description)
                .status("ACTIVE")
                .user(currentUser)
                .dealer(currentUser != null ? currentUser.getDealer() : null)
                .build();

        UserSvgFile saved = userSvgFileRepository.save(entity);
        log.info("[USER_SAVED_SVG_CREATED] id={}, fileName='{}', userId={}",
                saved.getId(), saved.getFileName(), currentUser != null ? currentUser.getId() : null);

        return toResponse(saved);
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
                .content(page.getContent().stream().map(this::toResponse).toList())
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
        Specification<UserSvgFile> spec = UserSvgFileSpecification.filter(
                keyword, categoryId, null, null, null, null, currentUser.getId(), null, null, status
        );

        Page<UserSvgFile> page = userSvgFileRepository.findAll(spec, pageable);
        return PageResponse.<UserSavedFileResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
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
        return toResponse(file);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSavedFileResponse getUserFile(User currentUser, Long id) {
        UserSvgFile file = userSvgFileRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND));
        return toResponse(file);
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
        UserSvgFile file = userSvgFileRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.FILE_NOT_FOUND));
        return fileStorageService.loadFileAsResource(file.getFilePath());
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

    private UserSavedFileResponse toResponse(UserSvgFile entity) {
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
            resp.setDealer(new UserSavedFileResponse.DealerSummaryDto(
                    entity.getDealer().getId(),
                    entity.getDealer().getName()
            ));
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
        if (node.getLevel() == VehicleNodeLevel.SUBTYPE && node.getParent() != null) {
            return node.getParent().getName();
        }
        if (node.getLevel() == VehicleNodeLevel.SERIES) {
            return node.getName();
        }
        return null;
    }
}
