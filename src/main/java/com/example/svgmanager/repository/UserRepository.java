package com.example.svgmanager.repository;

import com.example.svgmanager.entity.DeviceStatus;
import com.example.svgmanager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByKeycloakUserId(String keycloakUserId);

    Optional<User> findByIdAndAgentId(Long id, Long agentId);

    /**
     * Khoá hàng user trong transaction — hai máy đăng nhập cùng lúc không được cùng lọt
     * qua bước đếm số máy (F-57).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    Page<User> findByAgentId(Long agentId, Pageable pageable);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByAgent(User agent);

    // ── Thống kê usersAtLimit (F-57 · NGO-422) ───────────────────────────────────

    /**
     * Đếm số user đang hoạt động có số máy ACTIVE >= max_devices (mặc định defaultMaxDevices khi max_devices null)
     * toàn hệ thống.
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.deleted = false " +
            "AND (SELECT COUNT(d) FROM UserDevice d WHERE d.user.id = u.id AND d.status = :status) >= COALESCE(u.maxDevices, :defaultMax)")
    long countUsersAtLimit(@Param("status") DeviceStatus status, @Param("defaultMax") int defaultMax);

    /**
     * Đếm số user thuộc đại lý có số máy ACTIVE >= max_devices (mặc định defaultMaxDevices khi max_devices null)
     * theo AGENT scope.
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.deleted = false AND u.dealer.id = :dealerId " +
            "AND (SELECT COUNT(d) FROM UserDevice d WHERE d.user.id = u.id AND d.status = :status) >= COALESCE(u.maxDevices, :defaultMax)")
    long countUsersAtLimitAndDealerId(@Param("status") DeviceStatus status, @Param("defaultMax") int defaultMax, @Param("dealerId") Long dealerId);
}
