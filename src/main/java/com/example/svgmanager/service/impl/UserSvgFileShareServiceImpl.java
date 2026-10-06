package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.response.FileSharesResponse;
import com.example.svgmanager.dto.response.UserSvgFileShareResponse;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.UserSvgFile;
import com.example.svgmanager.entity.UserSvgFileShare;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.exception.UnauthorizedException;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.repository.UserSvgFileRepository;
import com.example.svgmanager.repository.UserSvgFileShareRepository;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.UserSvgFileShareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserSvgFileShareServiceImpl implements UserSvgFileShareService {

    private static final Logger log = LoggerFactory.getLogger(UserSvgFileShareServiceImpl.class);

    private final UserSvgFileShareRepository userSvgFileShareRepository;
    private final UserSvgFileRepository userSvgFileRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public UserSvgFileShareServiceImpl(UserSvgFileShareRepository userSvgFileShareRepository,
                                       UserSvgFileRepository userSvgFileRepository,
                                       UserRepository userRepository,
                                       AuditLogService auditLogService) {
        this.userSvgFileShareRepository = userSvgFileShareRepository;
        this.userSvgFileRepository = userSvgFileRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public UserSvgFileShareResponse shareFile(User currentUser, Long fileId, Long targetUserId) {
        if (currentUser == null) {
            throw new UnauthorizedException("Chưa xác thực người dùng");
        }
        if (currentUser.getRole() == Role.AGENT) {
            throw new ForbiddenException("Agent không có quyền chia sẻ file", ErrorCodes.USER_FILE_SHARE_FORBIDDEN);
        }

        UserSvgFile file = userSvgFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.USER_FILE_NOT_FOUND));

        if ("DELETED".equalsIgnoreCase(file.getStatus())) {
            throw new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.USER_FILE_NOT_FOUND);
        }

        if (!canManageShares(file, currentUser)) {
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

        // Kiểm tra bản ghi chia sẻ đã tồn tại hay chưa
        Optional<UserSvgFileShare> existingOpt = userSvgFileShareRepository.findByUserSvgFileIdAndSharedToUserId(fileId, targetUserId);
        UserSvgFileShare savedShare;
        if (existingOpt.isPresent()) {
            UserSvgFileShare existing = existingOpt.get();
            if ("ACTIVE".equalsIgnoreCase(existing.getStatus())) {
                // Idempotent: đã chia sẻ và đang active thì trả về luôn không duplicate
                return toShareResponse(existing);
            }
            existing.setStatus("ACTIVE");
            existing.setSharedByUser(currentUser);
            existing.setUpdatedAt(LocalDateTime.now());
            savedShare = userSvgFileShareRepository.save(existing);
        } else {
            UserSvgFileShare newShare = UserSvgFileShare.builder()
                    .userSvgFile(file)
                    .sharedToUser(targetUser)
                    .sharedByUser(currentUser)
                    .status("ACTIVE")
                    .build();
            savedShare = userSvgFileShareRepository.save(newShare);
        }

        log.info("[USER_FILE_SHARED] fileId={}, ownerId={}, sharedToUserId={}, sharedByUserId={}",
                file.getId(), file.getUser().getId(), targetUser.getId(), currentUser.getId());

        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole() != null ? currentUser.getRole().name() : "USER",
                "USER_FILE_SHARED",
                "UserSvgFileShare",
                savedShare.getId(),
                String.format("fileId=%d, ownerId=%d, sharedToUserId=%d, sharedByUserId=%d",
                        file.getId(), file.getUser().getId(), targetUser.getId(), currentUser.getId())
        );

        return toShareResponse(savedShare);
    }

    @Override
    @Transactional(readOnly = true)
    public FileSharesResponse getShares(User currentUser, Long fileId) {
        if (currentUser == null) {
            throw new UnauthorizedException("Chưa xác thực người dùng");
        }

        UserSvgFile file = userSvgFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.USER_FILE_NOT_FOUND));

        if ("DELETED".equalsIgnoreCase(file.getStatus())) {
            throw new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.USER_FILE_NOT_FOUND);
        }

        if (!canManageShares(file, currentUser)) {
            throw new ForbiddenException("Bạn không có quyền xem danh sách chia sẻ của file này", ErrorCodes.USER_FILE_SHARE_FORBIDDEN);
        }

        List<UserSvgFileShare> shares = userSvgFileShareRepository.findByUserSvgFileIdAndStatus(fileId, "ACTIVE");
        List<UserSvgFileShareResponse> shareDtos = shares.stream().map(this::toShareResponse).toList();
        return new FileSharesResponse(fileId, shareDtos);
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

        UserSvgFile file = userSvgFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.USER_FILE_NOT_FOUND));

        if ("DELETED".equalsIgnoreCase(file.getStatus())) {
            throw new ResourceNotFoundException("File bản lưu không tồn tại", ErrorCodes.USER_FILE_NOT_FOUND);
        }

        if (!canManageShares(file, currentUser)) {
            throw new ForbiddenException("Bạn không có quyền thu hồi chia sẻ của file này", ErrorCodes.USER_FILE_SHARE_FORBIDDEN);
        }

        UserSvgFileShare share = userSvgFileShareRepository.findByUserSvgFileIdAndSharedToUserIdAndStatus(fileId, targetUserId, "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền chia sẻ của người dùng này", ErrorCodes.USER_FILE_SHARE_NOT_FOUND));

        share.setStatus("REVOKED");
        share.setUpdatedAt(LocalDateTime.now());
        userSvgFileShareRepository.save(share);

        log.info("[USER_FILE_SHARE_REVOKED] fileId={}, ownerId={}, sharedToUserId={}, revokedByUserId={}",
                file.getId(), file.getUser().getId(), targetUserId, currentUser.getId());

        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole() != null ? currentUser.getRole().name() : "USER",
                "USER_FILE_SHARE_REVOKED",
                "UserSvgFileShare",
                share.getId(),
                String.format("fileId=%d, ownerId=%d, sharedToUserId=%d, sharedByUserId=%d",
                        file.getId(), file.getUser().getId(), targetUserId, currentUser.getId())
        );
    }

    @Override
    public boolean canAccess(UserSvgFile file, User currentUser) {
        if (file == null || currentUser == null) {
            return false;
        }
        if (currentUser.getRole() == Role.ADMIN) {
            return true;
        }
        if (file.getUser() != null && file.getUser().getId().equals(currentUser.getId())) {
            return true;
        }
        return userSvgFileShareRepository.existsByUserSvgFileIdAndSharedToUserIdAndStatus(file.getId(), currentUser.getId(), "ACTIVE");
    }

    @Override
    public boolean canManageShares(UserSvgFile file, User currentUser) {
        if (file == null || currentUser == null) {
            return false;
        }
        if (currentUser.getRole() == Role.ADMIN) {
            return true;
        }
        if (currentUser.getRole() == Role.USER && file.getUser() != null && file.getUser().getId().equals(currentUser.getId())) {
            return true;
        }
        return false;
    }

    private UserSvgFileShareResponse toShareResponse(UserSvgFileShare share) {
        UserSvgFileShareResponse dto = new UserSvgFileShareResponse();
        User target = share.getSharedToUser();
        dto.setUserId(target.getId());
        dto.setUsername(target.getUsername());
        String displayName = StringUtils.hasText(target.getFullName()) ? target.getFullName() : target.getUsername();
        dto.setDisplayName(displayName);

        if (target.getDealer() != null) {
            dto.setDealerId(target.getDealer().getId());
            String dName = null;
            try {
                dName = target.getDealer().getName();
            } catch (Exception ignored) {}
            dto.setDealerName(dName);
        }

        dto.setSharedAt(share.getCreatedAt());
        if (share.getSharedByUser() != null) {
            dto.setSharedBy(new UserSvgFileShareResponse.SharedByDto(
                    share.getSharedByUser().getId(),
                    share.getSharedByUser().getUsername()
            ));
        }
        dto.setStatus(share.getStatus());
        return dto;
    }
}
