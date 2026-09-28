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

    /** File thiết kế gắn thẳng vào lá danh mục (submodel) — nguồn của GET /api/v1/files. */
    List<SvgFile> findByCategoryIdAndStatusOrderByIdAsc(Long categoryId, String status);

    Optional<SvgFile> findByIdAndAgentId(Long id, Long agentId);

    Page<SvgFile> findByAgentId(Long agentId, Pageable pageable);

    boolean existsByUploadedBy(User user);

    long countByUploadedBy(User user);

    boolean existsByAgent(User agent);

    boolean existsByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);

    boolean existsByVehicleConfigurationId(Long vehicleConfigurationId);

    long countByVehicleConfigurationId(Long vehicleConfigurationId);

    @Query("SELECT CASE WHEN COUNT(svc) > 0 THEN true ELSE false END " +
           "FROM SvgFileVehicleConfiguration svc WHERE svc.vehicleConfiguration.id = :configId")
    boolean existsByAssignedVehicleConfigurationId(@Param("configId") Long configId);
}
