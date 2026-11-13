package com.example.svgmanager.service;

import com.example.svgmanager.dto.request.AdjustQuotaRequest;
import com.example.svgmanager.dto.response.CutQuotaStatsResponse;
import com.example.svgmanager.dto.response.DealerCutQuotaResponse;
import com.example.svgmanager.dto.response.QuotaReconciliationResponse;
import com.example.svgmanager.entity.CutJob;
import com.example.svgmanager.entity.CutQuotaLedger;
import com.example.svgmanager.entity.Dealer;
import com.example.svgmanager.entity.DealerCutQuota;
import com.example.svgmanager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface CutQuotaService {

    Dealer resolveDealerForUser(User user);

    DealerCutQuota getOrCreateDealerQuota(Dealer dealer);

    DealerCutQuotaResponse getDealerQuota(Long dealerId);

    DealerCutQuotaResponse getMyDealerQuota();

    CutQuotaLedger deductQuotaForCut(User user, Dealer dealer, CutJob cutJob, String idempotencyKey, String requestId, String ipAddress);

    CutQuotaLedger refundQuota(Long dealerId, Long cutJobId, String reason, String actorUsername);

    DealerCutQuotaResponse adjustQuota(Long dealerId, AdjustQuotaRequest request, String actorUsername);

    Page<CutQuotaLedger> getLedger(Long dealerId, Long userId, String transactionType, String status,
                                  LocalDateTime from, LocalDateTime to, Pageable pageable);

    CutQuotaStatsResponse getQuotaStats(Long dealerId);

    QuotaReconciliationResponse reconcile(Long dealerId);
}
