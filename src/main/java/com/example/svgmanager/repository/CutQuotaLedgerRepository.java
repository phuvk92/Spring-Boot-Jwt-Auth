package com.example.svgmanager.repository;

import com.example.svgmanager.entity.CutQuotaLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CutQuotaLedgerRepository extends JpaRepository<CutQuotaLedger, Long>, JpaSpecificationExecutor<CutQuotaLedger> {

    Optional<CutQuotaLedger> findByDealerIdAndIdempotencyKey(Long dealerId, String idempotencyKey);

    boolean existsByDealerIdAndIdempotencyKey(Long dealerId, String idempotencyKey);

    List<CutQuotaLedger> findByCutJobId(Long cutJobId);

    @Query("SELECT COALESCE(SUM(l.delta), 0) FROM CutQuotaLedger l WHERE l.dealer.id = :dealerId")
    int sumDeltaByDealerId(@Param("dealerId") Long dealerId);

    @Query("SELECT COALESCE(SUM(l.delta), 0) FROM CutQuotaLedger l WHERE l.user.id = :userId")
    int sumDeltaByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(l) FROM CutQuotaLedger l WHERE l.dealer.id = :dealerId AND l.transactionType = 'CONSUME'")
    long countConsumeByDealerId(@Param("dealerId") Long dealerId);

    @Query("SELECT COUNT(l) FROM CutQuotaLedger l WHERE l.dealer.id = :dealerId AND l.transactionType = 'REFUND'")
    long countRefundByDealerId(@Param("dealerId") Long dealerId);
}
