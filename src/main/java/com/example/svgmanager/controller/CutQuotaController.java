package com.example.svgmanager.controller;

import com.example.svgmanager.dto.request.AdjustQuotaRequest;
import com.example.svgmanager.dto.request.RefundQuotaRequest;
import com.example.svgmanager.dto.response.*;
import com.example.svgmanager.entity.CutQuotaLedger;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.CutQuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cut Quota Management", description = "Quản lý Quota lượt Cut và Sổ cái biến động theo User/Đại lý")
public class CutQuotaController {

    private final CutQuotaService cutQuotaService;
    private final CurrentUserService currentUserService;

    public CutQuotaController(CutQuotaService cutQuotaService, CurrentUserService currentUserService) {
        this.cutQuotaService = cutQuotaService;
        this.currentUserService = currentUserService;
    }

    // --- 1. USER & AGENT ENDPOINTS ---

    @GetMapping("/api/v1/quota/my-quota")
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'ADMIN')")
    @Operation(summary = "Lấy thông tin số lượt Cut hiện tại của đại lý thuộc phiên đăng nhập")
    public ResponseEntity<DealerCutQuotaResponse> getMyQuota() {
        return ResponseEntity.ok(cutQuotaService.getMyDealerQuota());
    }

    // --- 2. ADMIN ENDPOINTS ---

    @GetMapping("/api/admin/cut-quota/dealers/{dealerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xem thông tin Quota lượt Cut của một đại lý cụ thể")
    public ResponseEntity<DealerCutQuotaResponse> getDealerQuota(@PathVariable Long dealerId) {
        return ResponseEntity.ok(cutQuotaService.getDealerQuota(dealerId));
    }

    @PostMapping("/api/admin/cut-quota/dealers/{dealerId}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin điều chỉnh số lượt Cut của đại lý (Cộng thêm, Giảm trừ hoặc Thiết lập mới)")
    public ResponseEntity<DealerCutQuotaResponse> adjustDealerQuota(
            @PathVariable Long dealerId,
            @Valid @RequestBody AdjustQuotaRequest request
    ) {
        String actor;
        try {
            actor = currentUserService.getCurrentUser().getUsername();
        } catch (Exception e) {
            actor = "admin";
        }
        return ResponseEntity.ok(cutQuotaService.adjustQuota(dealerId, request, actor));
    }

    @PostMapping("/api/admin/cut-quota/dealers/{dealerId}/refund/{cutJobId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin hoàn trả 1 lượt Cut cho một lệnh cắt cụ thể của đại lý")
    public ResponseEntity<CutQuotaLedgerResponse> refundCutJob(
            @PathVariable Long dealerId,
            @PathVariable Long cutJobId,
            @RequestBody(required = false) RefundQuotaRequest request
    ) {
        String actor;
        try {
            actor = currentUserService.getCurrentUser().getUsername();
        } catch (Exception e) {
            actor = "admin";
        }
        String reason = request != null ? request.getReason() : "Hoàn trả từ quản trị viên";
        CutQuotaLedger ledger = cutQuotaService.refundQuota(dealerId, cutJobId, reason, actor);
        return ResponseEntity.ok(CutQuotaLedgerResponse.fromEntity(ledger));
    }

    @GetMapping("/api/admin/cut-quota/ledger")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xem lịch sử sổ cái biến động lượt Cut (Cut Quota Ledger) có phân trang và lọc")
    public ResponseEntity<PageResponse<CutQuotaLedgerResponse>> getLedger(
            @Parameter(description = "ID Đại lý") @RequestParam(required = false) Long dealerId,
            @Parameter(description = "ID User") @RequestParam(required = false) Long userId,
            @Parameter(description = "Loại giao dịch (ALLOCATE, CONSUME, REFUND, ADJUST)") @RequestParam(required = false) String transactionType,
            @Parameter(description = "Trạng thái (SUCCESS, FAILED, REFUNDED)") @RequestParam(required = false) String status,
            @Parameter(description = "Từ thời điểm") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Đến thời điểm") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Sort sort = Sort.by(
                "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC,
                sortBy != null && !sortBy.isBlank() ? sortBy : "createdAt"
        );
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);

        Page<CutQuotaLedger> ledgerPage = cutQuotaService.getLedger(dealerId, userId, transactionType, status, from, to, pageable);
        Page<CutQuotaLedgerResponse> responsePage = ledgerPage.map(CutQuotaLedgerResponse::fromEntity);

        return ResponseEntity.ok(PageResponse.of(responsePage));
    }

    @GetMapping("/api/admin/cut-quota/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thống kê tổng hợp số lượt Cut (toàn hệ thống hoặc theo đại lý)")
    public ResponseEntity<CutQuotaStatsResponse> getQuotaStats(
            @Parameter(description = "ID Đại lý (tuỳ chọn, bỏ trống để xem toàn hệ thống)") @RequestParam(required = false) Long dealerId
    ) {
        return ResponseEntity.ok(cutQuotaService.getQuotaStats(dealerId));
    }

    @GetMapping("/api/admin/cut-quota/reconciliation/{dealerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Đối soát tính toàn vẹn số dư Quota giữa bảng tổng hợp và Sổ cái giao dịch")
    public ResponseEntity<QuotaReconciliationResponse> reconcileQuota(@PathVariable Long dealerId) {
        return ResponseEntity.ok(cutQuotaService.reconcile(dealerId));
    }
}
