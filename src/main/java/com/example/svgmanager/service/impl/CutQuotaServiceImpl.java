package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.request.AdjustQuotaRequest;
import com.example.svgmanager.dto.response.CutQuotaStatsResponse;
import com.example.svgmanager.dto.response.DealerCutQuotaResponse;
import com.example.svgmanager.dto.response.QuotaReconciliationResponse;
import com.example.svgmanager.entity.*;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ConflictException;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.repository.CutJobRepository;
import com.example.svgmanager.repository.CutQuotaLedgerRepository;
import com.example.svgmanager.repository.CutQuotaLedgerSpecification;
import com.example.svgmanager.repository.DealerCutQuotaRepository;
import com.example.svgmanager.repository.DealerRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.CutQuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CutQuotaServiceImpl implements CutQuotaService {

    private static final Logger log = LoggerFactory.getLogger(CutQuotaServiceImpl.class);

    private final DealerCutQuotaRepository dealerCutQuotaRepository;
    private final CutQuotaLedgerRepository cutQuotaLedgerRepository;
    private final DealerRepository dealerRepository;
    private final CutJobRepository cutJobRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public CutQuotaServiceImpl(DealerCutQuotaRepository dealerCutQuotaRepository,
                               CutQuotaLedgerRepository cutQuotaLedgerRepository,
                               DealerRepository dealerRepository,
                               CutJobRepository cutJobRepository,
                               CurrentUserService currentUserService,
                               AuditLogService auditLogService) {
        this.dealerCutQuotaRepository = dealerCutQuotaRepository;
        this.cutQuotaLedgerRepository = cutQuotaLedgerRepository;
        this.dealerRepository = dealerRepository;
        this.cutJobRepository = cutJobRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public Dealer resolveDealerForUser(User user) {
        if (user == null) {
            return null;
        }

        // 1. Direct mapping from user's dealer relation
        if (user.getDealer() != null) {
            Dealer d = user.getDealer();
            if (!d.isDeleted() && "ACTIVE".equalsIgnoreCase(d.getStatus())) {
                return d;
            }
        }

        // 2. If user is an AGENT, check dealer where agentUser = user
        if (user.getRole() == Role.AGENT) {
            Optional<Dealer> opt = dealerRepository.findByAgentUserIdAndDeletedFalse(user.getId());
            if (opt.isPresent()) {
                Dealer d = opt.get();
                if ("ACTIVE".equalsIgnoreCase(d.getStatus())) {
                    return d;
                }
            }
        }

        return null;
    }

    @Override
    @Transactional
    public DealerCutQuota getOrCreateDealerQuota(Dealer dealer) {
        return dealerCutQuotaRepository.findByDealerId(dealer.getId())
                .orElseGet(() -> {
                    log.info("Initializing default cut quota (1000) for dealer id={} ({})", dealer.getId(), dealer.getName());
                    DealerCutQuota initialQuota = new DealerCutQuota(dealer, 1000);
                    return dealerCutQuotaRepository.save(initialQuota);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public DealerCutQuotaResponse getDealerQuota(Long dealerId) {
        DealerCutQuota quota = dealerCutQuotaRepository.findByDealerId(dealerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quota cho đại lý ID=" + dealerId, "QUOTA_NOT_FOUND"));
        return DealerCutQuotaResponse.fromEntity(quota);
    }

    @Override
    @Transactional(readOnly = true)
    public DealerCutQuotaResponse getMyDealerQuota() {
        User currentUser = currentUserService.getCurrentUser();
        Dealer dealer = resolveDealerForUser(currentUser);
        if (dealer == null) {
            throw new ForbiddenException("Tài khoản chưa được gán vào đại lý hợp lệ.", "NO_VALID_DEALER");
        }
        DealerCutQuota quota = dealerCutQuotaRepository.findByDealerId(dealer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quota cho đại lý " + dealer.getName(), "QUOTA_NOT_FOUND"));
        return DealerCutQuotaResponse.fromEntity(quota);
    }

    @Override
    @Transactional
    public CutQuotaLedger deductQuotaForCut(User user, Dealer dealer, CutJob cutJob, String idempotencyKey, String requestId, String ipAddress) {
        // 1. Check idempotency: if transaction with this key already recorded for this dealer, deduplicate
        if (StringUtils.hasText(idempotencyKey)) {
            Optional<CutQuotaLedger> existing = cutQuotaLedgerRepository.findByDealerIdAndIdempotencyKey(dealer.getId(), idempotencyKey.trim());
            if (existing.isPresent()) {
                log.info("Idempotent cut deduction hit for dealerId={} idempotencyKey={}", dealer.getId(), idempotencyKey);
                return existing.get();
            }
        }

        // 2. Lock dealer quota row pessimistically to guarantee atomicity and avoid negative quota
        DealerCutQuota quota = dealerCutQuotaRepository.findByDealerIdForUpdate(dealer.getId())
                .orElseGet(() -> {
                    DealerCutQuota q = new DealerCutQuota(dealer, 1000);
                    return dealerCutQuotaRepository.save(q);
                });

        if (quota.getRemainingCuts() < 1) {
            log.warn("Dealer {} (id={}) out of cut quota! Remaining: {}", dealer.getName(), dealer.getId(), quota.getRemainingCuts());
            throw new BadRequestException("Đại lý " + dealer.getName() + " đã sử dụng hết số lượt Cut được cấp (còn "
                    + quota.getRemainingCuts() + " lượt). Vui lòng liên hệ quản trị viên để nạp thêm quota.", "QUOTA_EXCEEDED");
        }

        // 3. Deduct
        int balanceBefore = quota.getRemainingCuts();
        int balanceAfter = balanceBefore - 1;
        quota.setRemainingCuts(balanceAfter);
        quota.setConsumedCuts(quota.getConsumedCuts() + 1);
        quota.setSuccessCuts(quota.getSuccessCuts() + 1);
        quota.setUpdatedAt(LocalDateTime.now());
        dealerCutQuotaRepository.save(quota);

        // 4. Record ledger entry
        CutQuotaLedger ledger = CutQuotaLedger.builder()
                .dealer(dealer)
                .dealerName(dealer.getName())
                .user(user)
                .username(user.getUsername())
                .transactionType("CONSUME")
                .delta(-1)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .cutJob(cutJob)
                .idempotencyKey(StringUtils.hasText(idempotencyKey) ? idempotencyKey.trim() : null)
                .requestId(requestId)
                .status("SUCCESS")
                .ipAddress(ipAddress)
                .notes("Trừ 1 lượt Cut cho máy " + (cutJob.getUserDevice() != null ? cutJob.getUserDevice().getDeviceName() : "-"))
                .createdBy(user.getUsername())
                .createdAt(LocalDateTime.now())
                .build();

        CutQuotaLedger savedLedger = cutQuotaLedgerRepository.save(ledger);

        log.info("Deducted 1 cut quota for dealer '{}' (id={}) by user '{}'. Balance: {} -> {}",
                dealer.getName(), dealer.getId(), user.getUsername(), balanceBefore, balanceAfter);

        return savedLedger;
    }

    @Override
    @Transactional
    public CutQuotaLedger refundQuota(Long dealerId, Long cutJobId, String reason, String actorUsername) {
        Dealer dealer = dealerRepository.findByIdAndDeletedFalse(dealerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đại lý ID=" + dealerId, "DEALER_NOT_FOUND"));

        CutJob job = cutJobRepository.findByIdAndDealerId(cutJobId, dealerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lệnh cắt #" + cutJobId + " thuộc đại lý này.", "CUT_JOB_NOT_FOUND"));

        if ("REFUNDED".equalsIgnoreCase(job.getStatus())) {
            throw new ConflictException("Lệnh cắt #" + cutJobId + " đã được hoàn trả trước đó.", "ALREADY_REFUNDED");
        }

        DealerCutQuota quota = dealerCutQuotaRepository.findByDealerIdForUpdate(dealerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quota cho đại lý", "QUOTA_NOT_FOUND"));

        int balanceBefore = quota.getRemainingCuts();
        int balanceAfter = balanceBefore + 1;
        quota.setRemainingCuts(balanceAfter);
        quota.setRefundedCuts(quota.getRefundedCuts() + 1);
        quota.setUpdatedAt(LocalDateTime.now());
        dealerCutQuotaRepository.save(quota);

        job.setStatus("REFUNDED");
        cutJobRepository.save(job);

        String note = "Hoàn trả 1 lượt Cut cho lệnh #" + cutJobId + (StringUtils.hasText(reason) ? " (Lý do: " + reason + ")" : "");
        CutQuotaLedger ledger = CutQuotaLedger.builder()
                .dealer(dealer)
                .dealerName(dealer.getName())
                .user(job.getUser())
                .username(job.getUsername() != null ? job.getUsername() : actorUsername)
                .transactionType("REFUND")
                .delta(1)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .cutJob(job)
                .status("SUCCESS")
                .notes(note)
                .createdBy(actorUsername)
                .createdAt(LocalDateTime.now())
                .build();

        CutQuotaLedger savedLedger = cutQuotaLedgerRepository.save(ledger);

        // Audit log
        auditLogService.log(actorUsername, "ADMIN", "CUT_QUOTA_REFUND", "CutJob", job.getId(),
                dealer.getId(), dealer.getName(), null, "REFUNDED", 1, balanceAfter, note);

        return savedLedger;
    }

    @Override
    @Transactional
    public DealerCutQuotaResponse adjustQuota(Long dealerId, AdjustQuotaRequest request, String actorUsername) {
        Dealer dealer = dealerRepository.findByIdAndDeletedFalse(dealerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đại lý ID=" + dealerId, "DEALER_NOT_FOUND"));

        DealerCutQuota quota = dealerCutQuotaRepository.findByDealerIdForUpdate(dealerId)
                .orElseGet(() -> {
                    DealerCutQuota q = new DealerCutQuota(dealer, 1000);
                    return dealerCutQuotaRepository.save(q);
                });

        int balanceBefore = quota.getRemainingCuts();
        int delta;
        int balanceAfter;
        String actionType = request.getType() != null ? request.getType().trim().toUpperCase() : "ADD";

        if ("ADD".equals(actionType)) {
            if (request.getAmount() <= 0) {
                throw new BadRequestException("Số lượng cộng thêm phải lớn hơn 0", "INVALID_AMOUNT");
            }
            delta = request.getAmount();
            balanceAfter = balanceBefore + delta;
            quota.setAllocatedCuts(quota.getAllocatedCuts() + delta);
            quota.setAdjustedCuts(quota.getAdjustedCuts() + delta);
        } else if ("SUBTRACT".equals(actionType)) {
            if (request.getAmount() <= 0) {
                throw new BadRequestException("Số lượng giảm trừ phải lớn hơn 0", "INVALID_AMOUNT");
            }
            if (request.getAmount() > balanceBefore) {
                throw new BadRequestException("Số lượt trừ (" + request.getAmount() + ") lớn hơn số dư khả dụng (" + balanceBefore + ")", "INSUFFICIENT_QUOTA");
            }
            delta = -request.getAmount();
            balanceAfter = balanceBefore + delta;
            quota.setAdjustedCuts(quota.getAdjustedCuts() + delta);
        } else if ("SET".equals(actionType)) {
            if (request.getAmount() < 0) {
                throw new BadRequestException("Số dư mới không được là số âm", "INVALID_AMOUNT");
            }
            delta = request.getAmount() - balanceBefore;
            balanceAfter = request.getAmount();
            quota.setAdjustedCuts(quota.getAdjustedCuts() + delta);
        } else {
            throw new BadRequestException("Loại điều chỉnh không hợp lệ (hỗ trợ ADD, SUBTRACT, SET)", "INVALID_TYPE");
        }

        quota.setRemainingCuts(balanceAfter);
        quota.setUpdatedAt(LocalDateTime.now());
        dealerCutQuotaRepository.save(quota);

        String notes = "Điều chỉnh quota (" + actionType + " " + Math.abs(delta) + "): " + (StringUtils.hasText(request.getNotes()) ? request.getNotes() : "Admin thao tác");

        CutQuotaLedger ledger = CutQuotaLedger.builder()
                .dealer(dealer)
                .dealerName(dealer.getName())
                .username(actorUsername)
                .transactionType("ADJUST")
                .delta(delta)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .status("SUCCESS")
                .notes(notes)
                .createdBy(actorUsername)
                .createdAt(LocalDateTime.now())
                .build();

        cutQuotaLedgerRepository.save(ledger);

        // Audit log
        auditLogService.log(actorUsername, "ADMIN", "CUT_QUOTA_ADJUST", "DealerCutQuota", quota.getId(),
                dealer.getId(), dealer.getName(), null, "SUCCESS", delta, balanceAfter, notes);

        return DealerCutQuotaResponse.fromEntity(quota);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CutQuotaLedger> getLedger(Long dealerId, Long userId, String transactionType, String status,
                                         LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Specification<CutQuotaLedger> spec = CutQuotaLedgerSpecification.filter(dealerId, userId, transactionType, status, from, to);
        return cutQuotaLedgerRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public CutQuotaStatsResponse getQuotaStats(Long dealerId) {
        if (dealerId != null) {
            DealerCutQuota q = dealerCutQuotaRepository.findByDealerId(dealerId).orElse(null);
            if (q == null) {
                return new CutQuotaStatsResponse(0, 0, 0, 0, 0, 0, 0, 0);
            }
            return new CutQuotaStatsResponse(
                    q.getAllocatedCuts(),
                    q.getConsumedCuts(),
                    q.getRemainingCuts(),
                    q.getSuccessCuts(),
                    q.getFailedCuts(),
                    q.getRefundedCuts(),
                    q.getAdjustedCuts(),
                    1
            );
        }

        long allocated = dealerCutQuotaRepository.sumTotalAllocatedCuts();
        long consumed = dealerCutQuotaRepository.sumTotalConsumedCuts();
        long remaining = dealerCutQuotaRepository.sumTotalRemainingCuts();
        long success = dealerCutQuotaRepository.sumTotalSuccessCuts();
        long failed = dealerCutQuotaRepository.sumTotalFailedCuts();
        long refunded = dealerCutQuotaRepository.sumTotalRefundedCuts();
        long adjusted = dealerCutQuotaRepository.sumTotalAdjustedCuts();
        long dealerCount = dealerCutQuotaRepository.count();

        return new CutQuotaStatsResponse(allocated, consumed, remaining, success, failed, refunded, adjusted, dealerCount);
    }

    @Override
    @Transactional(readOnly = true)
    public QuotaReconciliationResponse reconcile(Long dealerId) {
        Dealer dealer = dealerRepository.findByIdAndDeletedFalse(dealerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đại lý ID=" + dealerId, "DEALER_NOT_FOUND"));

        DealerCutQuota quota = dealerCutQuotaRepository.findByDealerId(dealerId)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có quota cho đại lý này", "QUOTA_NOT_FOUND"));

        int ledgerDeltaSum = cutQuotaLedgerRepository.sumDeltaByDealerId(dealerId);
        int expectedRemaining = quota.getAllocatedCuts() + ledgerDeltaSum;
        int tableRemaining = quota.getRemainingCuts();
        int difference = tableRemaining - expectedRemaining;
        boolean balanced = (difference == 0);

        String msg = balanced
                ? "Số dư Quota khớp hoàn toàn với Sổ cái giao dịch."
                : "Phát hiện chênh lệch " + difference + " lượt giữa Bảng Quota và Sổ cái giao dịch.";

        return new QuotaReconciliationResponse(dealerId, dealer.getName(), tableRemaining, expectedRemaining, difference, balanced, msg);
    }
}
