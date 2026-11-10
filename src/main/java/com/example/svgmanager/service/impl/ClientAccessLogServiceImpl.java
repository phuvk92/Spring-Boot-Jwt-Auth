package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.request.AccessEventRequest;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.UserDevice;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.UserDeviceRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.ClientAccessLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
public class ClientAccessLogServiceImpl implements ClientAccessLogService {

    /** Client ghép detail của template_download là "{fileKey} · {tên file}" (AccessLog.cs). */
    private static final String DETAIL_SEPARATOR = " · ";

    private final CurrentUserService currentUserService;
    private final UserDeviceRepository userDeviceRepository;
    private final SvgFileRepository svgFileRepository;
    private final AuditLogService auditLogService;

    public ClientAccessLogServiceImpl(CurrentUserService currentUserService,
                                      UserDeviceRepository userDeviceRepository,
                                      SvgFileRepository svgFileRepository,
                                      AuditLogService auditLogService) {
        this.currentUserService = currentUserService;
        this.userDeviceRepository = userDeviceRepository;
        this.svgFileRepository = svgFileRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public void record(AccessEventRequest event, String clientIp) {
        User user = currentUserService.getCurrentUser();
        String role = user.getRole() != null ? user.getRole().name() : "USER";
        String kind = clean(event.getKind()).toLowerCase(Locale.ROOT);

        // Máy theo claim `sid` của token như lượt cắt (F-38) — không có thì vẫn ghi, chỉ thiếu tên máy.
        UserDevice device = currentUserService.getCurrentJwt()
                .map(jwt -> jwt.getClaimAsString("sid"))
                .filter(StringUtils::hasText)
                .flatMap(userDeviceRepository::findByKeycloakSessionId)
                .orElse(null);

        String machine = device != null
                ? orDash(device.getDeviceName()) + " [" + device.getDeviceId() + "]"
                : "-";
        String tail = " | Máy: " + machine
                + " | IP: " + orDash(clientIp)
                + " | Lúc (client): " + orDash(clean(event.getAt()));

        if ("template_download".equals(kind)) {
            String detail = clean(event.getDetail());
            int sep = detail.indexOf(DETAIL_SEPARATOR);
            String fileKey = sep >= 0 ? detail.substring(0, sep).trim() : detail;
            String clientName = sep >= 0 ? detail.substring(sep + DETAIL_SEPARATOR.length()).trim() : "";

            SvgFile file = StringUtils.hasText(fileKey)
                    ? svgFileRepository.findByFileKey(fileKey).orElse(null)
                    : null;
            String name = file != null ? displayName(file) : clientName;

            auditLogService.log(user.getUsername(), role, OPEN_FILE, "SvgFile",
                    file != null ? file.getId() : null,
                    "File: " + orDash(name) + " [" + orDash(fileKey) + "]"
                            + (file == null && StringUtils.hasText(fileKey) ? " (không có trong kho)" : "")
                            + " | Lượt mở trong phiên: " + orDash(event.getDownloadsInSession())
                            + tail);
            return;
        }

        // Loại khác (session_start, hoặc giá trị lạ) — hợp đồng 0.4.0: ghi nguyên văn, không từ chối.
        auditLogService.log(user.getUsername(), role, kind.toUpperCase(Locale.ROOT), "ClientSession", null,
                "Sự kiện app cắt: " + orDash(kind)
                        + (StringUtils.hasText(event.getDetail()) ? " | Chi tiết: " + clean(event.getDetail()) : "")
                        + tail);
    }

    private static String displayName(SvgFile f) {
        if (StringUtils.hasText(f.getDisplayName())) return f.getDisplayName();
        if (StringUtils.hasText(f.getOriginalFilename())) return f.getOriginalFilename();
        return f.getRawOriginalFilename();
    }

    /** Chuỗi do client gửi: bỏ xuống dòng (chống giả dòng log), cắt gọn. */
    private static String clean(String s) {
        if (s == null) return "";
        String one = s.replaceAll("[\\r\\n\\t]", " ").trim();
        return one.length() > 300 ? one.substring(0, 300) : one;
    }

    private static String orDash(Object o) {
        return o == null || (o instanceof String s && !StringUtils.hasText(s)) ? "-" : o.toString();
    }
}
