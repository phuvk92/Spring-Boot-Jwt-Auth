package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.request.CreateDealerRequest;
import com.example.svgmanager.dto.request.UpdateDealerRequest;
import com.example.svgmanager.dto.response.DealerPackageResponse;
import com.example.svgmanager.dto.response.DealerResponse;
import com.example.svgmanager.dto.response.DealerStatsResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.entity.Dealer;
import com.example.svgmanager.entity.DealerPackageHistory;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ConflictException;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.repository.DealerPackageHistoryRepository;
import com.example.svgmanager.repository.DealerRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.DealerService;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DealerServiceImpl implements DealerService {

    private static final Logger log = LoggerFactory.getLogger(DealerServiceImpl.class);
    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DealerRepository dealerRepository;
    private final DealerPackageHistoryRepository dealerPackageHistoryRepository;
    private final CurrentUserService currentUserService;

    public DealerServiceImpl(DealerRepository dealerRepository,
                             DealerPackageHistoryRepository dealerPackageHistoryRepository,
                             CurrentUserService currentUserService) {
        this.dealerRepository = dealerRepository;
        this.dealerPackageHistoryRepository = dealerPackageHistoryRepository;
        this.currentUserService = currentUserService;
    }

    @Override
    public String generateUniqueCode() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        for (int attempt = 0; attempt < 100; attempt++) {
            StringBuilder sb = new StringBuilder(datePrefix);
            for (int i = 0; i < 6; i++) {
                sb.append(CHAR_POOL.charAt(RANDOM.nextInt(CHAR_POOL.length())));
            }
            String candidate = sb.toString();
            if (!dealerRepository.existsByCodeAndDeletedFalse(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Không thể sinh mã đại lý không trùng sau 100 lần thử");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DealerResponse> getDealers(String search, String status, String region, int page, int size, String sortBy, String sortDir) {
        String effectiveSortBy = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, effectiveSortBy));

        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanStatus = (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) ? status.trim() : null;
        String cleanRegion = (region != null && !region.isBlank() && !"all".equalsIgnoreCase(region)) ? region.trim() : null;

        Specification<Dealer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));

            if (cleanStatus != null) {
                predicates.add(cb.equal(cb.upper(root.get("status")), cleanStatus.toUpperCase()));
            }

            if (cleanRegion != null) {
                predicates.add(cb.equal(cb.upper(root.get("region")), cleanRegion.toUpperCase()));
            }

            if (cleanSearch != null) {
                String pattern = "%" + cleanSearch.toLowerCase() + "%";
                Predicate codeMatch = cb.like(cb.lower(root.get("code")), pattern);
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate contactMatch = cb.like(cb.lower(root.get("contactPerson")), pattern);
                Predicate phoneMatch = cb.like(cb.lower(root.get("phone")), pattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);
                Predicate regionMatch = cb.like(cb.lower(root.get("region")), pattern);
                predicates.add(cb.or(codeMatch, nameMatch, contactMatch, phoneMatch, emailMatch, regionMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Dealer> dealerPage = dealerRepository.findAll(spec, pageable);

        List<DealerResponse> responses = dealerPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<DealerResponse>builder()
                .content(responses)
                .page(dealerPage.getNumber())
                .size(dealerPage.getSize())
                .totalElements(dealerPage.getTotalElements())
                .totalPages(dealerPage.getTotalPages())
                .first(dealerPage.isFirst())
                .last(dealerPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DealerResponse> getAllDealers() {
        return dealerRepository.findAllByDeletedFalseOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DealerResponse getDealerById(Long id) {
        Dealer dealer = dealerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đại lý không tồn tại với ID: " + id));
        return mapToResponse(dealer);
    }

    @Override
    @Transactional
    public DealerResponse createDealer(CreateDealerRequest request) {
        String code = (request.getCode() != null && !request.getCode().trim().isEmpty())
                ? request.getCode().trim().toUpperCase()
                : generateUniqueCode();

        // Ensure uniqueness even in race condition
        if (dealerRepository.existsByCodeAndDeletedFalse(code)) {
            code = generateUniqueCode();
        }

        int maxAccounts = request.getMaxAccounts() != null ? request.getMaxAccounts() : 10;
        int totalAllocatedDays = request.getTotalAllocatedDays() != null ? request.getTotalAllocatedDays() : 3600;
        LocalDate startDate = request.getPackageStartDate() != null ? request.getPackageStartDate() : LocalDate.now();
        LocalDate expiryDate = request.getPackageExpiryDate() != null ? request.getPackageExpiryDate() : startDate.plusDays(360);
        String pkgStatus = (request.getPackageStatus() != null && !request.getPackageStatus().isBlank())
                ? request.getPackageStatus().trim().toUpperCase()
                : "ACTIVE";

        Dealer dealer = Dealer.builder()
                .code(code)
                .name(request.getName().trim())
                .region(request.getRegion() != null ? request.getRegion().trim() : null)
                .address(request.getAddress() != null ? request.getAddress().trim() : null)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .contactPerson(request.getContactPerson() != null ? request.getContactPerson().trim() : null)
                .plan("Cơ bản")
                .dueDate(null)
                .status(request.getStatus() != null && !request.getStatus().isBlank() ? request.getStatus().trim().toUpperCase() : "ACTIVE")
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .deleted(false)
                .maxAccounts(maxAccounts)
                .totalAllocatedDays(totalAllocatedDays)
                .usedAllocatedDays(0)
                .packageStartDate(startDate)
                .packageExpiryDate(expiryDate)
                .packageStatus(pkgStatus)
                .build();

        Dealer saved = dealerRepository.save(dealer);
        String actor = currentUserService.getCurrentJwt()
                .map(j -> j.getClaimAsString("preferred_username"))
                .orElse("system");

        dealerPackageHistoryRepository.save(DealerPackageHistory.builder()
                .dealer(saved)
                .action("GRANT_PACKAGE")
                .previousMaxAccounts(0)
                .newMaxAccounts(maxAccounts)
                .previousTotalDays(0)
                .newTotalDays(totalAllocatedDays)
                .usedDays(0)
                .changeDays(totalAllocatedDays)
                .notes("Khởi tạo gói đại lý mới: " + maxAccounts + " accounts, " + totalAllocatedDays + " ngày")
                .createdBy(actor)
                .build());

        log.info("[DEALER_CREATED] Dealer created: id={}, code='{}', name='{}', maxAccounts={}, totalDays={}, actor='{}'",
                saved.getId(), saved.getCode(), saved.getName(), maxAccounts, totalAllocatedDays, actor);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public DealerResponse updateDealer(Long id, UpdateDealerRequest request) {
        Dealer dealer = dealerRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đại lý không tồn tại với ID: " + id));

        String code = request.getCode().trim().toUpperCase();
        if (dealerRepository.existsByCodeAndIdNotAndDeletedFalse(code, id)) {
            throw new ConflictException("Mã đại lý '" + code + "' đã được sử dụng bởi đại lý khác");
        }

        dealer.setCode(code);
        dealer.setName(request.getName().trim());
        dealer.setRegion(request.getRegion() != null ? request.getRegion().trim() : null);
        dealer.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        dealer.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        dealer.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        dealer.setContactPerson(request.getContactPerson() != null ? request.getContactPerson().trim() : null);
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            dealer.setStatus(request.getStatus().trim().toUpperCase());
        }
        dealer.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);

        // --- Cập nhật gói account và kiểm tra giới hạn đang sử dụng ---
        long currentCreatedAccounts = dealerRepository.countActiveUserAccountsByDealerId(id);
        int currentUsedDays = dealer.getUsedAllocatedDays() != null ? dealer.getUsedAllocatedDays() : 0;
        int prevMaxAccounts = dealer.getMaxAccounts();
        int prevTotalDays = dealer.getTotalAllocatedDays();
        boolean packageChanged = false;

        if (request.getMaxAccounts() != null) {
            if (request.getMaxAccounts() < currentCreatedAccounts) {
                throw new BadRequestException(String.format(
                        "Không thể giảm số lượng account tối đa xuống %d vì đại lý đã tạo %d account",
                        request.getMaxAccounts(), currentCreatedAccounts
                ));
            }
            if (!Objects.equals(dealer.getMaxAccounts(), request.getMaxAccounts())) {
                dealer.setMaxAccounts(request.getMaxAccounts());
                packageChanged = true;
            }
        }

        if (request.getTotalAllocatedDays() != null) {
            if (request.getTotalAllocatedDays() < currentUsedDays) {
                throw new BadRequestException(String.format(
                        "Không thể giảm tổng quỹ ngày xuống %d ngày vì đại lý đã phân bổ %d ngày",
                        request.getTotalAllocatedDays(), currentUsedDays
                ));
            }
            if (!Objects.equals(dealer.getTotalAllocatedDays(), request.getTotalAllocatedDays())) {
                dealer.setTotalAllocatedDays(request.getTotalAllocatedDays());
                packageChanged = true;
            }
        }

        if (request.getPackageStartDate() != null) {
            dealer.setPackageStartDate(request.getPackageStartDate());
            packageChanged = true;
        }
        if (request.getPackageExpiryDate() != null) {
            dealer.setPackageExpiryDate(request.getPackageExpiryDate());
            packageChanged = true;
        }
        if (request.getPackageStatus() != null && !request.getPackageStatus().isBlank()) {
            dealer.setPackageStatus(request.getPackageStatus().trim().toUpperCase());
            packageChanged = true;
        }

        Dealer updated = dealerRepository.save(dealer);
        String actor = currentUserService.getCurrentJwt()
                .map(j -> j.getClaimAsString("preferred_username"))
                .orElse("system");

        if (packageChanged) {
            dealerPackageHistoryRepository.save(DealerPackageHistory.builder()
                    .dealer(updated)
                    .action("UPDATE_PACKAGE")
                    .previousMaxAccounts(prevMaxAccounts)
                    .newMaxAccounts(updated.getMaxAccounts())
                    .previousTotalDays(prevTotalDays)
                    .newTotalDays(updated.getTotalAllocatedDays())
                    .usedDays(currentUsedDays)
                    .changeDays(updated.getTotalAllocatedDays() - prevTotalDays)
                    .notes("Cập nhật thông tin gói đại lý")
                    .createdBy(actor)
                    .build());
        }

        log.info("[DEALER_UPDATED] Dealer updated: id={}, code='{}', name='{}', maxAccounts={}, totalDays={}, actor='{}'",
                updated.getId(), updated.getCode(), updated.getName(), updated.getMaxAccounts(), updated.getTotalAllocatedDays(), actor);

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteDealer(Long id) {
        Dealer dealer = dealerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đại lý không tồn tại với ID: " + id));

        dealer.setDeleted(true);
        dealerRepository.save(dealer);

        String actor = currentUserService.getCurrentJwt()
                .map(j -> j.getClaimAsString("preferred_username"))
                .orElse("system");
        log.info("[DEALER_DELETED] Dealer soft-deleted: id={}, code='{}', name='{}', actor='{}'",
                dealer.getId(), dealer.getCode(), dealer.getName(), actor);
    }

    @Override
    @Transactional(readOnly = true)
    public DealerStatsResponse getDealerStats() {
        long total = dealerRepository.countByDeletedFalse();
        long active = dealerRepository.countByDeletedFalseAndStatus("ACTIVE");
        long expiring = dealerRepository.countByDeletedFalseAndStatus("EXPIRING");
        long locked = dealerRepository.countByDeletedFalseAndStatus("LOCKED");
        return new DealerStatsResponse(total, active, expiring, locked);
    }

    @Override
    @Transactional
    public DealerResponse updateStatus(Long id, String status) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("Trạng thái không được để trống");
        }
        Dealer dealer = dealerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đại lý không tồn tại với ID: " + id));

        String cleanStatus = status.trim().toUpperCase();
        dealer.setStatus(cleanStatus);
        Dealer saved = dealerRepository.save(dealer);

        String actor = currentUserService.getCurrentJwt()
                .map(j -> j.getClaimAsString("preferred_username"))
                .orElse("system");
        log.info("[DEALER_STATUS_CHANGED] Dealer status updated: id={}, status='{}', actor='{}'",
                saved.getId(), saved.getStatus(), actor);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DealerPackageResponse getDealerPackage(Long id) {
        Dealer dealer = dealerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đại lý không tồn tại với ID: " + id));

        if (!currentUserService.isAdmin()) {
            User currentUser = currentUserService.getCurrentUser();
            if (currentUser.getDealer() == null || !currentUser.getDealer().getId().equals(id)) {
                throw new ForbiddenException("Không có quyền xem thông tin gói của đại lý khác");
            }
        }

        long activeAccounts = dealerRepository.countActiveUserAccountsByDealerId(dealer.getId());
        return DealerPackageResponse.builder()
                .dealerId(dealer.getId())
                .dealerCode(dealer.getCode())
                .dealerName(dealer.getName())
                .maxAccounts(dealer.getMaxAccounts())
                .createdAccounts((int) activeAccounts)
                .remainingAccounts(Math.max(0, dealer.getMaxAccounts() - (int) activeAccounts))
                .totalAllocatedDays(dealer.getTotalAllocatedDays())
                .usedAllocatedDays(dealer.getUsedAllocatedDays())
                .remainingAllocatedDays(dealer.getRemainingAllocatedDays())
                .packageStartDate(dealer.getPackageStartDate())
                .packageExpiryDate(dealer.getPackageExpiryDate())
                .packageStatus(dealer.getPackageStatus())
                .packageExpired(dealer.isPackageExpired())
                .packageActive(dealer.isPackageActive())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DealerPackageResponse getMyDealerPackage() {
        User currentUser = currentUserService.getCurrentUser();
        Dealer dealer = currentUser.getDealer();
        if (dealer == null) {
            throw new BadRequestException("Tài khoản của bạn chưa được liên kết với đại lý nào");
        }
        return getDealerPackage(dealer.getId());
    }

    private DealerResponse mapToResponse(Dealer dealer) {
        long usersCount = dealerRepository.countUsersByDealerId(dealer.getId());
        long activeAccounts = dealerRepository.countActiveUserAccountsByDealerId(dealer.getId());
        DealerResponse res = new DealerResponse(
                dealer.getId(),
                dealer.getCode(),
                dealer.getName(),
                dealer.getRegion(),
                dealer.getAddress(),
                dealer.getPhone(),
                dealer.getEmail(),
                dealer.getContactPerson(),
                dealer.getPlan(),
                dealer.getDueDate(),
                dealer.getStatus(),
                usersCount,
                dealer.getNotes(),
                dealer.getCreatedAt(),
                dealer.getUpdatedAt()
        );
        res.setMaxAccounts(dealer.getMaxAccounts());
        res.setCreatedAccounts((int) activeAccounts);
        res.setRemainingAccounts(Math.max(0, dealer.getMaxAccounts() - (int) activeAccounts));
        res.setTotalAllocatedDays(dealer.getTotalAllocatedDays());
        res.setUsedAllocatedDays(dealer.getUsedAllocatedDays());
        res.setRemainingAllocatedDays(dealer.getRemainingAllocatedDays());
        res.setPackageStartDate(dealer.getPackageStartDate());
        res.setPackageExpiryDate(dealer.getPackageExpiryDate());
        res.setPackageStatus(dealer.getPackageStatus());
        res.setPackageExpired(dealer.isPackageExpired());
        return res;
    }
}
