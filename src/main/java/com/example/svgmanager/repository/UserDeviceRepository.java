package com.example.svgmanager.repository;

import com.example.svgmanager.entity.DeviceStatus;
import com.example.svgmanager.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByUserIdAndDeviceId(Long userId, String deviceId);

    Optional<UserDevice> findByKeycloakSessionId(String keycloakSessionId);

    Optional<UserDevice> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndStatus(Long userId, DeviceStatus status);

    List<UserDevice> findByUserIdOrderByStatusAscLastSeenAtDesc(Long userId);
}
