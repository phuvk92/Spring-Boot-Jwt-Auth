package com.example.svgmanager.repository;

import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    /**
     * File cho app thợ (SA-DanhMucXe-v2 §3.3): ACTIVE + đúng danh mục + gắn vào một trong
     * các node cho sẵn (model kèm các phiên bản, hoặc subtype kèm model cha — service tính).
     * {@code year} null = không lọc năm; có year thì file không ghi năm (NULL) vẫn khớp (Q3).
     */
    @Query("SELECT DISTINCT f FROM SvgFile f JOIN f.vehicleNodes l "
            + "WHERE f.status = 'ACTIVE' "
            + "AND f.fileCategory.id = :categoryId "
            + "AND l.vehicleNode.id IN :nodeIds "
            + "AND (:year IS NULL OR f.modelYear IS NULL OR f.modelYear = :year) "
            + "ORDER BY f.id")
    List<SvgFile> findCatalogFiles(@Param("categoryId") Long categoryId,
                                   @Param("nodeIds") List<Long> nodeIds,
                                   @Param("year") Integer year);

    /**
     * Các năm có file ACTIVE khớp bộ lọc (catalog cấp year) — giảm dần.
     * File không ghi năm không sinh ra giá trị năm nên loại khỏi SELECT.
     * {@code nodeIds} rỗng + noNodeFilter=false thì truyền List.of(-1L) thay vì null.
     */
    @Query("SELECT DISTINCT f.modelYear FROM SvgFile f LEFT JOIN f.vehicleNodes l "
            + "WHERE f.status = 'ACTIVE' AND f.modelYear IS NOT NULL "
            + "AND (:categoryId IS NULL OR f.fileCategory.id = :categoryId) "
            + "AND (:noNodeFilter = true OR l.vehicleNode.id IN :nodeIds) "
            + "ORDER BY f.modelYear DESC")
    List<Integer> findCatalogYears(@Param("categoryId") Long categoryId,
                                   @Param("nodeIds") List<Long> nodeIds,
                                   @Param("noNodeFilter") boolean noNodeFilter);

    /** File ACTIVE không gắn mẫu xe nào — thẻ "Chưa gắn mẫu xe" (Q4 sinh ra khi xoá node). */
    @Query("SELECT COUNT(f) FROM SvgFile f WHERE f.status = :status "
            + "AND NOT EXISTS (SELECT 1 FROM SvgFileVehicleNode l WHERE l.svgFile = f)")
    long countUnlinkedByStatus(String status);

}
