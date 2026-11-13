package com.example.svgmanager.repository;

import com.example.svgmanager.entity.DealerCutQuota;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DealerCutQuotaRepository extends JpaRepository<DealerCutQuota, Long> {

    Optional<DealerCutQuota> findByDealerId(Long dealerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM DealerCutQuota q WHERE q.dealer.id = :dealerId")
    Optional<DealerCutQuota> findByDealerIdForUpdate(@Param("dealerId") Long dealerId);

    @Query("SELECT COALESCE(SUM(q.allocatedCuts), 0) FROM DealerCutQuota q")
    long sumTotalAllocatedCuts();

    @Query("SELECT COALESCE(SUM(q.consumedCuts), 0) FROM DealerCutQuota q")
    long sumTotalConsumedCuts();

    @Query("SELECT COALESCE(SUM(q.remainingCuts), 0) FROM DealerCutQuota q")
    long sumTotalRemainingCuts();

    @Query("SELECT COALESCE(SUM(q.successCuts), 0) FROM DealerCutQuota q")
    long sumTotalSuccessCuts();

    @Query("SELECT COALESCE(SUM(q.failedCuts), 0) FROM DealerCutQuota q")
    long sumTotalFailedCuts();

    @Query("SELECT COALESCE(SUM(q.refundedCuts), 0) FROM DealerCutQuota q")
    long sumTotalRefundedCuts();

    @Query("SELECT COALESCE(SUM(q.adjustedCuts), 0) FROM DealerCutQuota q")
    long sumTotalAdjustedCuts();
}
