package com.example.svgmanager.repository;

import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SvgFileRepository extends JpaRepository<SvgFile, Long>, JpaSpecificationExecutor<SvgFile> {

    Optional<SvgFile> findByStoredFilename(String storedFilename);

    /** Danh tính hợp đồng /api/v1/files — chuỗi slug do đội nội dung đặt. */
    Optional<SvgFile> findByFileKey(String fileKey);

    Optional<SvgFile> findByIdAndAgentId(Long id, Long agentId);

    Page<SvgFile> findByAgentId(Long agentId, Pageable pageable);

    boolean existsByUploadedBy(User user);

    long countByUploadedBy(User user);

    boolean existsByAgent(User agent);

    boolean existsByFileKey(String fileKey);

    /** Thẻ thống kê kho part file (SA v2 §3.2) — chỉ đếm file còn hiệu lực. */
    long countByStatus(String status);

    long countByStatusAndSource(String status, String source);

    /** File ACTIVE không gắn mẫu xe nào — thẻ "Chưa gắn mẫu xe" (Q4 sinh ra khi xoá node). */
    @Query("SELECT COUNT(f) FROM SvgFile f WHERE f.status = :status "
            + "AND NOT EXISTS (SELECT 1 FROM SvgFileVehicleNode l WHERE l.svgFile = f)")
    long countUnlinkedByStatus(String status);

}
