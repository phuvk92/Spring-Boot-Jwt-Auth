package com.example.svgmanager.service;

import com.example.svgmanager.dto.response.UserDeviceResponse;
import com.example.svgmanager.entity.User;

import java.util.List;

/**
 * F-57 — giới hạn số máy đăng ký theo tài khoản. Thiết kế: Pcut-Client/technical/SA-GioiHanThietBi.md.
 */
public interface UserDeviceService {

    /** Thông tin máy app cắt gửi kèm đăng nhập. */
    record DeviceContext(String deviceId, String deviceName, String platform, String ipAddress) {
    }

    /** Tắt được bằng {@code app.device.enforce=false} (test, hoặc khẩn cấp). */
    boolean isEnforced();

    int effectiveMaxDevices(User user);

    /**
     * Gắn phiên vừa đăng nhập vào máy. Máy đã đăng ký → cập nhật phiên (phiên cũ của chính máy đó bị chấm dứt).
     * Máy mới mà tài khoản đã đủ số máy → ForbiddenException {@code SESSION_LIMIT}.
     */
    void bindOnLogin(User user, DeviceContext device, String sessionId);

    /** Làm mới phiên: {@code sid} phải thuộc máy ACTIVE đúng {@code deviceId} → nếu không, UnauthorizedException {@code SESSION_REVOKED}. */
    void verifyOnRefresh(String deviceId, String sessionId);

    /** Phiên còn gắn với một máy ACTIVE không — dùng cho mọi request /api/internal/**, có cache ngắn. */
    boolean isSessionActive(String sessionId);

    /** Đăng xuất: máy vẫn giữ chỗ, chỉ bỏ liên kết phiên. */
    void releaseSession(String sessionId);

    List<UserDeviceResponse> listDevices(User user, String currentSessionId);

    /** Gỡ máy: nhả chỗ + chấm dứt phiên đang chạy trên máy đó. Gỡ máy đã gỡ thì không làm gì. */
    UserDeviceResponse revoke(User user, Long deviceRegistrationId, String actor, String actorRole);
}
