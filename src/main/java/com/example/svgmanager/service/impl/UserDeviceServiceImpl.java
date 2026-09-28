package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.response.UserDeviceResponse;
import com.example.svgmanager.entity.DeviceStatus;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.UserDevice;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.exception.UnauthorizedException;
import com.example.svgmanager.repository.UserDeviceRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.KeycloakUserService;
import com.example.svgmanager.service.UserDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserDeviceServiceImpl implements UserDeviceService {

    private static final Logger log = LoggerFactory.getLogger(UserDeviceServiceImpl.class);

    static final String SESSION_LIMIT_MESSAGE =
            "Tài khoản này đã được đăng ký trên một máy khác. Liên hệ quản trị đại lý để gỡ máy cũ rồi đăng nhập lại.";
    static final String SESSION_REVOKED_MESSAGE =
            "Máy này đã bị gỡ khỏi tài khoản hoặc phiên không còn hiệu lực. Vui lòng đăng nhập lại.";

    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;
    private final AuditLogService auditLogService;
    private final boolean enforced;
    private final int defaultMaxDevices;
    private final long sessionCacheMillis;

    /** sid → (còn hoạt động?, hết hạn lúc). Một instance backend nên cache cục bộ là đủ; gỡ máy thì xoá ngay. */
    private final Map<String, CachedSession> sessionCache = new ConcurrentHashMap<>();

    private record CachedSession(boolean active, long expiresAtMillis) {
    }

    public UserDeviceServiceImpl(
            UserDeviceRepository userDeviceRepository,
            UserRepository userRepository,
            KeycloakUserService keycloakUserService,
            AuditLogService auditLogService,
            @Value("${app.device.enforce:true}") boolean enforced,
            @Value("${app.device.max-per-user:1}") int defaultMaxDevices,
            @Value("${app.device.session-cache-seconds:30}") long sessionCacheSeconds
    ) {
        this.userDeviceRepository = userDeviceRepository;
        this.userRepository = userRepository;
        this.keycloakUserService = keycloakUserService;
        this.auditLogService = auditLogService;
        this.enforced = enforced;
        this.defaultMaxDevices = Math.max(1, defaultMaxDevices);
        this.sessionCacheMillis = Math.max(0, sessionCacheSeconds) * 1000;
    }

    @Override
    public boolean isEnforced() {
        return enforced;
    }

    @Override
    public int effectiveMaxDevices(User user) {
        Integer own = user.getMaxDevices();
        return (own != null && own > 0) ? own : defaultMaxDevices;
    }

    @Override
    @Transactional
    public void bindOnLogin(User user, DeviceContext device, String sessionId) {
        String deviceId = device.deviceId();
        LocalDateTime now = LocalDateTime.now();

        // Khoá hàng user: hai máy đăng nhập cùng lúc phải xếp hàng qua bước đếm dưới đây
        User locked = userRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new UnauthorizedException("User account not found"));

        UserDevice existing = userDeviceRepository.findByUserIdAndDeviceId(locked.getId(), deviceId).orElse(null);

        if (existing != null && existing.isActive()) {
            String oldSession = existing.getKeycloakSessionId();
            applyLogin(existing, device, sessionId, now);
            userDeviceRepository.save(existing);
            evict(sessionId);
            if (oldSession != null && !oldSession.equals(sessionId)) {
                // Cùng máy đăng nhập lại (vd mở app lần hai) → phiên cũ trên máy đó chấm dứt
                evict(oldSession);
                keycloakUserService.deleteSession(oldSession);
            }
            log.info("[DEVICE_LOGIN] user='{}' device='{}' (đã đăng ký)", locked.getUsername(), shortId(deviceId));
            return;
        }

        long active = userDeviceRepository.countByUserIdAndStatus(locked.getId(), DeviceStatus.ACTIVE);
        int max = effectiveMaxDevices(locked);
        if (active >= max) {
            log.warn("[DEVICE_LIMIT_REJECTED] user='{}' device='{}' active={} max={}",
                    locked.getUsername(), shortId(deviceId), active, max);
            audit(locked.getUsername(), locked, "DEVICE_LIMIT_REJECTED", locked.getId(),
                    "Từ chối máy mới " + describe(device) + " — đã có " + active + "/" + max + " máy");
            throw new ForbiddenException(SESSION_LIMIT_MESSAGE, ErrorCodes.SESSION_LIMIT);
        }

        UserDevice registered;
        if (existing != null) {
            // Máy từng bị gỡ đăng nhập lại khi còn chỗ → dùng lại bản ghi cũ
            existing.setStatus(DeviceStatus.ACTIVE);
            existing.setRevokedAt(null);
            existing.setRevokedBy(null);
            applyLogin(existing, device, sessionId, now);
            registered = userDeviceRepository.save(existing);
        } else {
            registered = userDeviceRepository.save(UserDevice.register(
                    locked, deviceId, trimToNull(device.deviceName()), trimToNull(device.platform()),
                    trimToNull(device.ipAddress()), sessionId, now));
        }
        evict(sessionId);

        audit(locked.getUsername(), locked, "DEVICE_REGISTERED", registered.getId(),
                "Đăng ký máy " + describe(device) + " (" + (active + 1) + "/" + max + ")");
        log.info("[DEVICE_REGISTERED] user='{}' device='{}' ({}/{})",
                locked.getUsername(), shortId(deviceId), active + 1, max);
    }

    @Override
    @Transactional
    public void verifyOnRefresh(String deviceId, String sessionId) {
        UserDevice d = StringUtils.hasText(sessionId)
                ? userDeviceRepository.findByKeycloakSessionId(sessionId).orElse(null)
                : null;

        if (d == null || !d.isActive() || !d.getDeviceId().equals(deviceId)) {
            log.warn("[DEVICE_REFRESH_REJECTED] sid='{}' device='{}' found={} active={}",
                    sessionId, shortId(deviceId), d != null, d != null && d.isActive());
            throw new UnauthorizedException(SESSION_REVOKED_MESSAGE, ErrorCodes.SESSION_REVOKED);
        }

        d.setLastSeenAt(LocalDateTime.now());
        userDeviceRepository.save(d);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSessionActive(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return false;
        }
        long now = System.currentTimeMillis();
        CachedSession cached = sessionCache.get(sessionId);
        if (cached != null && cached.expiresAtMillis() > now) {
            return cached.active();
        }
        boolean active = userDeviceRepository.findByKeycloakSessionId(sessionId)
                .map(UserDevice::isActive)
                .orElse(false);
        if (sessionCacheMillis > 0) {
            sessionCache.put(sessionId, new CachedSession(active, now + sessionCacheMillis));
        }
        return active;
    }

    @Override
    @Transactional
    public void releaseSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }
        userDeviceRepository.findByKeycloakSessionId(sessionId).ifPresent(d -> {
            d.setKeycloakSessionId(null);
            userDeviceRepository.save(d);
        });
        evict(sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDeviceResponse> listDevices(User user, String currentSessionId) {
        return userDeviceRepository.findByUserIdOrderByStatusAscLastSeenAtDesc(user.getId()).stream()
                .map(d -> UserDeviceResponse.of(d, currentSessionId))
                .toList();
    }

    @Override
    @Transactional
    public UserDeviceResponse revoke(User user, Long deviceRegistrationId, String actor, String actorRole) {
        UserDevice d = userDeviceRepository.findByIdAndUserId(deviceRegistrationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceRegistrationId));

        if (!d.isActive()) {
            return UserDeviceResponse.of(d, null);
        }

        String session = d.getKeycloakSessionId();
        d.setStatus(DeviceStatus.REVOKED);
        d.setRevokedAt(LocalDateTime.now());
        d.setRevokedBy(actor);
        d.setKeycloakSessionId(null);
        UserDevice saved = userDeviceRepository.save(d);

        evict(session);
        keycloakUserService.deleteSession(session);

        auditLogService.log(actor, actorRole, "DEVICE_REVOKED", "User", user.getId(),
                "Gỡ máy '" + describeSaved(saved) + "' khỏi tài khoản " + user.getUsername());
        log.info("[DEVICE_REVOKED] user='{}' device='{}' by='{}'", user.getUsername(), shortId(saved.getDeviceId()), actor);

        return UserDeviceResponse.of(saved, null);
    }

    private void applyLogin(UserDevice d, DeviceContext device, String sessionId, LocalDateTime now) {
        d.setKeycloakSessionId(sessionId);
        d.setLastSeenAt(now);
        if (StringUtils.hasText(device.ipAddress())) {
            d.setLastIp(device.ipAddress().trim());
        }
        if (StringUtils.hasText(device.deviceName())) {
            d.setDeviceName(device.deviceName().trim());
        }
        if (StringUtils.hasText(device.platform())) {
            d.setPlatform(device.platform().trim());
        }
    }

    private void evict(String sessionId) {
        if (sessionId != null) {
            sessionCache.remove(sessionId);
        }
    }

    private void audit(String actor, User user, String action, Long entityId, String details) {
        String role = user.getRole() != null ? user.getRole().name() : "USER";
        auditLogService.log(actor, role, action, "UserDevice", entityId, details);
    }

    private static String describe(DeviceContext device) {
        String name = StringUtils.hasText(device.deviceName()) ? device.deviceName().trim() : "?";
        return "'" + name + "' [" + shortId(device.deviceId()) + "]";
    }

    private static String describeSaved(UserDevice d) {
        return (d.getDeviceName() != null ? d.getDeviceName() : "?") + " [" + shortId(d.getDeviceId()) + "]";
    }

    private static String shortId(String deviceId) {
        if (deviceId == null) {
            return "?";
        }
        return deviceId.length() > 12 ? deviceId.substring(0, 12) : deviceId;
    }

    private static String trimToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
