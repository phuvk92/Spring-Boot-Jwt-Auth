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
import com.example.svgmanager.exception.UnauthorizedException;
import com.example.svgmanager.repository.CutJobRepository;
import com.example.svgmanager.repository.UserDeviceRepository;
import com.example.svgmanager.repository.WorkDesignRepository;
import com.example.svgmanager.security.CurrentUserService;
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
    private final CurrentUserService currentUserService;

    private final Map<String, IdempotentRecord> idempotencyCache = new ConcurrentHashMap<>();

    private record IdempotentRecord(CutJobResponse response, long expiresAtMillis) {
    }

    public CutHistoryServiceImpl(CutJobRepository cutJobRepository,
                                 UserDeviceRepository userDeviceRepository,
                                 WorkDesignRepository workDesignRepository,
                                 CurrentUserService currentUserService) {
        this.cutJobRepository = cutJobRepository;
        this.userDeviceRepository = userDeviceRepository;
        this.workDesignRepository = workDesignRepository;
        this.currentUserService = currentUserService;
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
        return dto;
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
        // Có designId thuộc chính user này → đặt work_designs.has_been_cut = true
        // designId không thuộc user (hoặc không tồn tại) → bỏ liên kết (lưu lượt cắt với design_id = null, design_version = null)
        String effectiveDesignId = null;
        Integer effectiveDesignVersion = null;

        if (StringUtils.hasText(request.getDesignId())) {
            String candidateKey = request.getDesignId().trim();
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

        if (cacheKey != null) {
            idempotencyCache.put(cacheKey, new IdempotentRecord(response, System.currentTimeMillis() + IDEMPOTENCY_TTL_MILLIS));
        }

        return response;
    }
}

