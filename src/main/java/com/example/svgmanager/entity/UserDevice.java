package com.example.svgmanager.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Máy đã đăng ký của một tài khoản (F-57 — license theo máy, giới hạn theo số máy đã đăng ký).
 * Mỗi (user, deviceId) một dòng; gỡ máy thì chuyển REVOKED chứ không xoá, để còn dấu vết.
 */
@Entity
@Table(name = "user_devices",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_devices_user_device", columnNames = {"user_id", "device_id"}),
        indexes = {
                @Index(name = "idx_user_devices_user_status", columnList = "user_id, status"),
                @Index(name = "idx_user_devices_session", columnList = "keycloak_session_id")
        })
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Định danh máy do app cắt gửi (mã băm, không phải tên máy). */
    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "device_name", length = 255)
    private String deviceName;

    @Column(length = 50)
    private String platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceStatus status = DeviceStatus.ACTIVE;

    /** {@code sid} của phiên Keycloak đang chạy trên máy này — null khi đã đăng xuất hoặc bị gỡ. */
    @Column(name = "keycloak_session_id", length = 64)
    private String keycloakSessionId;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "last_ip", length = 64)
    private String lastIp;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by", length = 255)
    private String revokedBy;

    public UserDevice() {
    }

    public static UserDevice register(User user, String deviceId, String deviceName, String platform,
                                      String ip, String sessionId, LocalDateTime now) {
        UserDevice d = new UserDevice();
        d.user = user;
        d.deviceId = deviceId;
        d.deviceName = deviceName;
        d.platform = platform;
        d.status = DeviceStatus.ACTIVE;
        d.keycloakSessionId = sessionId;
        d.firstSeenAt = now;
        d.lastSeenAt = now;
        d.lastIp = ip;
        return d;
    }

    public boolean isActive() {
        return status == DeviceStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public DeviceStatus getStatus() { return status; }
    public void setStatus(DeviceStatus status) { this.status = status; }
    public String getKeycloakSessionId() { return keycloakSessionId; }
    public void setKeycloakSessionId(String keycloakSessionId) { this.keycloakSessionId = keycloakSessionId; }
    public LocalDateTime getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(LocalDateTime firstSeenAt) { this.firstSeenAt = firstSeenAt; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public String getLastIp() { return lastIp; }
    public void setLastIp(String lastIp) { this.lastIp = lastIp; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    public String getRevokedBy() { return revokedBy; }
    public void setRevokedBy(String revokedBy) { this.revokedBy = revokedBy; }
}
