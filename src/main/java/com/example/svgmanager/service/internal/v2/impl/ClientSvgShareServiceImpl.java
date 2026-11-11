package com.example.svgmanager.service.internal.v2.impl;

import com.example.svgmanager.dto.internal.v2.ClientShareResponse;
import com.example.svgmanager.dto.internal.v2.CreateClientShareRequest;
import com.example.svgmanager.dto.internal.v2.VerifyClientShareResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.v2.ClientSvgShare;
import com.example.svgmanager.entity.v2.UserSvgFileV2;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.exception.UnauthorizedException;
import com.example.svgmanager.repository.internal.v2.ClientSvgShareRepository;
import com.example.svgmanager.repository.internal.v2.UserSvgFileV2Repository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.internal.v2.ClientShareTokenService;
import com.example.svgmanager.service.internal.v2.ClientSvgShareService;
import com.example.svgmanager.service.internal.v2.FileEncryptionV2Service;
import com.example.svgmanager.service.internal.v2.FileStorageV2Service;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClientSvgShareServiceImpl implements ClientSvgShareService {

    private static final Logger log = LoggerFactory.getLogger(ClientSvgShareServiceImpl.class);
    private static final String RANDOM_ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final ClientSvgShareRepository clientSvgShareRepository;
    private final UserSvgFileV2Repository userSvgFileV2Repository;
    private final ClientShareTokenService clientShareTokenService;
    private final FileStorageV2Service fileStorageV2Service;
    private final FileEncryptionV2Service fileEncryptionV2Service;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    public ClientSvgShareServiceImpl(
            ClientSvgShareRepository clientSvgShareRepository,
            UserSvgFileV2Repository userSvgFileV2Repository,
            ClientShareTokenService clientShareTokenService,
            FileStorageV2Service fileStorageV2Service,
            FileEncryptionV2Service fileEncryptionV2Service,
            AuditLogService auditLogService,
            PasswordEncoder passwordEncoder,
            CurrentUserService currentUserService
    ) {
        this.clientSvgShareRepository = clientSvgShareRepository;
        this.userSvgFileV2Repository = userSvgFileV2Repository;
        this.clientShareTokenService = clientShareTokenService;
        this.fileStorageV2Service = fileStorageV2Service;
        this.fileEncryptionV2Service = fileEncryptionV2Service;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
    }

    @Override
    @Transactional
    public ClientShareResponse createShare(User currentUser, CreateClientShareRequest request, HttpServletRequest httpRequest) {
        if (currentUser == null) {
            throw new UnauthorizedException("Chưa xác thực người dùng");
        }

        UserSvgFileV2 file = userSvgFileV2Repository.findById(request.getFileId())
                .orElseThrow(() -> new ResourceNotFoundException("File bản lưu V2 không tồn tại", ErrorCodes.FILE_NOT_FOUND));

        if ("DELETED".equalsIgnoreCase(file.getStatus())) {
            throw new ResourceNotFoundException("File bản lưu V2 không tồn tại", ErrorCodes.FILE_NOT_FOUND);
        }

        // Quyền sở hữu: Chỉ Owner hoặc ADMIN mới được tạo lượt chia sẻ
        if (!file.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Chỉ người sở hữu file mới có quyền tạo lượt chia sẻ", ErrorCodes.CLIENT_SHARE_FORBIDDEN);
        }

        // Sinh Share ID duy nhất theo quy tắc: ddMMyyyy + 6 ký tự random (Asia/Ho_Chi_Minh)
        String shareId = generateUniqueShareId();

        String rawPassword = request.getPassword().trim();
        String passwordHash = passwordEncoder.encode(rawPassword);

        String clientId = resolveClientId(httpRequest);

        ClientSvgShare share = ClientSvgShare.builder()
                .shareId(shareId)
                .userSvgFile(file)
                .ownerClientId(clientId)
                .ownerUser(currentUser)
                .passwordHash(passwordHash)
                .status("ACTIVE")
                .note(request.getNote())
                .expiresAt(request.getExpiresAt())
                .verifyCount(0)
                .previewCount(0)
                .downloadCount(0)
                .failedVerifyAttempts(0)
                .tokenVersion(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ClientSvgShare saved = clientSvgShareRepository.save(share);

        String requestId = resolveRequestId(httpRequest);
        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole() != null ? currentUser.getRole().name() : "USER",
                "CLIENT_SHARE_CREATED",
                "ClientSvgShare",
                saved.getId(),
                String.format("shareId=%s, fileId=%d, fileName=%s, owner=%s, requestId=%s, result=SUCCESS",
                        shareId, file.getId(), file.getFileName(), currentUser.getUsername(), requestId)
        );

        log.info("[CLIENT_SHARE_CREATED] shareId={}, fileId={}, owner={}", shareId, file.getId(), currentUser.getUsername());

        // Tuyệt đối không trả Password trong response theo yêu cầu mục 3
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClientShareResponse> listShares(
            User currentUser,
            String status,
            Long fileId,
            String keyword,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable,
            HttpServletRequest httpRequest
    ) {
        if (currentUser == null) {
            throw new UnauthorizedException("Chưa xác thực người dùng");
        }

        Page<ClientSvgShare> page;
        if (currentUser.getRole() == Role.ADMIN) {
            page = clientSvgShareRepository.findAllWithFilters(status, fileId, keyword, from, to, pageable);
        } else {
            // Client A chỉ xem được lượt chia sẻ do chính mình tạo
            page = clientSvgShareRepository.findByOwnerWithFilters(currentUser.getId(), status, fileId, keyword, from, to, pageable);
        }

        String requestId = resolveRequestId(httpRequest);
        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole() != null ? currentUser.getRole().name() : "USER",
                "CLIENT_SHARE_LISTED",
                "ClientSvgShare",
                null,
                String.format("actor=%s, role=%s, status=%s, fileId=%s, page=%d, size=%d, total=%d, requestId=%s, result=SUCCESS",
                        currentUser.getUsername(), currentUser.getRole(), status, fileId,
                        pageable.getPageNumber(), pageable.getPageSize(), page.getTotalElements(), requestId)
        );

        return PageResponse.of(page.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientShareResponse getShare(User currentUser, String shareId, HttpServletRequest httpRequest) {
        if (currentUser == null) {
            throw new UnauthorizedException("Chưa xác thực người dùng");
        }

        ClientSvgShare share = clientSvgShareRepository.findByShareId(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Lượt chia sẻ không tồn tại", ErrorCodes.CLIENT_SHARE_NOT_FOUND));

        // Kiểm tra quyền: Chỉ Owner hoặc ADMIN mới được xem chi tiết
        if (!share.getOwnerUser().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Bạn không có quyền xem thông tin lượt chia sẻ này", ErrorCodes.CLIENT_SHARE_FORBIDDEN);
        }

        String requestId = resolveRequestId(httpRequest);
        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole() != null ? currentUser.getRole().name() : "USER",
                "CLIENT_SHARE_VIEWED",
                "ClientSvgShare",
                share.getId(),
                String.format("shareId=%s, fileId=%d, fileName=%s, viewer=%s, requestId=%s, result=SUCCESS",
                        share.getShareId(), share.getUserSvgFile().getId(), share.getUserSvgFile().getFileName(),
                        currentUser.getUsername(), requestId)
        );

        return toResponse(share);
    }

    @Override
    @Transactional
    public ClientShareResponse revokeShare(User currentUser, String shareId, String reason, HttpServletRequest httpRequest) {
        if (currentUser == null) {
            throw new UnauthorizedException("Chưa xác thực người dùng");
        }

        ClientSvgShare share = clientSvgShareRepository.findByShareId(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Lượt chia sẻ không tồn tại", ErrorCodes.CLIENT_SHARE_NOT_FOUND));

        // Kiểm tra quyền: Chỉ Owner hoặc ADMIN mới được thu hồi
        if (!share.getOwnerUser().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Bạn không có quyền thu hồi lượt chia sẻ này", ErrorCodes.CLIENT_SHARE_FORBIDDEN);
        }

        // An toàn khi gọi lặp lại (Idempotency): nếu đã thu hồi thì trả kết quả thành công ngay
        if (share.isRevoked()) {
            log.info("[CLIENT_SHARE_ALREADY_REVOKED] shareId={} is already revoked", shareId);
            return toResponse(share);
        }

        share.setStatus("REVOKED");
        share.setRevokedAt(LocalDateTime.now());
        share.setRevokedBy(currentUser);
        share.setRevokedByUsername(currentUser.getUsername());
        share.setRevokeReason(reason);
        // Tăng version token để vô hiệu hóa tức thì mọi share token đã cấp trước đó
        share.setTokenVersion(share.getTokenVersion() + 1);
        share.setUpdatedAt(LocalDateTime.now());

        ClientSvgShare saved = clientSvgShareRepository.save(share);

        String requestId = resolveRequestId(httpRequest);
        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole() != null ? currentUser.getRole().name() : "USER",
                "CLIENT_SHARE_REVOKED",
                "ClientSvgShare",
                saved.getId(),
                String.format("shareId=%s, fileId=%d, fileName=%s, revokedBy=%s, reason=%s, requestId=%s, result=SUCCESS",
                        share.getShareId(), share.getUserSvgFile().getId(), share.getUserSvgFile().getFileName(),
                        currentUser.getUsername(), reason, requestId)
        );

        log.info("[CLIENT_SHARE_REVOKED] shareId={}, fileId={}, revokedBy={}", shareId, share.getUserSvgFile().getId(), currentUser.getUsername());

        // File gốc không bị xoá, chỉ huỷ quyền truy cập chia sẻ
        return toResponse(saved);
    }

    @Override
    @Transactional
    public VerifyClientShareResponse verifyShare(String shareId, String password, HttpServletRequest httpRequest) {
        ClientSvgShare share = clientSvgShareRepository.findByShareId(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Lượt chia sẻ không tồn tại", ErrorCodes.CLIENT_SHARE_NOT_FOUND));

        String clientB = resolveClientBIdentifier(httpRequest);
        String requestId = resolveRequestId(httpRequest);

        // Kiểm tra trạng thái đã thu hồi
        if (share.isRevoked()) {
            auditLogService.log(
                    clientB,
                    "CLIENT",
                    "CLIENT_SHARE_VERIFY_FAILED",
                    "ClientSvgShare",
                    share.getId(),
                    String.format("shareId=%s, clientB=%s, reason=SHARE_REVOKED, requestId=%s, result=FAILED",
                            shareId, clientB, requestId)
            );
            throw new BadRequestException("Lượt chia sẻ đã bị thu hồi bởi người tạo", ErrorCodes.CLIENT_SHARE_REVOKED);
        }

        // Kiểm tra hết hạn
        if (share.isExpired()) {
            auditLogService.log(
                    clientB,
                    "CLIENT",
                    "CLIENT_SHARE_VERIFY_FAILED",
                    "ClientSvgShare",
                    share.getId(),
                    String.format("shareId=%s, clientB=%s, reason=SHARE_EXPIRED, requestId=%s, result=FAILED",
                            shareId, clientB, requestId)
            );
            throw new BadRequestException("Lượt chia sẻ đã hết hạn sử dụng", ErrorCodes.CLIENT_SHARE_EXPIRED);
        }

        // Kiểm tra chủ sở hữu lượt chia sẻ còn hiệu lực
        if (share.getOwnerUser() != null && (!share.getOwnerUser().isEnabled() || share.getOwnerUser().isDeleted() || (share.getOwnerUser().getRole() != Role.ADMIN && share.getOwnerUser().isExpired()))) {
            throw new BadRequestException("Tài khoản người chia sẻ đã hết hạn hoặc bị vô hiệu hóa", ErrorCodes.USER_ACCOUNT_EXPIRED);
        }

        // Chống tấn công brute-force mật khẩu (khóa tạm thời nếu nhập sai liên tiếp)
        if (share.getFailedVerifyAttempts() >= MAX_FAILED_ATTEMPTS) {
            auditLogService.log(
                    clientB,
                    "CLIENT",
                    "CLIENT_SHARE_VERIFY_FAILED",
                    "ClientSvgShare",
                    share.getId(),
                    String.format("shareId=%s, clientB=%s, reason=RATE_LIMIT_EXCEEDED, attempts=%d, requestId=%s, result=FAILED",
                            shareId, clientB, share.getFailedVerifyAttempts(), requestId)
            );
            throw new BadRequestException("Lượt chia sẻ tạm thời bị khóa do nhập sai mật khẩu quá 5 lần. Vui lòng thử lại sau.", ErrorCodes.RATE_LIMIT_EXCEEDED);
        }

        // Kiểm tra file gốc còn tồn tại
        if ("DELETED".equalsIgnoreCase(share.getUserSvgFile().getStatus())) {
            throw new ResourceNotFoundException("File bản lưu đã bị xoá", ErrorCodes.FILE_NOT_FOUND);
        }

        // Xác thực mật khẩu
        if (!passwordEncoder.matches(password, share.getPasswordHash())) {
            share.setFailedVerifyAttempts(share.getFailedVerifyAttempts() + 1);
            share.setLastAccessedAt(LocalDateTime.now());
            clientSvgShareRepository.save(share);

            auditLogService.log(
                    clientB,
                    "CLIENT",
                    "CLIENT_SHARE_VERIFY_FAILED",
                    "ClientSvgShare",
                    share.getId(),
                    String.format("shareId=%s, clientB=%s, reason=INVALID_PASSWORD, attempts=%d, requestId=%s, result=FAILED",
                            shareId, clientB, share.getFailedVerifyAttempts(), requestId)
            );
            throw new BadRequestException("Mật khẩu chia sẻ không chính xác", ErrorCodes.CLIENT_SHARE_INVALID_PASSWORD);
        }

        // Reset bộ đếm số lần sai, tăng verifyCount và cập nhật lastAccessedAt
        share.setFailedVerifyAttempts(0);
        share.setVerifyCount(share.getVerifyCount() + 1);
        share.setLastAccessedAt(LocalDateTime.now());
        clientSvgShareRepository.save(share);

        // Sinh share token
        String shareToken = clientShareTokenService.generateToken(share, clientShareTokenService.getDefaultTokenValiditySeconds());

        auditLogService.log(
                clientB,
                "CLIENT",
                "CLIENT_SHARE_VERIFY_SUCCESS",
                "ClientSvgShare",
                share.getId(),
                String.format("shareId=%s, fileId=%d, fileName=%s, clientB=%s, requestId=%s, result=SUCCESS",
                        share.getShareId(), share.getUserSvgFile().getId(), share.getUserSvgFile().getFileName(),
                        clientB, requestId)
        );

        return new VerifyClientShareResponse(
                shareToken,
                "Bearer",
                clientShareTokenService.getDefaultTokenValiditySeconds(),
                share.getShareId(),
                share.getUserSvgFile().getId(),
                share.getUserSvgFile().getFileName()
        );
    }

    @Override
    @Transactional
    public byte[] previewShare(String shareId, String shareToken, HttpServletRequest httpRequest) {
        ClientSvgShare share = findAndValidateToken(shareId, shareToken, httpRequest, "PREVIEW");

        UserSvgFileV2 file = share.getUserSvgFile();
        if ("DELETED".equalsIgnoreCase(file.getStatus())) {
            throw new ResourceNotFoundException("File bản lưu đã bị xoá", ErrorCodes.FILE_NOT_FOUND);
        }

        // Đọc ciphertext từ V2 storage và giải mã phong bì on-the-fly
        byte[] ciphertext = fileStorageV2Service.loadEncryptedFile(file.getStorageKey());
        byte[] decrypted = fileEncryptionV2Service.decrypt(
                ciphertext,
                file.getEncryptedDek(),
                file.getEncryptionIv(),
                file.getEncryptionAlgorithm(),
                file.getEncryptionKeyVersion()
        );

        share.setPreviewCount(share.getPreviewCount() + 1);
        share.setLastAccessedAt(LocalDateTime.now());
        clientSvgShareRepository.save(share);

        String clientB = resolveClientBIdentifier(httpRequest);
        String requestId = resolveRequestId(httpRequest);
        auditLogService.log(
                clientB,
                "CLIENT",
                "CLIENT_SHARE_PREVIEWED",
                "ClientSvgShare",
                share.getId(),
                String.format("shareId=%s, fileId=%d, fileName=%s, clientB=%s, requestId=%s, result=SUCCESS",
                        share.getShareId(), file.getId(), file.getFileName(), clientB, requestId)
        );

        return decrypted;
    }

    @Override
    @Transactional
    public byte[] downloadShare(String shareId, String shareToken, HttpServletRequest httpRequest) {
        ClientSvgShare share = findAndValidateToken(shareId, shareToken, httpRequest, "DOWNLOAD");

        UserSvgFileV2 file = share.getUserSvgFile();
        if ("DELETED".equalsIgnoreCase(file.getStatus())) {
            throw new ResourceNotFoundException("File bản lưu đã bị xoá", ErrorCodes.FILE_NOT_FOUND);
        }

        byte[] ciphertext = fileStorageV2Service.loadEncryptedFile(file.getStorageKey());
        byte[] decrypted = fileEncryptionV2Service.decrypt(
                ciphertext,
                file.getEncryptedDek(),
                file.getEncryptionIv(),
                file.getEncryptionAlgorithm(),
                file.getEncryptionKeyVersion()
        );

        share.setDownloadCount(share.getDownloadCount() + 1);
        share.setLastAccessedAt(LocalDateTime.now());
        clientSvgShareRepository.save(share);

        String clientB = resolveClientBIdentifier(httpRequest);
        String requestId = resolveRequestId(httpRequest);
        auditLogService.log(
                clientB,
                "CLIENT",
                "CLIENT_SHARE_DOWNLOADED",
                "ClientSvgShare",
                share.getId(),
                String.format("shareId=%s, fileId=%d, fileName=%s, clientB=%s, requestId=%s, result=SUCCESS",
                        share.getShareId(), file.getId(), file.getFileName(), clientB, requestId)
        );

        return decrypted;
    }

    @Override
    @Transactional(readOnly = true)
    public String getShareFilename(String shareId) {
        ClientSvgShare share = clientSvgShareRepository.findByShareId(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Lượt chia sẻ không tồn tại", ErrorCodes.CLIENT_SHARE_NOT_FOUND));
        return share.getUserSvgFile().getOriginalFileName() != null
                ? share.getUserSvgFile().getOriginalFileName()
                : share.getUserSvgFile().getFileName();
    }

    private ClientSvgShare findAndValidateToken(String shareId, String shareToken, HttpServletRequest httpRequest, String action) {
        ClientSvgShare share = clientSvgShareRepository.findByShareId(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Lượt chia sẻ không tồn tại", ErrorCodes.CLIENT_SHARE_NOT_FOUND));

        String clientB = resolveClientBIdentifier(httpRequest);
        String requestId = resolveRequestId(httpRequest);

        if (share.isRevoked()) {
            auditLogService.log(
                    clientB,
                    "CLIENT",
                    "CLIENT_SHARE_" + action + "_FAILED",
                    "ClientSvgShare",
                    share.getId(),
                    String.format("shareId=%s, clientB=%s, reason=SHARE_REVOKED, requestId=%s, result=FAILED",
                            shareId, clientB, requestId)
            );
            throw new ForbiddenException("Lượt chia sẻ đã bị thu hồi bởi người tạo", ErrorCodes.CLIENT_SHARE_REVOKED);
        }

        if (share.isExpired()) {
            auditLogService.log(
                    clientB,
                    "CLIENT",
                    "CLIENT_SHARE_" + action + "_FAILED",
                    "ClientSvgShare",
                    share.getId(),
                    String.format("shareId=%s, clientB=%s, reason=SHARE_EXPIRED, requestId=%s, result=FAILED",
                            shareId, clientB, requestId)
            );
            throw new ForbiddenException("Lượt chia sẻ đã hết hạn sử dụng", ErrorCodes.CLIENT_SHARE_EXPIRED);
        }

        if (share.getOwnerUser() != null && (!share.getOwnerUser().isEnabled() || share.getOwnerUser().isDeleted() || (share.getOwnerUser().getRole() != Role.ADMIN && share.getOwnerUser().isExpired()))) {
            throw new ForbiddenException("Tài khoản người chia sẻ đã hết hạn hoặc bị vô hiệu hóa", ErrorCodes.USER_ACCOUNT_EXPIRED);
        }

        if (!clientShareTokenService.validateToken(shareToken, share)) {
            auditLogService.log(
                    clientB,
                    "CLIENT",
                    "CLIENT_SHARE_" + action + "_FAILED",
                    "ClientSvgShare",
                    share.getId(),
                    String.format("shareId=%s, clientB=%s, reason=INVALID_SHARE_TOKEN, requestId=%s, result=FAILED",
                            shareId, clientB, requestId)
            );
            throw new ForbiddenException("Mã xác thực chia sẻ không hợp lệ hoặc đã bị vô hiệu", ErrorCodes.CLIENT_SHARE_TOKEN_INVALID);
        }

        return share;
    }

    private ClientShareResponse toResponse(ClientSvgShare share) {
        ClientShareResponse resp = new ClientShareResponse();
        resp.setShareId(share.getShareId());
        resp.setFileId(share.getUserSvgFile().getId());
        resp.setFileName(share.getUserSvgFile().getFileName());
        resp.setOriginalFileName(share.getUserSvgFile().getOriginalFileName());
        resp.setFileSize(share.getUserSvgFile().getFileSize());
        resp.setStatus(share.getStatus());
        resp.setNote(share.getNote());
        resp.setExpiresAt(share.getExpiresAt());
        resp.setVerifyCount(share.getVerifyCount());
        resp.setPreviewCount(share.getPreviewCount());
        resp.setDownloadCount(share.getDownloadCount());
        resp.setLastAccessedAt(share.getLastAccessedAt());
        resp.setCreatedAt(share.getCreatedAt());
        resp.setRevokedAt(share.getRevokedAt());
        resp.setRevokedByUsername(share.getRevokedByUsername());
        resp.setRevokeReason(share.getRevokeReason());

        if (share.getOwnerUser() != null) {
            resp.setCreatedBy(new ClientShareResponse.CreatedByDto(
                    share.getOwnerUser().getId(),
                    share.getOwnerUser().getUsername(),
                    share.getOwnerUser().getFullName()
            ));
        }

        return resp;
    }

    /**
     * Sinh Share ID theo quy tắc: ddMMyyyy (Asia/Ho_Chi_Minh) + 6 ký tự random cryptographic (A-Z, a-z, 0-9).
     * Ví dụ: 01112026Ab1234, 02112026Xy7890.
     * Tự động kiểm tra và retry nếu phát hiện trùng lặp.
     */
    private String generateUniqueShareId() {
        String datePrefix = LocalDateTime.now(VIETNAM_ZONE).format(DATE_FORMATTER);

        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(datePrefix);
            for (int i = 0; i < 6; i++) {
                sb.append(RANDOM_ALPHANUMERIC.charAt(SECURE_RANDOM.nextInt(RANDOM_ALPHANUMERIC.length())));
            }
            String candidate = sb.toString();
            if (!clientSvgShareRepository.existsByShareId(candidate)) {
                return candidate;
            }
            log.warn("[SHARE_ID_COLLISION] Candidate '{}' collided, retrying... (attempt={})", candidate, attempt + 1);
        }

        // Fallback hiếm hoi nếu collision liên tục
        return datePrefix + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    private String resolveClientId(HttpServletRequest httpRequest) {
        if (httpRequest != null) {
            String clientHeader = httpRequest.getHeader("X-Client-ID");
            if (clientHeader != null && !clientHeader.isBlank()) {
                return clientHeader.trim();
            }
        }
        return "cutting-client";
    }

    private String resolveClientBIdentifier(HttpServletRequest httpRequest) {
        Optional<User> userOpt = currentUserService.getOptionalCurrentUser();
        if (userOpt.isPresent()) {
            return userOpt.get().getUsername();
        }
        if (httpRequest != null) {
            String clientHeader = httpRequest.getHeader("X-Client-ID");
            if (clientHeader != null && !clientHeader.isBlank()) {
                return clientHeader;
            }
            String forwarded = httpRequest.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return "client-ip:" + forwarded.split(",")[0].trim();
            }
            return "client-ip:" + httpRequest.getRemoteAddr();
        }
        return "ANONYMOUS";
    }

    private String resolveRequestId(HttpServletRequest httpRequest) {
        if (httpRequest != null) {
            String header = httpRequest.getHeader("X-Request-ID");
            if (header != null && !header.isBlank()) {
                return header.trim();
            }
        }
        String mdc = MDC.get("requestId");
        if (mdc != null && !mdc.isBlank()) {
            return mdc;
        }
        return UUID.randomUUID().toString();
    }
}
