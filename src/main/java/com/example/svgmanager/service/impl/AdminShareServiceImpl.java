package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.internal.v2.ClientShareResponse;
import com.example.svgmanager.dto.internal.v2.RevokeClientShareRequest;
import com.example.svgmanager.dto.request.AdminRevokeShareRequest;
import com.example.svgmanager.dto.response.AdminShareActivityResponse;
import com.example.svgmanager.dto.response.AdminShareItemResponse;
import com.example.svgmanager.dto.response.AdminShareStatsResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.entity.AuditLog;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.UserSvgFileShare;
import com.example.svgmanager.entity.v2.AuditLogV2;
import com.example.svgmanager.entity.v2.ClientSvgShare;
import com.example.svgmanager.entity.v2.UnifiedShare;
import com.example.svgmanager.entity.v2.UserSvgFileV2Share;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.exception.UnauthorizedException;
import com.example.svgmanager.repository.AuditLogRepository;
import com.example.svgmanager.repository.UserSvgFileShareRepository;
import com.example.svgmanager.repository.internal.v2.AuditLogV2Repository;
import com.example.svgmanager.repository.internal.v2.ClientSvgShareRepository;
import com.example.svgmanager.repository.internal.v2.UnifiedShareRepository;
import com.example.svgmanager.repository.internal.v2.UnifiedShareSpecification;
import com.example.svgmanager.repository.internal.v2.UserSvgFileV2ShareRepository;
import com.example.svgmanager.service.AdminShareService;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.internal.v2.ClientSvgShareService;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AdminShareServiceImpl implements AdminShareService {

    private static final Logger log = LoggerFactory.getLogger(AdminShareServiceImpl.class);

    private final UnifiedShareRepository unifiedShareRepository;
    private final ClientSvgShareRepository clientSvgShareRepository;
    private final ClientSvgShareService clientSvgShareService;
    private final UserSvgFileV2ShareRepository userSvgFileV2ShareRepository;
    private final UserSvgFileShareRepository userSvgFileShareRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogV2Repository auditLogV2Repository;
    private final AuditLogService auditLogService;
    private final jakarta.persistence.EntityManager entityManager;

    public AdminShareServiceImpl(
            UnifiedShareRepository unifiedShareRepository,
            ClientSvgShareRepository clientSvgShareRepository,
            ClientSvgShareService clientSvgShareService,
            UserSvgFileV2ShareRepository userSvgFileV2ShareRepository,
            UserSvgFileShareRepository userSvgFileShareRepository,
            AuditLogRepository auditLogRepository,
            AuditLogV2Repository auditLogV2Repository,
            AuditLogService auditLogService,
            jakarta.persistence.EntityManager entityManager
    ) {
        this.unifiedShareRepository = unifiedShareRepository;
        this.clientSvgShareRepository = clientSvgShareRepository;
        this.clientSvgShareService = clientSvgShareService;
        this.userSvgFileV2ShareRepository = userSvgFileV2ShareRepository;
        this.userSvgFileShareRepository = userSvgFileShareRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditLogV2Repository = auditLogV2Repository;
        this.auditLogService = auditLogService;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminShareItemResponse> listShares(
            String shareType,
            String status,
            String keyword,
            Long dealerId,
            Long fileId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        Specification<UnifiedShare> spec = UnifiedShareSpecification.filter(
                shareType, status, keyword, dealerId, fileId, from, to
        );

        Page<UnifiedShare> page = unifiedShareRepository.findAll(spec, pageable);
        List<AdminShareItemResponse> dtos = page.getContent().stream()
                .map(this::toItemResponse)
                .toList();

        return PageResponse.<AdminShareItemResponse>builder()
                .content(dtos)
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
    public AdminShareItemResponse getShareDetail(String shareId) {
        if (!StringUtils.hasText(shareId)) {
            throw new ResourceNotFoundException("Mã chia sẻ không được để trống", ErrorCodes.CLIENT_SHARE_NOT_FOUND);
        }

        UnifiedShare share = unifiedShareRepository.findByShareId(shareId.trim())
                .or(() -> unifiedShareRepository.findById(shareId.trim()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt chia sẻ: " + shareId, ErrorCodes.CLIENT_SHARE_NOT_FOUND));

        return toItemResponse(share);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminShareActivityResponse> getShareHistory(String shareId) {
        if (!StringUtils.hasText(shareId)) {
            return Collections.emptyList();
        }

        String searchShareId = shareId.trim();
        List<AdminShareActivityResponse> list = new ArrayList<>();

        // 1. Tìm trong audit_logs với pattern shareId
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.like(root.get("action"), "%SHARE%"));
            preds.add(cb.like(cb.lower(root.get("details")), "%" + searchShareId.toLowerCase() + "%"));
            return cb.and(preds.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> auditLogs = auditLogRepository.findAll(spec, pageable);

        for (AuditLog logItem : auditLogs.getContent()) {
            list.add(mapAuditLogToActivity(logItem, searchShareId));
        }

        // 2. Tìm trong audit_log_v2 cho các endpoint gọi tới shareId này
        try {
            Pageable v2Pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<AuditLogV2> v2Logs = auditLogV2Repository.findAll(
                    (root, query, cb) -> cb.like(root.get("endpoint"), "%" + searchShareId + "%"),
                    v2Pageable
            );
            for (AuditLogV2 v2Item : v2Logs.getContent()) {
                list.add(mapAuditLogV2ToActivity(v2Item, searchShareId));
            }
        } catch (Exception e) {
            log.warn("Failed to query audit_log_v2 for shareId={}: {}", searchShareId, e.getMessage());
        }

        // Sắp xếp giảm dần theo thời gian và khử trùng nếu có
        list.sort((a, b) -> {
            if (a.getTimestamp() == null || b.getTimestamp() == null) return 0;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminShareActivityResponse> listActivityHistory(
            String shareId,
            Long fileId,
            String action,
            String result,
            String keyword,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.like(root.get("action"), "%SHARE%"));

            if (StringUtils.hasText(shareId)) {
                preds.add(cb.like(cb.lower(root.get("details")), "%shareid=" + shareId.trim().toLowerCase() + "%"));
            }

            if (fileId != null) {
                preds.add(cb.like(root.get("details"), "%fileid=" + fileId + "%"));
            }

            if (StringUtils.hasText(action) && !"ALL".equalsIgnoreCase(action.trim())) {
                preds.add(cb.like(cb.upper(root.get("action")), "%" + action.trim().toUpperCase() + "%"));
            }

            if (StringUtils.hasText(result) && !"ALL".equalsIgnoreCase(result.trim())) {
                preds.add(cb.like(cb.upper(root.get("details")), "%RESULT=" + result.trim().toUpperCase() + "%"));
            }

            if (from != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            }

            if (to != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
            }

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate actorPred = cb.like(cb.lower(root.get("actor")), pattern);
                Predicate detailsPred = cb.like(cb.lower(root.get("details")), pattern);
                Predicate actionPred = cb.like(cb.lower(root.get("action")), pattern);
                preds.add(cb.or(actorPred, detailsPred, actionPred));
            }

            return cb.and(preds.toArray(new Predicate[0]));
        };

        Page<AuditLog> page = auditLogRepository.findAll(spec, pageable);
        List<AdminShareActivityResponse> dtos = page.getContent().stream()
                .map(logItem -> mapAuditLogToActivity(logItem, null))
                .toList();

        return PageResponse.<AdminShareActivityResponse>builder()
                .content(dtos)
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
    public AdminShareStatsResponse getStatistics() {
        AdminShareStatsResponse stats = new AdminShareStatsResponse();

        stats.setTotalShares(unifiedShareRepository.count());
        stats.setAdminShares(unifiedShareRepository.countByShareType("ADMIN_TO_CLIENT"));
        stats.setClientShares(unifiedShareRepository.countByShareType("CLIENT_TO_CLIENT"));
        stats.setActiveShares(unifiedShareRepository.countByStatus("ACTIVE"));
        stats.setRevokedShares(unifiedShareRepository.countByStatus("REVOKED"));
        stats.setExpiredShares(unifiedShareRepository.countByStatus("EXPIRED"));

        stats.setTotalPreviews(unifiedShareRepository.sumTotalPreviews());
        stats.setTotalDownloads(unifiedShareRepository.sumTotalDownloads());
        stats.setTotalVerifies(unifiedShareRepository.sumTotalVerifies());
        stats.setActiveClients(unifiedShareRepository.countActiveClients());

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        long todayCount = unifiedShareRepository.count(
                (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), startOfToday)
        );
        stats.setSharesCreatedToday(todayCount);

        long monthCount = unifiedShareRepository.count(
                (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), startOfMonth)
        );
        stats.setSharesCreatedThisMonth(monthCount);

        return stats;
    }

    @Override
    @Transactional
    public AdminShareItemResponse revokeShare(
            User currentUser,
            String shareId,
            AdminRevokeShareRequest request,
            HttpServletRequest httpRequest
    ) {
        if (currentUser == null) {
            throw new UnauthorizedException("Chưa xác thực người dùng");
        }
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền thu hồi lượt chia sẻ này", ErrorCodes.CLIENT_SHARE_FORBIDDEN);
        }

        String targetShareId = shareId != null ? shareId.trim() : "";
        String reason = (request != null && StringUtils.hasText(request.getReason()))
                ? request.getReason().trim()
                : "Thu hồi bởi Quản trị viên (Admin)";

        // 1. Kiểm tra Client-to-Client Share
        Optional<ClientSvgShare> clientShareOpt = clientSvgShareRepository.findByShareId(targetShareId);
        if (clientShareOpt.isPresent()) {
            ClientSvgShare share = clientShareOpt.get();
            clientSvgShareService.revokeShare(currentUser, share.getShareId(), reason, httpRequest);
            if (entityManager != null) { entityManager.flush(); }
            return getShareDetail(share.getShareId());
        }

        // 2. Kiểm tra V2 Direct Share (DIR-V2-{id} hoặc id dạng số)
        Long directV2Id = parseDirectId(targetShareId, "DIR-V2-");
        if (directV2Id != null) {
            Optional<UserSvgFileV2Share> v2ShareOpt = userSvgFileV2ShareRepository.findById(directV2Id);
            if (v2ShareOpt.isPresent()) {
                UserSvgFileV2Share v2Share = v2ShareOpt.get();
                v2Share.setStatus("REVOKED");
                v2Share.setRevokedAt(LocalDateTime.now());
                v2Share.setRevokedById(currentUser.getId());
                v2Share.setRevokeReason(reason);
                v2Share.setUpdatedAt(LocalDateTime.now());
                userSvgFileV2ShareRepository.save(v2Share);

                auditLogService.log(
                        currentUser.getUsername(),
                        currentUser.getRole() != null ? currentUser.getRole().name() : "ADMIN",
                        "USER_FILE_V2_SHARE_REVOKED",
                        "UserSvgFileV2Share",
                        v2Share.getId(),
                        String.format("fileId=%d, targetUserId=%d, revokedByUserId=%d, reason=%s",
                                v2Share.getUserSvgFile().getId(), v2Share.getSharedToUser().getId(), currentUser.getId(), reason)
                );
                if (entityManager != null) { entityManager.flush(); }
                return getShareDetail("DIR-V2-" + v2Share.getId());
            }
        }

        // 3. Kiểm tra V1 Direct Share (DIR-V1-{id})
        Long directV1Id = parseDirectId(targetShareId, "DIR-V1-");
        if (directV1Id != null) {
            Optional<UserSvgFileShare> v1ShareOpt = userSvgFileShareRepository.findById(directV1Id);
            if (v1ShareOpt.isPresent()) {
                UserSvgFileShare v1Share = v1ShareOpt.get();
                v1Share.setStatus("REVOKED");
                v1Share.setRevokedAt(LocalDateTime.now());
                v1Share.setRevokedById(currentUser.getId());
                v1Share.setRevokeReason(reason);
                v1Share.setUpdatedAt(LocalDateTime.now());
                userSvgFileShareRepository.save(v1Share);

                auditLogService.log(
                        currentUser.getUsername(),
                        currentUser.getRole() != null ? currentUser.getRole().name() : "ADMIN",
                        "USER_FILE_SHARE_REVOKED",
                        "UserSvgFileShare",
                        v1Share.getId(),
                        String.format("fileId=%d, targetUserId=%d, revokedByUserId=%d, reason=%s",
                                v1Share.getUserSvgFile().getId(), v1Share.getSharedToUser().getId(), currentUser.getId(), reason)
                );
                if (entityManager != null) { entityManager.flush(); }
                return getShareDetail("DIR-V1-" + v1Share.getId());
            }
        }

        // Không tìm thấy
        throw new ResourceNotFoundException("Không tìm thấy lượt chia sẻ tương ứng với mã: " + shareId, ErrorCodes.CLIENT_SHARE_NOT_FOUND);
    }

    private Long parseDirectId(String text, String prefix) {
        if (!StringUtils.hasText(text)) return null;
        if (text.startsWith(prefix)) {
            try {
                return Long.parseLong(text.substring(prefix.length()));
            } catch (NumberFormatException ignored) {}
        }
        if (text.matches("^\\d+$")) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private AdminShareItemResponse toItemResponse(UnifiedShare entity) {
        AdminShareItemResponse dto = new AdminShareItemResponse();
        dto.setUid(entity.getUid());
        dto.setOriginalId(entity.getOriginalId());
        dto.setShareId(entity.getShareId());
        dto.setShareType(entity.getShareType());
        dto.setFileId(entity.getFileId());
        dto.setFileName(entity.getFileName());
        dto.setCategoryName(entity.getCategoryName());

        if (entity.getSharedByUserId() != null) {
            dto.setSharedBy(new AdminShareItemResponse.UserSummaryDto(
                    entity.getSharedByUserId(),
                    entity.getSharedByUsername(),
                    entity.getSharedByFullName(),
                    entity.getSharedByRole(),
                    entity.getSharedByDealerId(),
                    entity.getSharedByDealerName()
            ));
        }

        if (entity.getSharedToUserId() != null) {
            dto.setRecipient(new AdminShareItemResponse.UserSummaryDto(
                    entity.getSharedToUserId(),
                    entity.getSharedToUsername(),
                    entity.getSharedToFullName(),
                    entity.getSharedToRole(),
                    entity.getSharedToDealerId(),
                    entity.getSharedToDealerName()
            ));
        }

        dto.setOwnerClientId(entity.getOwnerClientId());
        dto.setStatus(entity.getStatus());
        dto.setExpiresAt(entity.getExpiresAt());
        dto.setRevokedAt(entity.getRevokedAt());

        if (entity.getRevokedById() != null || entity.getRevokedByUsername() != null) {
            dto.setRevokedBy(new AdminShareItemResponse.UserSummaryDto(
                    entity.getRevokedById(),
                    entity.getRevokedByUsername(),
                    null,
                    null,
                    null,
                    null
            ));
        }

        dto.setRevokeReason(entity.getRevokeReason());
        dto.setVerifyCount(entity.getVerifyCount());
        dto.setFailedVerifyCount(entity.getFailedVerifyCount());
        dto.setPreviewCount(entity.getPreviewCount());
        dto.setDownloadCount(entity.getDownloadCount());
        dto.setLastAccessedAt(entity.getLastAccessedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setVersion(entity.getVersion());

        return dto;
    }

    private AdminShareActivityResponse mapAuditLogToActivity(AuditLog auditLog, String defaultShareId) {
        AdminShareActivityResponse dto = new AdminShareActivityResponse();
        dto.setId(auditLog.getId());
        dto.setTimestamp(auditLog.getTimestamp());
        dto.setActorUsername(auditLog.getActor());
        dto.setActorRole(auditLog.getActorRole());
        dto.setDetails(auditLog.getDetails());

        String details = auditLog.getDetails() != null ? auditLog.getDetails() : "";
        dto.setShareId(extractParam(details, "shareId", defaultShareId));
        dto.setFileName(extractParam(details, "fileName", null));
        String fileIdStr = extractParam(details, "fileId", null);
        if (fileIdStr != null && fileIdStr.matches("^\\d+$")) {
            dto.setFileId(Long.parseLong(fileIdStr));
        }

        dto.setRequestId(extractParam(details, "requestId", null));
        dto.setReason(extractParam(details, "reason", null));
        dto.setClientRecipient(extractParam(details, "clientB", null));
        dto.setClientSource(extractParam(details, "owner", null));

        String rawAction = auditLog.getAction();
        if ("CLIENT_SHARE_CREATED".equalsIgnoreCase(rawAction) || "USER_FILE_SHARED".equalsIgnoreCase(rawAction) || "USER_FILE_V2_SHARED".equalsIgnoreCase(rawAction)) {
            dto.setAction("CREATE");
            dto.setActionDisplay("Tạo lượt chia sẻ");
            dto.setResult("SUCCESS");
        } else if ("CLIENT_SHARE_VERIFY_SUCCESS".equalsIgnoreCase(rawAction)) {
            dto.setAction("VERIFY");
            dto.setActionDisplay("Xác thực mật khẩu thành công");
            dto.setResult("SUCCESS");
        } else if ("CLIENT_SHARE_VERIFY_FAILED".equalsIgnoreCase(rawAction)) {
            dto.setAction("VERIFY");
            dto.setActionDisplay("Xác thực mật khẩu thất bại");
            dto.setResult("FAILED");
        } else if ("CLIENT_SHARE_PREVIEWED".equalsIgnoreCase(rawAction)) {
            dto.setAction("PREVIEW");
            dto.setActionDisplay("Xem trước file SVG");
            dto.setResult("SUCCESS");
        } else if ("CLIENT_SHARE_DOWNLOADED".equalsIgnoreCase(rawAction)) {
            dto.setAction("DOWNLOAD");
            dto.setActionDisplay("Tải xuống file SVG");
            dto.setResult("SUCCESS");
        } else if ("CLIENT_SHARE_REVOKED".equalsIgnoreCase(rawAction) || "USER_FILE_SHARE_REVOKED".equalsIgnoreCase(rawAction) || "USER_FILE_V2_SHARE_REVOKED".equalsIgnoreCase(rawAction)) {
            dto.setAction("REVOKE");
            dto.setActionDisplay("Thu hồi lượt chia sẻ");
            dto.setResult("SUCCESS");
        } else {
            dto.setAction(rawAction);
            dto.setActionDisplay(rawAction);
            dto.setResult("SUCCESS");
        }

        dto.setShareType(rawAction.startsWith("CLIENT_") ? "CLIENT_TO_CLIENT" : "ADMIN_TO_CLIENT");
        return dto;
    }

    private AdminShareActivityResponse mapAuditLogV2ToActivity(AuditLogV2 v2, String defaultShareId) {
        AdminShareActivityResponse dto = new AdminShareActivityResponse();
        dto.setId(v2.getId());
        dto.setTimestamp(v2.getCreatedAt());
        dto.setActorUsername(v2.getUsername());
        dto.setActorRole(v2.getRole());
        dto.setDealerName(v2.getDealerName());
        dto.setClientIp(v2.getClientIp());
        dto.setRequestId(v2.getRequestId());
        dto.setHttpStatus(v2.getHttpStatus());
        dto.setResult("SUCCESS".equalsIgnoreCase(v2.getResult()) ? "SUCCESS" : "FAILED");
        dto.setReason(v2.getErrorCode());
        dto.setClientRecipient(v2.getClientId());

        String ep = v2.getEndpoint() != null ? v2.getEndpoint() : "";
        dto.setShareId(defaultShareId);
        dto.setShareType("CLIENT_TO_CLIENT");

        if (ep.endsWith("/verify")) {
            dto.setAction("VERIFY");
            dto.setActionDisplay("Gọi API Xác thực mật khẩu");
        } else if (ep.endsWith("/preview")) {
            dto.setAction("PREVIEW");
            dto.setActionDisplay("Gọi API Xem trước file");
        } else if (ep.endsWith("/download")) {
            dto.setAction("DOWNLOAD");
            dto.setActionDisplay("Gọi API Tải xuống file");
        } else if (v2.getHttpMethod() != null && "DELETE".equalsIgnoreCase(v2.getHttpMethod())) {
            dto.setAction("REVOKE");
            dto.setActionDisplay("Gọi API Thu hồi chia sẻ");
        } else {
            dto.setAction("ACCESS");
            dto.setActionDisplay("Truy cập chia sẻ file");
        }

        return dto;
    }

    private String extractParam(String text, String key, String defaultVal) {
        if (!StringUtils.hasText(text)) return defaultVal;
        Pattern p = Pattern.compile(key + "=([^,]+)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return defaultVal;
    }
}
