package com.example.svgmanager.repository;

import com.example.svgmanager.entity.DeviceStatus;
import com.example.svgmanager.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long>, JpaSpecificationExecutor<UserDevice> {

    Optional<UserDevice> findByUserIdAndDeviceId(Long userId, String deviceId);

    Optional<UserDevice> findByKeycloakSessionId(String keycloakSessionId);

    Optional<UserDevice> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndStatus(Long userId, DeviceStatus status);

    List<UserDevice> findByUserIdOrderByStatusAscLastSeenAtDesc(Long userId);

    // ── Thống kê thiết bị toàn hệ thống (F-57 · NGO-422) ────────────────────────

    /** Tổng số máy ACTIVE toàn hệ thống */
    long countByStatus(DeviceStatus status);

    /** Tổng số máy ACTIVE theo đại lý (AGENT scope) */
    @Query("SELECT COUNT(d) FROM UserDevice d WHERE d.status = :status AND d.user.dealer.id = :dealerId AND d.user.deleted = false")
    long countByStatusAndDealerId(@Param("status") DeviceStatus status, @Param("dealerId") Long dealerId);

    /** Máy ACTIVE có lastSeenAt >= :since (activeNow) toàn hệ thống */
    @Query("SELECT COUNT(d) FROM UserDevice d WHERE d.status = :status AND d.lastSeenAt >= :since AND d.user.deleted = false")
    long countActiveSince(@Param("status") DeviceStatus status, @Param("since") LocalDateTime since);

    /** Máy ACTIVE có lastSeenAt >= :since (activeNow) theo đại lý (AGENT scope) */
    @Query("SELECT COUNT(d) FROM UserDevice d WHERE d.status = :status AND d.lastSeenAt >= :since AND d.user.dealer.id = :dealerId AND d.user.deleted = false")
    long countActiveSinceAndDealerId(@Param("status") DeviceStatus status, @Param("since") LocalDateTime since, @Param("dealerId") Long dealerId);

    /** Máy ACTIVE có lastSeenAt < :staleThreshold (staleDevices) toàn hệ thống */
    @Query("SELECT COUNT(d) FROM UserDevice d WHERE d.status = :status AND d.lastSeenAt < :staleThreshold AND d.user.deleted = false")
    long countStaleDevices(@Param("status") DeviceStatus status, @Param("staleThreshold") LocalDateTime staleThreshold);

    /** Máy ACTIVE có lastSeenAt < :staleThreshold (staleDevices) theo đại lý (AGENT scope) */
    @Query("SELECT COUNT(d) FROM UserDevice d WHERE d.status = :status AND d.lastSeenAt < :staleThreshold AND d.user.dealer.id = :dealerId AND d.user.deleted = false")
    long countStaleDevicesAndDealerId(@Param("status") DeviceStatus status, @Param("staleThreshold") LocalDateTime staleThreshold, @Param("dealerId") Long dealerId);
}
