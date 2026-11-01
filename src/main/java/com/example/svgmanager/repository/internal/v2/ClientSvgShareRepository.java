package com.example.svgmanager.repository.internal.v2;

import com.example.svgmanager.entity.v2.ClientSvgShare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ClientSvgShareRepository extends JpaRepository<ClientSvgShare, Long>, JpaSpecificationExecutor<ClientSvgShare> {

    Optional<ClientSvgShare> findByShareId(String shareId);

    boolean existsByShareId(String shareId);

    @Query("SELECT s FROM ClientSvgShare s WHERE s.ownerUser.id = :userId " +
            "AND (:status IS NULL OR LOWER(s.status) = LOWER(:status)) " +
            "AND (:fileId IS NULL OR s.userSvgFile.id = :fileId) " +
            "AND (:from IS NULL OR s.createdAt >= :from) " +
            "AND (:to IS NULL OR s.createdAt <= :to) " +
            "AND (:keyword IS NULL OR LOWER(s.userSvgFile.fileName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(s.shareId) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(s.note) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ClientSvgShare> findByOwnerWithFilters(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("fileId") Long fileId,
            @Param("keyword") String keyword,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @Query("SELECT s FROM ClientSvgShare s WHERE " +
            "(:status IS NULL OR LOWER(s.status) = LOWER(:status)) " +
            "AND (:fileId IS NULL OR s.userSvgFile.id = :fileId) " +
            "AND (:from IS NULL OR s.createdAt >= :from) " +
            "AND (:to IS NULL OR s.createdAt <= :to) " +
            "AND (:keyword IS NULL OR LOWER(s.userSvgFile.fileName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(s.shareId) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(s.note) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ClientSvgShare> findAllWithFilters(
            @Param("status") String status,
            @Param("fileId") Long fileId,
            @Param("keyword") String keyword,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}
