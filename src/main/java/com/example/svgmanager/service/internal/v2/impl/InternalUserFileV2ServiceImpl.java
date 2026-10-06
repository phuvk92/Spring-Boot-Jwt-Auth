package com.example.svgmanager.service.internal.v2.impl;

import com.example.svgmanager.dto.internal.v2.*;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.entity.*;
import com.example.svgmanager.entity.v2.UserSvgFileV2;
import com.example.svgmanager.entity.v2.UserSvgFileV2Share;
import com.example.svgmanager.exception.*;
import com.example.svgmanager.repository.*;
import com.example.svgmanager.repository.internal.v2.UserSvgFileV2Repository;
import com.example.svgmanager.repository.internal.v2.UserSvgFileV2ShareRepository;
import com.example.svgmanager.repository.internal.v2.UserSvgFileV2Specification;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.internal.v2.EncryptedFileResult;
import com.example.svgmanager.service.internal.v2.FileEncryptionV2Service;
import com.example.svgmanager.service.internal.v2.FileStorageV2Service;
import com.example.svgmanager.service.internal.v2.InternalUserFileV2Service;
import com.example.svgmanager.service.internal.v2.security.RateLimiterV2Service;
import com.example.svgmanager.service.internal.v2.security.SvgSecurityV2Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InternalUserFileV2ServiceImpl implements InternalUserFileV2Service {

    private static final Logger log = LoggerFactory.getLogger(InternalUserFileV2ServiceImpl.class);

    private final UserSvgFileV2Repository userSvgFileV2Repository;
    private final UserSvgFileV2ShareRepository userSvgFileV2ShareRepository;
    private final UserRepository userRepository;
    private final DealerRepository dealerRepository;
    private final FileCategoryRepository fileCategoryRepository;
    private final VehicleNodeRepository vehicleNodeRepository;
    private final SvgSecurityV2Service svgSecurityV2Service;
    private final FileEncryptionV2Service fileEncryptionV2Service;
    private final FileStorageV2Service fileStorageV2Service;
    private final AuditLogService auditLogService;
    private final RateLimiterV2Service rateLimiterV2Service;

    public InternalUserFileV2ServiceImpl(
            UserSvgFileV2Repository userSvgFileV2Repository,
            UserSvgFileV2ShareRepository userSvgFileV2ShareRepository,
            UserRepository userRepository,
            DealerRepository dealerRepository,
            FileCategoryRepository fileCategoryRepository,
            VehicleNodeRepository vehicleNodeRepository,
            SvgSecurityV2Service svgSecurityV2Service,
            FileEncryptionV2Service fileEncryptionV2Service,
            FileStorageV2Service fileStorageV2Service,
            AuditLogService auditLogService,
            RateLimiterV2Service rateLimiterV2Service
    ) {
        this.userSvgFileV2Repository = userSvgFileV2Repository;
        this.userSvgFileV2ShareRepository = userSvgFileV2ShareRepository;
        this.userRepository = userRepository;
        this.dealerRepository = dealerRepository;
        this.fileCategoryRepository = fileCategoryRepository;
        this.vehicleNodeRepository = vehicleNodeRepository;
        this.svgSecurityV2Service = svgSecurityV2Service;
        this.fileEncryptionV2Service = fileEncryptionV2Service;
        this.fileStorageV2Service = fileStorageV2Service;
        this.auditLogService = auditLogService;
        this.rateLimiterV2Service = rateLimiterV2Service;
    }

    @Override
    @Transactional
    public UserSvgFileV2Response saveUserFile(
            User currentUser,
            byte[] fileBytes,
            String originalFilename,
            String mimeType,
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
            throw new UnauthorizedException("Chưa xác thực người dùng");
        }
        if (fileBytes == null || fileBytes.length == 0) {
            throw new BadRequestException("Nội dung file SVG không được để trống", ErrorCodes.FILE_REQUIRED);
        }

        // 1. Kiểm tra giới hạn tần suất tải lên (Rate Limiting)
        rateLimiterV2Service.checkRateLimit(currentUser.getId(), "UPLOAD");

        // 2. Validate & Sanitize SVG với security engine V2 chuyên dụng
        byte[] sanitizedBytes;
        try {
            sanitizedBytes = svgSecurityV2Service.validateAndSanitize(fileBytes, originalFilename, mimeType);
        } catch (SvgSecurityException ex) {
            String auditAction = ErrorCodes.SVG_MALWARE_DETECTED.equals(ex.getCode())
                    ? "V2_SVG_MALWARE_REJECTED"
                    : "V2_SVG_SECURITY_REJECTED";
            auditLogService.log(
                    currentUser.getUsername(),
                    currentUser.getRole() != null ? currentUser.getRole().name() : "USER",
                    auditAction,
                    "UserSvgFileV2",
                    null,
                    "Từ chối file: " + originalFilename + " - Lý do: " + ex.getMessage()
            );
            throw ex;
        }

        // 3. Mã hoá phong bì AES-256-GCM (Envelope Encryption)
        EncryptedFileResult encResult = fileEncryptionV2Service.encrypt(sanitizedBytes);

        // 4. Lưu ciphertext blob vào V2 storage độc lập
        String fileUuid = UUID.randomUUID().toString();
        String storageKey = fileStorageV2Service.storeEncryptedFile(encResult.ciphertext(), currentUser.getId(), fileUuid);

        // 5. Chuẩn hoá tên file
        String finalOriginal = StringUtils.hasText(originalFilename) ? originalFilename.trim() : "file.svg";
        String finalDisplay = StringUtils.hasText(customFileName) ? customFileName.trim() : finalOriginal;
        if (!finalDisplay.toLowerCase().endsWith(".svg")) {
            finalDisplay += ".svg";
        }

        // 6. Map category & vehicleNode
        FileCategory category = categoryId != null ? fileCategoryRepository.findById(categoryId).orElse(null) : null;
        VehicleNode vehicleNode = vehicleNodeId != null ? vehicleNodeRepository.findById(vehicleNodeId).orElse(null) : null;

        Dealer dealer = currentUser.getDealer() != null
                ? dealerRepository.findById(currentUser.getDealer().getId()).orElse(null)
                : null;

        // 7. Tạo bản ghi V2 Entity
        UserSvgFileV2 entity = UserSvgFileV2.builder()
                .fileName(finalDisplay)
                .originalFileName(finalOriginal)
                .storageKey(storageKey)
                .filePath(storageKey)
                .fileSize((long) sanitizedBytes.length)
                .mimeType("image/svg+xml")
                .plaintextChecksum(encResult.plaintextChecksum())
                .encryptedChecksum(encResult.encryptedChecksum())
                .encryptionAlgorithm(encResult.algorithm())
                .encryptionKeyVersion(encResult.keyVersion())
                .encryptedDek(encResult.encryptedDek())
                .encryptionIv(encResult.iv())
                .category(category)
                .vehicleNode(vehicleNode)
                .productGroup(productGroup)
                .productGroupName(productGroupName)
                .brandName(brandName)
                .modelName(modelName)
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
                .dealer(dealer)
                .build();

        UserSvgFileV2 saved = userSvgFileV2Repository.save(entity);
        log.info("[V2_SVG_UPLOAD] id={}, fileName='{}', userId={}, storageKey={}",
                saved.getId(), saved.getFileName(), currentUser.getId(), storageKey);

        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole() != null ? currentUser.getRole().name() : "USER",
                "V2_SVG_UPLOAD",
                "UserSvgFileV2",
                saved.getId(),
                "Tải lên và mã hoá file SVG V2: " + saved.getFileName()
        );

        return toResponse(saved, currentUser);
    }

    @Override
    @Transactional
    public UserSvgFileV2Response updateUserFile(
            User currentUser,
            Long id,
            byte[] fileBytes,
            String originalFilename,
            String mimeType,
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
            throw new UnauthorizedException("Chưa xác thực người dùng");
        }

        UserSvgFileV2 existing = userSvgFileV2Repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File bản lưu V2 không tồn tại", ErrorCodes.FILE_NOT_FOUND));

        if ("DELETED".equalsIgnoreCase(existing.getStatus())) {
            throw new ResourceNotFoundException("File bản lưu V2 không tồn tại", ErrorCodes.FILE_NOT_FOUND);
        }

        if (!existing.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Chỉ người sở hữu file mới có quyền sửa đổi", ErrorCodes.USER_FILE_SHARE_FORBIDDEN);
        }

        // Rate limit check
        rateLimiterV2Service.checkRateLimit(currentUser.getId(), "UPLOAD");

        // Nếu có nội dung mới, validate, sanitize và mã hoá lại
        if (fileBytes != null && fileBytes.length > 0) {
            byte[] sanitizedBytes = svgSecurityV2Service.validateAndSanitize(fileBytes, originalFilename, mimeType);
            EncryptedFileResult encResult = fileEncryptionV2Service.encrypt(sanitizedBytes);

            String fileUuid = UUID.randomUUID().toString();
            String storageKey = fileStorageV2Service.storeEncryptedFile(encResult.ciphertext(), currentUser.getId(), fileUuid);

            // Xoá file cũ trong V2 storage
            fileStorageV2Service.deleteEncryptedFile(existing.getStorageKey());

            existing.setStorageKey(storageKey);
            existing.setFilePath(storageKey);
            existing.setFileSize((long) sanitizedBytes.length);
            existing.setPlaintextChecksum(encResult.plaintextChecksum());
            existing.setEncryptedChecksum(encResult.encryptedChecksum());
            existing.setEncryptionAlgorithm(encResult.algorithm());
            existing.setEncryptionKeyVersion(encResult.keyVersion());
            existing.setEncryptedDek(encResult.encryptedDek());
            existing.setEncryptionIv(encResult.iv());
        }

        if (StringUtils.hasText(customFileName)) {
            String display = customFileName.trim();
            if (!display.toLowerCase().endsWith(".svg")) display += ".svg";
            existing.setFileName(display);
        }

        if (categoryId != null) {
            fileCategoryRepository.findById(categoryId).ifPresent(existing::setCategory);
        }
        if (vehicleNodeId != null) {
            vehicleNodeRepository.findById(vehicleNodeId).ifPresent(existing::setVehicleNode);
        }
        if (brandName != null) existing.setBrandName(brandName);
        if (modelName != null) existing.setModelName(modelName);
        if (yearFrom != null) existing.setYearFrom(yearFrom);
        if (yearTo != null) existing.setYearTo(yearTo);
        if (generationCode != null) existing.setGenerationCode(generationCode);
        if (productGroup != null) existing.setProductGroup(productGroup);
        if (productGroupName != null) existing.setProductGroupName(productGroupName);
        if (filmWidth != null) existing.setFilmWidth(filmWidth);
        if (rollLength != null) existing.setRollLength(rollLength);
        if (axisX != null) existing.setAxisX(axisX);
        if (axisY != null) existing.setAxisY(axisY);
        if (sourceFileKey != null) existing.setSourceFileKey(sourceFileKey);
        if (description != null) existing.setDescription(description);

        existing.setUpdatedAt(LocalDateTime.now());
        UserSvgFileV2 updated = userSvgFileV2Repository.save(existing);

        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole() != null ? currentUser.getRole().name() : "USER",
                "V2_SVG_UPLOAD",
                "UserSvgFileV2",
                updated.getId(),
                "Cập nhật file SVG V2: " + updated.getFileName()
        );

        return toResponse(updated, currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSvgFileV2Response> listUserFiles(
            User currentUser,
            String keyword,
            Long categoryId,
            String status,
            Pageable pageable
    ) {
        if (currentUser == null) {
            throw new UnauthorizedException("Chưa xác thực người dùng");
        }

        Specification<UserSvgFileV2> spec = UserSvgFileV2Specification.filterAccessibleByUser(
                currentUser, keyword, categoryId, status
        );

        Page<UserSvgFileV2> page = userSvgFileV2Repository.findAll(spec, pageable);
        return PageResponse.<UserSvgFileV2Response>builder()
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
    public UserSvgFileV2Response getUserFile(User currentUser, Long id) {
        UserSvgFileV2 file = findAndCheckAccess(currentUser, id);
        return toResponse(file, currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadUserFile(User currentUser, Long id) {
        rateLimiterV2Service.checkRateLimit(currentUser.getId(), "DOWNLOAD");
        byte[] decryptedBytes = getUserFileBytes(currentUser, id);

        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole() != null ? currentUser.getRole().name() : "USER",
                "V2_SVG_DOWNLOAD",
                "UserSvgFileV2",
                id,
                "Tải file SVG V2 giải mã: id=" + id
        );

        return new ByteArrayResource(decryptedBytes);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getUserFileBytes(User currentUser, Long id) {
        rateLimiterV2Service.checkRateLimit(currentUser.getId(), "PREVIEW");
        UserSvgFileV2 file = findAndCheckAccess(currentUser, id);

        // Đọc ciphertext blob từ V2 storage độc lập
        byte[] ciphertext = fileStorageV2Service.loadEncryptedFile(file.getStorageKey());

        // Giải mã phong bì on-the-fly
        byte[] decrypted = fileEncryptionV2Service.decrypt(
                ciphertext,
                file.getEncryptedDek(),
                file.getEncryptionIv(),
                file.getEncryptionAlgorithm(),
                file.getEncryptionKeyVersion()
        );

        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole() != null ? currentUser.getRole().name() : "USER",
                "V2_SVG_PREVIEW",
                "UserSvgFileV2",
                id,
                "Xem trước file SVG V2 giải mã on-the-fly: id=" + id
        );

        return decrypted;
    }

    @Override
    @Transactional
    public void deleteUserFile(User currentUser, Long id) {
        if (currentUser == null) {
            throw new UnauthorizedException("Chưa xác thực người dùng");
        }

        UserSvgFileV2 file = userSvgFileV2Repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File bản lưu V2 không tồn tại", ErrorCodes.FILE_NOT_FOUND));

        if ("DELETED".equalsIgnoreCase(file.getStatus())) {
            throw new ResourceNotFoundException("File bản lưu V2 không tồn tại", ErrorCodes.FILE_NOT_FOUND);
        }

        if (!file.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            if (canAccessV2(file, currentUser)) {
                throw new ForbiddenException("Chỉ người sở hữu file mới có quyền xoá file này", ErrorCodes.USER_FILE_SHARE_FORBIDDEN);
            }
            throw new ResourceNotFoundException("File bản lưu V2 không tồn tại", ErrorCodes.FILE_NOT_FOUND);
        }

        file.setStatus("DELETED");
        file.setUpdatedAt(LocalDateTime.now());
        userSvgFileV2Repository.save(file);

        log.info("[V2_SVG_DELETE] id={}, fileName='{}', userId={}", file.getId(), file.getFileName(), currentUser.getId());

        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole() != null ? currentUser.getRole().name() : "USER",
                "V2_SVG_DELETE",
                "UserSvgFileV2",
                file.getId(),
                "Xoá mềm bản lưu SVG V2: " + file.getFileName()
        );
    }

    @Override
    @Transactional
    public UserSvgFileV2ShareResponse shareFile(User currentUser, Long fileId, Long targetUserId) {
        if (currentUser == null) {
            throw new UnauthorizedException("Chưa xác thực người dùng");
        }
        if (currentUser.getRole() == Role.AGENT) {
            throw new ForbiddenException("Agent không có quyền chia sẻ file", ErrorCodes.USER_FILE_SHARE_FORBIDDEN);
        }

        UserSvgFileV2 file = userSvgFileV2Repository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File bản lưu V2 không tồn tại", ErrorCodes.USER_FILE_NOT_FOUND));

        if ("DELETED".equalsIgnoreCase(file.getStatus())) {
            throw new ResourceNotFoundException("File bản lưu V2 không tồn tại", ErrorCodes.USER_FILE_NOT_FOUND);
        }

        if (!canManageSharesV2(file, currentUser)) {
            throw new ForbiddenException("Bạn không có quyền chia sẻ file này", ErrorCodes.USER_FILE_SHARE_FORBIDDEN);
        }

        if (targetUserId == null) {
            throw new BadRequestException("Người dùng nhận quyền không hợp lệ", ErrorCodes.INVALID_SHARE_TARGET);
        }

        if (targetUserId.equals(currentUser.getId())) {
            throw new BadRequestException("Không thể tự chia sẻ file cho chính mình", ErrorCodes.CANNOT_SHARE_TO_SELF);
        }

        if (file.getUser() != null && targetUserId.equals(file.getUser().getId())) {
            throw new BadRequestException("Không thể chia sẻ cho chính chủ sở hữu của file", ErrorCodes.CANNOT_SHARE_TO_SELF);
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng nhận quyền không tồn tại", ErrorCodes.INVALID_SHARE_TARGET));

        if (targetUser.isDeleted()) {
            throw new ResourceNotFoundException("Người dùng nhận quyền không tồn tại", ErrorCodes.INVALID_SHARE_TARGET);
        }

        if (targetUser.getRole() != Role.USER) {
            throw new BadRequestException("Chỉ được chia sẻ cho người dùng máy cắt (USER)", ErrorCodes.INVALID_SHARE_TARGET);
        }

        if (!targetUser.isEnabled() || (targetUser.getExpirationDate() != null && targetUser.getExpirationDate().isBefore(java.time.LocalDate.now()))) {
            throw new BadRequestException("Người dùng nhận quyền đã bị vô hiệu hoá hoặc hết hạn", ErrorCodes.INVALID_SHARE_TARGET);
        }

        rateLimiterV2Service.checkRateLimit(currentUser.getId(), "SHARE");

        Optional<UserSvgFileV2Share> existingOpt = userSvgFileV2ShareRepository.findByUserSvgFileIdAndSharedToUserId(fileId, targetUserId);
        UserSvgFileV2Share savedShare;
        if (existingOpt.isPresent()) {
            UserSvgFileV2Share existing = existingOpt.get();
            if ("ACTIVE".equalsIgnoreCase(existing.getStatus())) {
                return toShareResponse(existing);
            }
            existing.setStatus("ACTIVE");
            existing.setSharedByUser(currentUser);
            existing.setUpdatedAt(LocalDateTime.now());
            savedShare = userSvgFileV2ShareRepository.save(existing);
        } else {
            UserSvgFileV2Share newShare = UserSvgFileV2Share.builder()
                    .userSvgFile(file)
                    .sharedToUser(targetUser)
                    .sharedByUser(currentUser)
                    .status("ACTIVE")
                    .build();
            savedShare = userSvgFileV2ShareRepository.save(newShare);
        }

        log.info("[V2_SVG_SHARE] fileId={}, ownerId={}, sharedToUserId={}, sharedByUserId={}",
                file.getId(), file.getUser().getId(), targetUser.getId(), currentUser.getId());

        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole() != null ? currentUser.getRole().name() : "USER",
                "V2_SVG_SHARE",
                "UserSvgFileV2Share",
                savedShare.getId(),
                String.format("fileId=%d, ownerId=%d, sharedToUserId=%d, sharedByUserId=%d",
                        file.getId(), file.getUser().getId(), targetUser.getId(), currentUser.getId())
        );

        return toShareResponse(savedShare);
    }

    @Override
    @Transactional(readOnly = true)
    public FileSharesV2Response getShares(User currentUser, Long fileId) {
        if (currentUser == null) {
            throw new UnauthorizedException("Chưa xác thực người dùng");
        }

        UserSvgFileV2 file = userSvgFileV2Repository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File bản lưu V2 không tồn tại", ErrorCodes.USER_FILE_NOT_FOUND));

        if ("DELETED".equalsIgnoreCase(file.getStatus())) {
            throw new ResourceNotFoundException("File bản lưu V2 không tồn tại", ErrorCodes.USER_FILE_NOT_FOUND);
        }

        if (!canManageSharesV2(file, currentUser)) {
            throw new ForbiddenException("Bạn không có quyền xem danh sách chia sẻ của file này", ErrorCodes.USER_FILE_SHARE_FORBIDDEN);
        }

        List<UserSvgFileV2Share> shares = userSvgFileV2ShareRepository.findByUserSvgFileIdAndStatus(fileId, "ACTIVE");
        List<UserSvgFileV2ShareResponse> dtos = shares.stream().map(this::toShareResponse).toList();
        return new FileSharesV2Response(fileId, dtos);
    }

    @Override
    @Transactional
    public void revokeShare(User currentUser, Long fileId, Long targetUserId) {
        if (currentUser == null) {
            throw new UnauthorizedException("Chưa xác thực người dùng");
        }
        if (currentUser.getRole() == Role.AGENT) {
            throw new ForbiddenException("Agent không có quyền thu hồi chia sẻ file", ErrorCodes.USER_FILE_SHARE_FORBIDDEN);
        }

        UserSvgFileV2 file = userSvgFileV2Repository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File bản lưu V2 không tồn tại", ErrorCodes.USER_FILE_NOT_FOUND));

        if ("DELETED".equalsIgnoreCase(file.getStatus())) {
            throw new ResourceNotFoundException("File bản lưu V2 không tồn tại", ErrorCodes.USER_FILE_NOT_FOUND);
        }

        if (!canManageSharesV2(file, currentUser)) {
            throw new ForbiddenException("Bạn không có quyền thu hồi chia sẻ của file này", ErrorCodes.USER_FILE_SHARE_FORBIDDEN);
        }

        UserSvgFileV2Share share = userSvgFileV2ShareRepository.findByUserSvgFileIdAndSharedToUserIdAndStatus(fileId, targetUserId, "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền chia sẻ của người dùng này", ErrorCodes.USER_FILE_SHARE_NOT_FOUND));

        share.setStatus("REVOKED");
        share.setUpdatedAt(LocalDateTime.now());
        userSvgFileV2ShareRepository.save(share);

        log.info("[V2_SVG_SHARE_REVOKED] fileId={}, targetUserId={}, revokedByUserId={}", file.getId(), targetUserId, currentUser.getId());

        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole() != null ? currentUser.getRole().name() : "USER",
                "V2_SVG_SHARE_REVOKED",
                "UserSvgFileV2Share",
                share.getId(),
                String.format("fileId=%d, targetUserId=%d, revokedByUserId=%d", file.getId(), targetUserId, currentUser.getId())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public String getOriginalFilename(Long id) {
        return userSvgFileV2Repository.findById(id)
                .map(UserSvgFileV2::getFileName)
                .orElse("file.svg");
    }

    private UserSvgFileV2 findAndCheckAccess(User currentUser, Long id) {
        if (currentUser == null) {
            throw new UnauthorizedException("Chưa xác thực người dùng");
        }

        UserSvgFileV2 file = userSvgFileV2Repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File bản lưu V2 không tồn tại", ErrorCodes.FILE_NOT_FOUND));

        if ("DELETED".equalsIgnoreCase(file.getStatus())) {
            throw new ResourceNotFoundException("File bản lưu V2 không tồn tại", ErrorCodes.FILE_NOT_FOUND);
        }

        if (!canAccessV2(file, currentUser)) {
            throw new ForbiddenException("Không có quyền truy cập file này", ErrorCodes.USER_FILE_SHARE_FORBIDDEN);
        }

        return file;
    }

    private boolean canAccessV2(UserSvgFileV2 file, User user) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (file.getUser() != null && file.getUser().getId().equals(user.getId())) {
            return true;
        }
        return userSvgFileV2ShareRepository.existsByUserSvgFileIdAndSharedToUserIdAndStatus(file.getId(), user.getId(), "ACTIVE");
    }

    private boolean canManageSharesV2(UserSvgFileV2 file, User user) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        return file.getUser() != null && file.getUser().getId().equals(user.getId());
    }

    private UserSvgFileV2Response toResponse(UserSvgFileV2 entity, User currentUser) {
        boolean isOwner = entity.getUser() != null && entity.getUser().getId().equals(currentUser.getId());
        String accessType = isOwner ? "OWNER" : "SHARED";

        UserSvgFileV2Response response = new UserSvgFileV2Response();
        response.setId(entity.getId());
        response.setFileName(entity.getFileName());
        response.setOriginalFileName(entity.getOriginalFileName());
        response.setDescription(entity.getDescription());
        response.setFileSize(entity.getFileSize());
        response.setMimeType(entity.getMimeType());
        response.setChecksum(entity.getPlaintextChecksum());
        response.setStatus(entity.getStatus());
        response.setAccessType(accessType);
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getCategory() != null) {
            response.setCategory(new UserSvgFileV2Response.CategoryDto(
                    entity.getCategory().getId(),
                    entity.getCategory().getName()
            ));
        }

        if (entity.getVehicleNode() != null || entity.getProductGroup() != null || entity.getBrandName() != null) {
            response.setVehicleConfiguration(new UserSvgFileV2Response.VehicleConfigurationDto(
                    entity.getVehicleNode() != null ? entity.getVehicleNode().getId() : null,
                    entity.getProductGroup(),
                    entity.getProductGroupName(),
                    entity.getBrandName(),
                    entity.getModelName(),
                    entity.getYearFrom(),
                    entity.getYearTo(),
                    entity.getGenerationCode()
            ));
        }

        if (entity.getFilmWidth() != null || entity.getRollLength() != null || entity.getAxisX() != null || entity.getAxisY() != null) {
            response.setCutSize(new UserSvgFileV2Response.CutSizeDto(
                    entity.getFilmWidth(),
                    entity.getFilmWidthUnit(),
                    entity.getRollLength(),
                    entity.getRollLengthUnit(),
                    entity.getAxisX(),
                    entity.getAxisY()
            ));
        }

        if (entity.getUser() != null) {
            String name = entity.getUser().getFullName() != null ? entity.getUser().getFullName() : entity.getUser().getUsername();
            response.setCreatedBy(new UserSvgFileV2Response.UserSummaryDto(
                    entity.getUser().getId(),
                    entity.getUser().getUsername(),
                    name
            ));
        }

        if (entity.getDealer() != null) {
            response.setDealer(new UserSvgFileV2Response.DealerSummaryDto(
                    entity.getDealer().getId(),
                    entity.getDealer().getName()
            ));
        }

        return response;
    }

    private UserSvgFileV2ShareResponse toShareResponse(UserSvgFileV2Share share) {
        User sharedTo = share.getSharedToUser();
        User sharedBy = share.getSharedByUser();

        UserSvgFileV2ShareResponse response = new UserSvgFileV2ShareResponse();
        response.setUserId(sharedTo.getId());
        response.setUsername(sharedTo.getUsername());
        String name = sharedTo.getFullName() != null ? sharedTo.getFullName() : sharedTo.getUsername();
        response.setDisplayName(name);
        if (sharedTo.getDealer() != null) {
            response.setDealerId(sharedTo.getDealer().getId());
            response.setDealerName(sharedTo.getDealer().getName());
        }
        response.setSharedAt(share.getCreatedAt());
        response.setStatus(share.getStatus());

        if (sharedBy != null) {
            response.setSharedBy(new UserSvgFileV2ShareResponse.SharedByDto(
                    sharedBy.getId(),
                    sharedBy.getUsername()
            ));
        }

        return response;
    }
}
