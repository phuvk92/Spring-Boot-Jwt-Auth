package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.request.RecordCutRequest;
import com.example.svgmanager.dto.response.CutHistoryResponse;
import com.example.svgmanager.dto.response.CutJobResponse;
import com.example.svgmanager.dto.response.CutStatsResponse;
import com.example.svgmanager.entity.CutJob;
import com.example.svgmanager.entity.CutOutcome;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.UserDevice;
import com.example.svgmanager.entity.WorkDesign;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.exception.UnauthorizedException;
import com.example.svgmanager.repository.CutJobRepository;
import com.example.svgmanager.repository.UserDeviceRepository;
import com.example.svgmanager.repository.UserSvgFileRepository;
import com.example.svgmanager.repository.WorkDesignRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.CutHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CutHistoryServiceImpl implements CutHistoryService {

    private static final Logger log = LoggerFactory.getLogger(CutHistoryServiceImpl.class);

    private static final long IDEMPOTENCY_TTL_MILLIS = 24L * 60 * 60 * 1000; // 24 hours (CL-42)

    private final CutJobRepository cutJobRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final WorkDesignRepository workDesignRepository;
    private final UserSvgFileRepository userSvgFileRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    private final Map<String, IdempotentRecord> idempotencyCache = new ConcurrentHashMap<>();

    private record IdempotentRecord(CutJobResponse response, long expiresAtMillis) {
    }

    public CutHistoryServiceImpl(CutJobRepository cutJobRepository,
                                 UserDeviceRepository userDeviceRepository,
                                 WorkDesignRepository workDesignRepository,
                                 UserSvgFileRepository userSvgFileRepository,
                                 CurrentUserService currentUserService,
                                 AuditLogService auditLogService) {
        this.cutJobRepository = cutJobRepository;
        this.userDeviceRepository = userDeviceRepository;
        this.workDesignRepository = workDesignRepository;
        this.userSvgFileRepository = userSvgFileRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public CutHistoryResponse getHistoryForCurrentDevice() {
        String sessionId = currentUserService.getCurrentJwt()
                .map(jwt -> jwt.getClaimAsString("sid"))
                .orElse(null);

        UserDevice device = (sessionId == null || sessionId.isBlank())
                ? null
                : userDeviceRepository.findByKeycloakSessionId(sessionId).orElse(null);

        List<CutJob> rows;
        if (device == null) {
            // Token không gắn máy nào → lịch sử rỗng, KHÔNG suy rộng ra cả tài khoản:
            // suy theo user sẽ lộ bản ghi của máy khác, sai scope "trên máy này".
            rows = List.of();
        } else {
            rows = cutJobRepository.findByUserDeviceIdOrderByCutAtDesc(device.getId());
        }

        List<CutJobResponse> jobs = rows.stream().map(this::toDto).toList();
        return new CutHistoryResponse(toStats(rows), jobs);
    }

    @Override
    @Transactional(readOnly = true)
    public CutJobResponse getJobForCurrentDevice(Long id) {
        String sessionId = currentUserService.getCurrentJwt()
                .map(jwt -> jwt.getClaimAsString("sid"))
                .orElse(null);

        UserDevice device = (sessionId == null || sessionId.isBlank())
                ? null
                : userDeviceRepository.findByKeycloakSessionId(sessionId).orElse(null);

        // Cùng scope "trên máy này" với danh sách: job của máy khác (kể cả cùng tài khoản)
        // trả 404 như job không tồn tại — không để lộ id hợp lệ của máy khác.
        return (device == null ? java.util.Optional.<CutJob>empty()
                : cutJobRepository.findByIdAndUserDeviceId(id, device.getId()))
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt cắt trên máy này.", "CUT_NOT_FOUND"));
    }

    private CutJobResponse toDto(CutJob j) {
        CutJobResponse dto = new CutJobResponse();
        dto.setId(j.getId());
        dto.setAt(j.getCutAt());
        // Lazy proxy an toàn trong transaction readOnly — chỉ đọc tên máy.
        UserDevice d = j.getUserDevice();
        dto.setDeviceName(d != null ? d.getDeviceName() : null);
        dto.setPartLabel(j.getPartLabel());
        dto.setVehicleLabel(j.getVehicleLabel());
        dto.setFilmUsage(j.getFilmUsage());
        dto.setDuration(j.getDuration());
        dto.setOutcome(j.getOutcome() != null ? j.getOutcome().contractValue() : null);
        dto.setDesignId(j.getDesignId());
        dto.setDesignVersion(j.getDesignVersion());

        // Cờ designAvailable: khi có designId, kiểm tra xem bản lưu có còn tồn tại và không bị xoá (status != DELETED)
        if (StringUtils.hasText(j.getDesignId())) {
            boolean available = isDesignAvailable(j.getDesignId().trim());
            dto.setDesignAvailable(available);
        } else {
            dto.setDesignAvailable(null);
        }

        return dto;
    }

    private boolean isDesignAvailable(String designId) {
        try {
            Long userFileId = Long.parseLong(designId);
            var opt = userSvgFileRepository.findById(userFileId);
            if (opt.isPresent()) {
                return !"DELETED".equalsIgnoreCase(opt.get().getStatus());
            }
        } catch (NumberFormatException ignored) {
        }
        return workDesignRepository.existsByDesignKey(designId);
    }

    private CutStatsResponse toStats(List<CutJob> rows) {
        int jobCount = rows.size();
        int recutCount = (int) rows.stream().filter(j -> j.getOutcome() == CutOutcome.RECUT).count();
        int vehicleCount = (int) rows.stream()
                .map(CutJob::getVehicleLabel)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        // filmUsed: chỉ tổng hợp khi có số đo nguồn; A6a chưa chốt đại lượng đo nên
        // thiếu dữ liệu → null (fail-closed về số tính tiền), không đoán từ chuỗi nhãn.
        BigDecimal meters = rows.stream()
                .map(CutJob::getFilmUsageMeters)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean anyMeters = rows.stream().anyMatch(j -> j.getFilmUsageMeters() != null);
        String filmUsed = anyMeters ? formatMeters(meters) : null;

        String period = rows.isEmpty() ? null : formatPeriod(
                rows.stream().map(CutJob::getCutAt).filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(null),
                rows.stream().map(CutJob::getCutAt).filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null));

        return new CutStatsResponse(jobCount, filmUsed, recutCount, vehicleCount, period);
    }

    /** "212 m" — dấu phẩy thập phân theo kiểu nhãn hiển thị khắp hợp đồng. */
    private static String formatMeters(BigDecimal meters) {
        return meters.stripTrailingZeros().toPlainString().replace('.', ',') + " m";
    }

    /** "10/08 – 16/08/2026" (cùng tháng-năm gộp nhãn đầu), khác thì ghi đủ hai đầu. */
    private static String formatPeriod(LocalDateTime min, LocalDateTime max) {
        if (min == null || max == null) {
            return null;
        }
        DateTimeFormatter full = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);
        if (min.getYear() == max.getYear() && min.getMonth() == max.getMonth()) {
            return min.format(DateTimeFormatter.ofPattern("dd/MM", Locale.ROOT))
                    + " – " + max.format(full);
        }
        return min.format(full) + " – " + max.format(full);
    }

    @Override
    @Transactional
    public CutJobResponse recordCut(RecordCutRequest request, String idempotencyKey) {
        String sessionId = currentUserService.getCurrentJwt()
                .map(jwt -> jwt.getClaimAsString("sid"))
                .orElse(null);

        UserDevice device = (sessionId == null || sessionId.isBlank())
                ? null
                : userDeviceRepository.findByKeycloakSessionId(sessionId).orElse(null);

        if (device == null || !device.isActive()) {
            throw new UnauthorizedException("Máy này đã bị gỡ khỏi tài khoản hoặc phiên không còn hiệu lực. Vui lòng đăng nhập lại.",
                    ErrorCodes.SESSION_REVOKED);
        }

        // Kiểm tra Idempotency-Key (CL-42): cùng key + cùng máy trong 24 giờ
        String cacheKey = (StringUtils.hasText(idempotencyKey))
                ? device.getId() + ":" + idempotencyKey.trim()
                : null;

        if (cacheKey != null) {
            IdempotentRecord cached = idempotencyCache.get(cacheKey);
            long now = System.currentTimeMillis();
            if (cached != null && cached.expiresAtMillis() > now) {
                return cached.response();
            }
        }

        User currentUser = currentUserService.getCurrentUser();

        // Xử lý designId:
        // Chấp nhận khi là id user_svg_files thuộc chính user (ưu tiên — board 04/10 hướng phuvk).
        // Vẫn nhận work_designs của user như cũ.
        // Không thuộc user (hoặc không tồn tại) → bỏ liên kết (design_id = null, design_version = null).
        String effectiveDesignId = null;
        Integer effectiveDesignVersion = null;

        if (StringUtils.hasText(request.getDesignId())) {
            String candidateKey = request.getDesignId().trim();
            // 1. Ưu tiên kiểm tra user_svg_files thuộc chính user (ID thường là dạng số)
            Long userFileId = null;
            try {
                userFileId = Long.parseLong(candidateKey);
            } catch (NumberFormatException ignored) {
            }

            boolean matched = false;
            if (userFileId != null) {
                var userSvgFile = userSvgFileRepository.findByIdAndUserId(userFileId, currentUser.getId()).orElse(null);
                if (userSvgFile != null && !"DELETED".equalsIgnoreCase(userSvgFile.getStatus())) {
                    effectiveDesignId = candidateKey;
                    effectiveDesignVersion = request.getDesignVersion();
                    matched = true;
                }
            }

            // 2. Nếu chưa match user_svg_files, kiểm tra work_designs của user như cũ
            if (!matched) {
                WorkDesign workDesign = workDesignRepository.findByDesignKeyAndOwnerId(candidateKey, currentUser.getId()).orElse(null);
                if (workDesign != null) {
                    effectiveDesignId = candidateKey;
                    effectiveDesignVersion = request.getDesignVersion();
                    if (!workDesign.isHasBeenCut()) {
                        workDesign.setHasBeenCut(true);
                        workDesignRepository.save(workDesign);
                    }
                }
            }
        }

        // Tự động xác định outcome:
        // server tự đặt RECUT khi cùng máy đã có lượt cắt cùng designId trước đó, còn lại COMPLETED
        CutOutcome outcome = CutOutcome.COMPLETED;
        if (effectiveDesignId != null && cutJobRepository.existsByUserDeviceIdAndDesignId(device.getId(), effectiveDesignId)) {
            outcome = CutOutcome.RECUT;
        }

        CutJob job = new CutJob();
        job.setUserDevice(device);
        job.setCutAt(request.getCutAt() != null ? request.getCutAt() : LocalDateTime.now());
        job.setPartLabel(request.getPartLabel());
        job.setVehicleLabel(request.getVehicleLabel());
        job.setFilmUsage(request.getFilmUsage());
        job.setFilmUsageMeters(request.getFilmUsageMeters());
        job.setDuration(request.getDuration());
        job.setOutcome(outcome);
        job.setDesignId(effectiveDesignId);
        job.setDesignVersion(effectiveDesignVersion);
        job.setCreatedAt(LocalDateTime.now());

        CutJob saved = cutJobRepository.save(job);
        CutJobResponse response = toDto(saved);

        // Audit nghiệp vụ: interceptor chỉ ghi "POST /api/v1/cuts" + HTTP status, nên ghi thêm
        // một dòng mang nội dung lượt cắt để admin tra ở trang Audit. Lượt gửi lại cùng
        // Idempotency-Key đã return ở trên nên không ghi đôi.
        auditLogService.log(currentUser.getUsername(),
                currentUser.getRole() != null ? currentUser.getRole().name() : "USER",
                "CUT_RECORDED", "CutJob", saved.getId(), auditDetails(saved, device));

        if (cacheKey != null) {
            idempotencyCache.put(cacheKey, new IdempotentRecord(response, System.currentTimeMillis() + IDEMPOTENCY_TTL_MILLIS));
        }

        return response;
    }

    /**
     * Chi tiết lượt cắt cho cột audit_logs.details — chỉ số liệu như DTO, KHÔNG hình học (F-38 · RB-01).
     * Trường thiếu ghi "-" để các dòng audit cùng một khuôn, dễ đọc và dễ tìm theo từ khoá.
     */
    private static String auditDetails(CutJob j, UserDevice device) {
        String meters = j.getFilmUsageMeters() != null
                ? " (" + j.getFilmUsageMeters().stripTrailingZeros().toPlainString() + " m)"
                : "";
        String design = StringUtils.hasText(j.getDesignId())
                ? j.getDesignId() + (j.getDesignVersion() != null ? " v" + j.getDesignVersion() : "")
                : "-";
        return "Xe: " + orDash(j.getVehicleLabel())
                + " | Chi tiết: " + orDash(j.getPartLabel())
                + " | Phim: " + orDash(j.getFilmUsage()) + meters
                + " | Thời lượng: " + orDash(j.getDuration())
                + " | Kết quả: " + j.getOutcome().name()
                + " | Bản thiết kế: " + design
                + " | Máy: " + orDash(device.getDeviceName()) + " [" + device.getDeviceId() + "]"
                + " | Cắt lúc: " + j.getCutAt();
    }

    private static String orDash(String s) {
        return StringUtils.hasText(s) ? s : "-";
    }
}

