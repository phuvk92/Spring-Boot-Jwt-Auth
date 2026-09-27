package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.request.CreateDealerRequest;
import com.example.svgmanager.dto.request.UpdateDealerRequest;
import com.example.svgmanager.dto.response.DealerResponse;
import com.example.svgmanager.dto.response.DealerStatsResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.entity.Dealer;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ConflictException;
import com.example.svgmanager.exception.ResourceNotFoundException;
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
import java.util.stream.Collectors;

@Service
public class DealerServiceImpl implements DealerService {

    private static final Logger log = LoggerFactory.getLogger(DealerServiceImpl.class);
    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DealerRepository dealerRepository;
    private final CurrentUserService currentUserService;

    public DealerServiceImpl(DealerRepository dealerRepository, CurrentUserService currentUserService) {
        this.dealerRepository = dealerRepository;
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

        Dealer dealer = Dealer.builder()
                .code(code)
                .name(request.getName().trim())
                .region(request.getRegion() != null ? request.getRegion().trim() : null)
                .address(request.getAddress() != null ? request.getAddress().trim() : null)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .contactPerson(request.getContactPerson() != null ? request.getContactPerson().trim() : null)
                .plan(request.getPlan() != null && !request.getPlan().isBlank() ? request.getPlan().trim() : "Cơ bản")
                .dueDate(request.getDueDate() != null ? request.getDueDate().trim() : null)
                .status(request.getStatus() != null && !request.getStatus().isBlank() ? request.getStatus().trim().toUpperCase() : "ACTIVE")
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .deleted(false)
                .build();

        Dealer saved = dealerRepository.save(dealer);
        String actor = currentUserService.getCurrentJwt()
                .map(j -> j.getClaimAsString("preferred_username"))
                .orElse("system");
        log.info("[DEALER_CREATED] Dealer created: id={}, code='{}', name='{}', actor='{}'",
                saved.getId(), saved.getCode(), saved.getName(), actor);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public DealerResponse updateDealer(Long id, UpdateDealerRequest request) {
        Dealer dealer = dealerRepository.findByIdAndDeletedFalse(id)
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
        if (request.getPlan() != null && !request.getPlan().isBlank()) {
            dealer.setPlan(request.getPlan().trim());
        }
        dealer.setDueDate(request.getDueDate() != null ? request.getDueDate().trim() : null);
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            dealer.setStatus(request.getStatus().trim().toUpperCase());
        }
        dealer.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);

        Dealer updated = dealerRepository.save(dealer);
        String actor = currentUserService.getCurrentJwt()
                .map(j -> j.getClaimAsString("preferred_username"))
                .orElse("system");
        log.info("[DEALER_UPDATED] Dealer updated: id={}, code='{}', name='{}', actor='{}'",
                updated.getId(), updated.getCode(), updated.getName(), actor);

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

    private DealerResponse mapToResponse(Dealer dealer) {
        long usersCount = dealerRepository.countUsersByDealerId(dealer.getId());
        return new DealerResponse(
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
    }
}
