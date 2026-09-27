package com.example.svgmanager.controller;

import com.example.svgmanager.dto.request.CreateDealerRequest;
import com.example.svgmanager.dto.request.UpdateDealerRequest;
import com.example.svgmanager.dto.response.DealerResponse;
import com.example.svgmanager.dto.response.DealerStatsResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.service.DealerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dealers")
@Tag(name = "Dealers", description = "Quản lý đại lý và chi nhánh")
public class DealerController {

    private final DealerService dealerService;

    public DealerController(DealerService dealerService) {
        this.dealerService = dealerService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách đại lý (phân trang, lọc, tìm kiếm)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    public ResponseEntity<PageResponse<DealerResponse>> getDealers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        PageResponse<DealerResponse> response = dealerService.getDealers(search, status, region, page, size, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    @Operation(summary = "Lấy toàn bộ đại lý (cho dropdown selection)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    public ResponseEntity<List<DealerResponse>> getAllDealers() {
        return ResponseEntity.ok(dealerService.getAllDealers());
    }

    @GetMapping("/stats")
    @Operation(summary = "Thống kê số lượng đại lý theo trạng thái")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    public ResponseEntity<DealerStatsResponse> getDealerStats() {
        return ResponseEntity.ok(dealerService.getDealerStats());
    }

    @GetMapping("/generate-code")
    @Operation(summary = "Sinh mã đại lý tự động (ddMMyyyy + 6 ký tự random)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> generateCode() {
        return ResponseEntity.ok(Map.of("code", dealerService.generateUniqueCode()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết đại lý theo ID")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    public ResponseEntity<DealerResponse> getDealerById(@PathVariable Long id) {
        return ResponseEntity.ok(dealerService.getDealerById(id));
    }

    @PostMapping
    @Operation(summary = "Thêm mới đại lý (Admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DealerResponse> createDealer(@Valid @RequestBody CreateDealerRequest request) {
        DealerResponse created = dealerService.createDealer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin đại lý (Admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DealerResponse> updateDealer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDealerRequest request
    ) {
        DealerResponse updated = dealerService.updateDealer(id, request);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái đại lý (Admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DealerResponse> updateDealerStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String status = body.get("status");
        DealerResponse updated = dealerService.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa mềm đại lý (Admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteDealer(@PathVariable Long id) {
        dealerService.deleteDealer(id);
        return ResponseEntity.ok(Map.of("message", "Xóa đại lý thành công"));
    }
}
