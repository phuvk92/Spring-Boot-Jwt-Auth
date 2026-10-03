package com.example.svgmanager.repository;

import com.example.svgmanager.entity.WorkDesign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkDesignRepository extends JpaRepository<WorkDesign, Long> {

    /** Màn "Bản đã lưu" — mới sửa gần nhất lên đầu, chỉ bản của đúng chủ. */
    List<WorkDesign> findByOwnerIdOrderByUpdatedAtDesc(Long ownerUserId);

    /** Tra cứu cho endpoint versions — KÈM chủ sở hữu: bản của người khác = 404, không lộ tồn tại. */
    Optional<WorkDesign> findByDesignKeyAndOwnerId(String designKey, Long ownerUserId);

    boolean existsByDesignKey(String designKey);
}
