package com.example.svgmanager.repository;

import com.example.svgmanager.entity.VehicleNode;
import com.example.svgmanager.entity.VehicleNodeLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleNodeRepository extends JpaRepository<VehicleNode, Long> {

    /** Trang hãng — gốc cây (parent IS NULL ⇔ BRAND). */
    Page<VehicleNode> findByParentIsNull(Pageable pageable);

    List<VehicleNode> findByParentIsNullOrderByDisplayOrderAscIdAsc();

    List<VehicleNode> findByParentIdOrderByDisplayOrderAscIdAsc(Long parentId);

    /** Trùng tên trong cùng cha (không phân biệt hoa thường) — nghiệp vụ 409 NODE_NAME_TAKEN. */
    boolean existsByParentIdAndNameIgnoreCase(Long parentId, String name);

    boolean existsByParentIsNullAndNameIgnoreCase(String name);

    /**
     * Id các node có tên khớp {@code q} (không phân biệt hoa thường) — đầu vào để
     * tìm hãng chứa chúng khi tìm kiếm cây.
     */
    @Query("SELECT n.id FROM VehicleNode n WHERE LOWER(n.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Long> findIdsByNameContaining(@Param("q") String q);

    /**
     * Id các hãng (gốc) là tổ tiên của một trong các node cho trước — quy hồi lên gốc.
     */
    @Query(value = """
            WITH RECURSIVE ancestors(id, parent_id) AS (
                SELECT id, parent_id FROM vehicle_nodes WHERE id IN (:nodeIds)
                UNION ALL
                SELECT n.id, n.parent_id FROM vehicle_nodes n
                JOIN ancestors a ON n.id = a.parent_id
            )
            SELECT DISTINCT id FROM ancestors WHERE parent_id IS NULL
            """, nativeQuery = true)
    List<Long> findRootIdsOf(@Param("nodeIds") List<Long> nodeIds);

    Page<VehicleNode> findByParentIsNullAndIdIn(List<Long> ids, Pageable pageable);

    /** Đếm cả nhánh con (mọi hậu duệ, không tính chính node). */
    @Query(value = """
            WITH RECURSIVE subtree(id) AS (
                SELECT id FROM vehicle_nodes WHERE id = :id
                UNION ALL
                SELECT n.id FROM vehicle_nodes n JOIN subtree s ON n.parent_id = s.id
            )
            SELECT COUNT(*) - 1 FROM subtree
            """, nativeQuery = true)
    long countDescendants(@Param("id") Long id);

    /** Id của node và mọi hậu duệ — phục vụ xoá nhánh / đếm file mất liên kết. */
    @Query(value = """
            WITH RECURSIVE subtree(id) AS (
                SELECT id FROM vehicle_nodes WHERE id = :id
                UNION ALL
                SELECT n.id FROM vehicle_nodes n JOIN subtree s ON n.parent_id = s.id
            )
            SELECT id FROM subtree
            """, nativeQuery = true)
    List<Long> findSubtreeIds(@Param("id") Long id);

    long countByParentId(Long parentId);
}
