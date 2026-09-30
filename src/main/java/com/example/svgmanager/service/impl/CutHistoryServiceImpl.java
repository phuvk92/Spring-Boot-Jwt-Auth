package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.response.CutHistoryResponse;
import com.example.svgmanager.dto.response.CutJobResponse;
import com.example.svgmanager.dto.response.CutStatsResponse;
import com.example.svgmanager.entity.CutJob;
import com.example.svgmanager.entity.CutOutcome;
import com.example.svgmanager.entity.UserDevice;
import com.example.svgmanager.repository.CutJobRepository;
import com.example.svgmanager.repository.UserDeviceRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.CutHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class CutHistoryServiceImpl implements CutHistoryService {

    private static final Logger log = LoggerFactory.getLogger(CutHistoryServiceImpl.class);

    private final CutJobRepository cutJobRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final CurrentUserService currentUserService;

    public CutHistoryServiceImpl(CutJobRepository cutJobRepository,
                                 UserDeviceRepository userDeviceRepository,
                                 CurrentUserService currentUserService) {
        this.cutJobRepository = cutJobRepository;
        this.userDeviceRepository = userDeviceRepository;
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
}
