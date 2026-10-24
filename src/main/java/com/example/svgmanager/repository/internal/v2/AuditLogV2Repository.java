package com.example.svgmanager.repository.internal.v2;

import com.example.svgmanager.entity.v2.AuditLogV2;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface AuditLogV2Repository extends JpaRepository<AuditLogV2, Long>, JpaSpecificationExecutor<AuditLogV2> {

    @Query("SELECT COUNT(a) FROM AuditLogV2 a WHERE " +
            "(:from IS NULL OR a.createdAt >= :from) AND " +
            "(:to IS NULL OR a.createdAt <= :to) AND " +
            "(:clientId IS NULL OR a.clientId = :clientId) AND " +
            "(:dealerId IS NULL OR a.dealerId = :dealerId)")
    long countFiltered(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("clientId") String clientId,
            @Param("dealerId") Long dealerId
    );

    @Query("SELECT a.result AS result, COUNT(a.id) AS cnt FROM AuditLogV2 a WHERE " +
            "(:from IS NULL OR a.createdAt >= :from) AND " +
            "(:to IS NULL OR a.createdAt <= :to) AND " +
            "(:clientId IS NULL OR a.clientId = :clientId) AND " +
            "(:dealerId IS NULL OR a.dealerId = :dealerId) " +
            "GROUP BY a.result")
    List<Map<String, Object>> countByResultGroup(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("clientId") String clientId,
            @Param("dealerId") Long dealerId
    );

    @Query("SELECT a.endpoint AS endpoint, a.httpMethod AS httpMethod, COUNT(a.id) AS cnt, AVG(a.executionTimeMs) AS avgMs " +
            "FROM AuditLogV2 a WHERE " +
            "(:from IS NULL OR a.createdAt >= :from) AND " +
            "(:to IS NULL OR a.createdAt <= :to) AND " +
            "(:clientId IS NULL OR a.clientId = :clientId) AND " +
            "(:dealerId IS NULL OR a.dealerId = :dealerId) " +
            "GROUP BY a.endpoint, a.httpMethod " +
            "ORDER BY COUNT(a.id) DESC")
    List<Map<String, Object>> findTopEndpoints(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("clientId") String clientId,
            @Param("dealerId") Long dealerId,
            Pageable pageable
    );

    @Query("SELECT a.clientId AS clientId, a.clientName AS clientName, COUNT(a.id) AS cnt " +
            "FROM AuditLogV2 a WHERE " +
            "(:from IS NULL OR a.createdAt >= :from) AND " +
            "(:to IS NULL OR a.createdAt <= :to) AND " +
            "(:clientId IS NULL OR a.clientId = :clientId) AND " +
            "(:dealerId IS NULL OR a.dealerId = :dealerId) AND " +
            "a.clientId IS NOT NULL " +
            "GROUP BY a.clientId, a.clientName " +
            "ORDER BY COUNT(a.id) DESC")
    List<Map<String, Object>> findTopClients(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("clientId") String clientId,
            @Param("dealerId") Long dealerId,
            Pageable pageable
    );

    @Query("SELECT a.username AS username, a.role AS role, COUNT(a.id) AS cnt " +
            "FROM AuditLogV2 a WHERE " +
            "(:from IS NULL OR a.createdAt >= :from) AND " +
            "(:to IS NULL OR a.createdAt <= :to) AND " +
            "(:clientId IS NULL OR a.clientId = :clientId) AND " +
            "(:dealerId IS NULL OR a.dealerId = :dealerId) AND " +
            "a.username IS NOT NULL " +
            "GROUP BY a.username, a.role " +
            "ORDER BY COUNT(a.id) DESC")
    List<Map<String, Object>> findTopUsers(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("clientId") String clientId,
            @Param("dealerId") Long dealerId,
            Pageable pageable
    );
}
