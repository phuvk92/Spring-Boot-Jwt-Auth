package com.example.svgmanager.repository;

import com.example.svgmanager.entity.SvgFileDealerPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SvgFileDealerPermissionRepository extends JpaRepository<SvgFileDealerPermission, Long> {

    List<SvgFileDealerPermission> findBySvgFileId(Long svgFileId);

    Optional<SvgFileDealerPermission> findBySvgFileIdAndDealerId(Long svgFileId, Long dealerId);

    boolean existsBySvgFileIdAndDealerId(Long svgFileId, Long dealerId);

    @Modifying
    @Query("DELETE FROM SvgFileDealerPermission sdp WHERE sdp.svgFile.id = :svgFileId")
    void deleteBySvgFileId(@Param("svgFileId") Long svgFileId);

    @Modifying
    @Query("DELETE FROM SvgFileDealerPermission sdp WHERE sdp.svgFile.id = :svgFileId AND sdp.dealer.id = :dealerId")
    void deleteBySvgFileIdAndDealerId(@Param("svgFileId") Long svgFileId, @Param("dealerId") Long dealerId);

    @Query("SELECT COUNT(sdp) FROM SvgFileDealerPermission sdp WHERE sdp.svgFile.id = :svgFileId AND sdp.canView = true")
    int countActiveDealersBySvgFileId(@Param("svgFileId") Long svgFileId);
}
