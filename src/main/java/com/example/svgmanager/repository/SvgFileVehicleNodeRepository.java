package com.example.svgmanager.repository;

import com.example.svgmanager.entity.SvgFileVehicleNode;
import com.example.svgmanager.entity.SvgFileVehicleNodeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SvgFileVehicleNodeRepository extends JpaRepository<SvgFileVehicleNode, SvgFileVehicleNodeId> {

    List<SvgFileVehicleNode> findBySvgFileId(Long svgFileId);

    /** Số file (distinct) gắn vào một trong các node — file mất liên kết khi xoá nhánh. */
    @Query("SELECT COUNT(DISTINCT l.svgFile.id) FROM SvgFileVehicleNode l WHERE l.vehicleNode.id IN :nodeIds")
    long countDistinctFilesByNodeIds(@Param("nodeIds") List<Long> nodeIds);

    @Modifying
    @Query("DELETE FROM SvgFileVehicleNode l WHERE l.vehicleNode.id IN :nodeIds")
    void deleteByVehicleNodeIdIn(@Param("nodeIds") List<Long> nodeIds);

    /** Thẻ "modelsWithFiles" — số mẫu xe (MODEL/SUBTYPE) có ít nhất một file ACTIVE. */
    @Query("SELECT COUNT(DISTINCT l.vehicleNode.id) FROM SvgFileVehicleNode l WHERE l.svgFile.status = :status")
    long countDistinctNodesByFileStatus(@Param("status") String status);
}
