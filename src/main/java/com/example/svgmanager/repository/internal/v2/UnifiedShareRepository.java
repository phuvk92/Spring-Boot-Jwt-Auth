package com.example.svgmanager.repository.internal.v2;

import com.example.svgmanager.entity.v2.UnifiedShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnifiedShareRepository extends JpaRepository<UnifiedShare, String>, JpaSpecificationExecutor<UnifiedShare> {

    Optional<UnifiedShare> findByShareId(String shareId);

    @Query("SELECT COUNT(u) FROM UnifiedShare u WHERE u.shareType = :shareType")
    long countByShareType(@Param("shareType") String shareType);

    @Query("SELECT COUNT(u) FROM UnifiedShare u WHERE u.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT COALESCE(SUM(u.previewCount), 0) FROM UnifiedShare u")
    long sumTotalPreviews();

    @Query("SELECT COALESCE(SUM(u.downloadCount), 0) FROM UnifiedShare u")
    long sumTotalDownloads();

    @Query("SELECT COALESCE(SUM(u.verifyCount), 0) FROM UnifiedShare u")
    long sumTotalVerifies();

    @Query("SELECT COUNT(DISTINCT u.sharedByUserId) FROM UnifiedShare u WHERE u.status = 'ACTIVE'")
    long countActiveClients();
}
