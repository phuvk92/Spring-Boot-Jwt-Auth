package com.example.svgmanager.repository;

import com.example.svgmanager.entity.DeviceStatus;
import com.example.svgmanager.entity.Role;
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

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByKeycloakUserId(String keycloakUserId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.dealer WHERE u.keycloakUserId = :keycloakUserId")
    Optional<User> findByKeycloakUserIdWithDealer(@Param("keycloakUserId") String keycloakUserId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.dealer WHERE u.username = :username")
    Optional<User> findByUsernameWithDealer(@Param("username") String username);

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

    Optional<User> findByDealerIdAndRoleAndDeletedFalse(Long dealerId, Role role);

    boolean existsByDealerIdAndRoleAndDeletedFalse(Long dealerId, Role role);

    boolean existsByDealerIdAndRoleAndIdNotAndDeletedFalse(Long dealerId, Role role, Long id);

    List<User> findAllByDealerIdAndDeletedFalseOrderByCreatedAtDesc(Long dealerId);

    List<User> findAllByDealerIdAndRoleAndDeletedFalseOrderByCreatedAtDesc(Long dealerId, Role role);

    @Query("SELECT COALESCE(SUM(u.allocatedDays), 0) FROM User u WHERE u.dealer.id = :dealerId AND u.role = 'USER' AND u.deleted = false")
    int sumAllocatedDaysByDealerIdAndRoleUser(@Param("dealerId") Long dealerId);

    // ── Thống kê usersAtLimit (F-57 · NGO-422) ──────────────────────────────────

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

    // ── Thống kê vai trò người dùng (ADMIN, AGENT, USER) ─────────────────────────

    @Query("SELECT COUNT(DISTINCT u.id) FROM User u WHERE (u.deleted IS NULL OR u.deleted = false) AND u.role = :role")
    long countByRoleAndDeletedFalse(@Param("role") Role role);

    @Query("SELECT COUNT(DISTINCT u.id) FROM User u WHERE (u.deleted IS NULL OR u.deleted = false) AND u.role = :role " +
            "AND (u.agent.id = :agentId OR (u.dealer IS NOT NULL AND u.dealer.id = :dealerId))")
    long countByRoleAndDeletedFalseAndAgentScope(@Param("role") Role role, @Param("agentId") Long agentId, @Param("dealerId") Long dealerId);

    @Query("SELECT COUNT(DISTINCT u.id) FROM User u WHERE (u.deleted IS NULL OR u.deleted = false) AND u.role = :role " +
            "AND u.agent.id = :agentId")
    long countByRoleAndDeletedFalseAndAgentId(@Param("role") Role role, @Param("agentId") Long agentId);
}
