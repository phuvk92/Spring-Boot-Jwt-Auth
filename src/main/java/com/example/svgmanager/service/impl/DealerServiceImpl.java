package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.request.CreateDealerRequest;
import com.example.svgmanager.dto.request.UpdateDealerRequest;
import com.example.svgmanager.dto.response.DealerPackageResponse;
import com.example.svgmanager.dto.response.DealerResponse;
import com.example.svgmanager.dto.response.DealerStatsResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.UserResponse;
import com.example.svgmanager.entity.Dealer;
import com.example.svgmanager.entity.DealerPackageHistory;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ConflictException;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.mapper.UserMapper;
import com.example.svgmanager.repository.DealerPackageHistoryRepository;
import com.example.svgmanager.repository.DealerRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.UserPartCategoryPermissionRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.DealerService;
import com.example.svgmanager.service.KeycloakUserService;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DealerServiceImpl implements DealerService {

    private static final Logger log = LoggerFactory.getLogger(DealerServiceImpl.class);
    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$"
    );
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@(.+)$"
    );

    private final DealerRepository dealerRepository;
    private final DealerPackageHistoryRepository dealerPackageHistoryRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;
    private final UserMapper userMapper;
    private final SvgFileRepository svgFileRepository;
    private final UserPartCategoryPermissionRepository userPartCategoryPermissionRepository;

    public DealerServiceImpl(DealerRepository dealerRepository,
                             DealerPackageHistoryRepository dealerPackageHistoryRepository,
                             CurrentUserService currentUserService,
                             UserRepository userRepository,
                             KeycloakUserService keycloakUserService,
                             UserMapper userMapper) {
        this(dealerRepository, dealerPackageHistoryRepository, currentUserService, userRepository, keycloakUserService, userMapper, null, null);
    }

    @Autowired
    public DealerServiceImpl(DealerRepository dealerRepository,
                             DealerPackageHistoryRepository dealerPackageHistoryRepository,
                             CurrentUserService currentUserService,
                             UserRepository userRepository,
                             KeycloakUserService keycloakUserService,
                             UserMapper userMapper,
                             @Autowired(required = false) SvgFileRepository svgFileRepository,
                             @Autowired(required = false) UserPartCategoryPermissionRepository userPartCategoryPermissionRepository) {
        this.dealerRepository = dealerRepository;
        this.dealerPackageHistoryRepository = dealerPackageHistoryRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.keycloakUserService = keycloakUserService;
        this.userMapper = userMapper;
        this.svgFileRepository = svgFileRepository;
        this.userPartCategoryPermissionRepository = userPartCategoryPermissionRepository;
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

            // Data scope isolation: AGENT only sees their own dealer
            if (!currentUserService.isAdmin()) {
                User currentUser = currentUserService.getCurrentUser();
                Long agentDealerId = currentUser.getDealer() != null ? currentUser.getDealer().getId() : -1L;
                predicates.add(cb.equal(root.get("id"), agentDealerId));
            }

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
        if (!currentUserService.isAdmin()) {
            User currentUser = currentUserService.getCurrentUser();
            if (currentUser.getDealer() != null) {
                return List.of(mapToResponse(currentUser.getDealer()));
            }
            return List.of();
        }
        return dealerRepository.findAllByDeletedFalseOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DealerResponse getDealerById(Long id) {
        Dealer dealer = dealerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đại lý không tồn tại với ID: " + id));

        if (!currentUserService.isAdmin()) {
            User currentUser = currentUserService.getCurrentUser();
            if (currentUser.getDealer() == null || !currentUser.getDealer().getId().equals(id)) {
                throw new ForbiddenException("Không có quyền xem thông tin của đại lý khác");
            }
        }

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

        // --- Validate Agent Credentials ---
        String effectiveAgentEmail = StringUtils.hasText(request.getAgentEmail())
                ? request.getAgentEmail().trim()
                : (StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : null);

        if (!StringUtils.hasText(effectiveAgentEmail)) {
            throw new BadRequestException("Email tài khoản đăng nhập đại lý là bắt buộc (vui lòng điền Email đại lý hoặc Email tài khoản)");
        }
        if (!EMAIL_PATTERN.matcher(effectiveAgentEmail).matches()) {
            throw new BadRequestException("Email tài khoản đại lý không hợp lệ: " + effectiveAgentEmail);
        }

        String effectiveAgentUsername = StringUtils.hasText(request.getAgentUsername())
                ? request.getAgentUsername().trim()
                : effectiveAgentEmail;

        if (!StringUtils.hasText(request.getAgentPassword())) {
            throw new BadRequestException("Mật khẩu tài khoản đại lý không được để trống");
        }
        if (!PASSWORD_PATTERN.matcher(request.getAgentPassword()).matches()) {
            throw new BadRequestException("Mật khẩu đại lý phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt");
        }

        if (userRepository.existsByUsername(effectiveAgentUsername)) {
            throw new ConflictException("Tên đăng nhập '" + effectiveAgentUsername + "' đã được sử dụng");
        }
        if (userRepository.existsByEmail(effectiveAgentEmail)) {
            throw new ConflictException("Email '" + effectiveAgentEmail + "' đã được sử dụng");
        }

        String effectiveAgentFullName = StringUtils.hasText(request.getAgentFullName())
                ? request.getAgentFullName().trim()
                : (StringUtils.hasText(request.getContactPerson()) ? request.getContactPerson().trim() : request.getName().trim());

        String effectiveAgentPhone = StringUtils.hasText(request.getAgentPhone())
                ? request.getAgentPhone().trim()
                : (StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null);

        // --- Step 1: Create Keycloak User with Role AGENT ---
        String keycloakUserId = keycloakUserService.createUser(
                effectiveAgentUsername,
                effectiveAgentEmail,
                request.getAgentPassword(),
                Role.AGENT,
                true,
                effectiveAgentFullName
        );

        // --- Step 2: Create Dealer and User with Compensating Rollback ---
        Dealer saved;
        try {
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

            saved = dealerRepository.save(dealer);

            // Create Agent User in Database: Role AGENT, allocatedDays = 0 (DO NOT consume quota)
            User agentUser = User.builder()
                    .keycloakUserId(keycloakUserId)
                    .username(effectiveAgentUsername)
                    .email(effectiveAgentEmail)
                    .fullName(effectiveAgentFullName)
                    .phone(effectiveAgentPhone)
                    .role(Role.AGENT)
                    .dealer(saved)
                    .enabled(true)
                    .deleted(false)
                    .allocatedDays(0) // AGENT consumes 0 days from quota
                    .build();

            User savedAgent = userRepository.save(agentUser);

            // Bind agent_user_id on Dealer
            saved.setAgentUser(savedAgent);
            saved = dealerRepository.save(saved);

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
                    .notes("Khởi tạo gói đại lý mới: " + maxAccounts + " accounts, " + totalAllocatedDays + " ngày. Tạo tài khoản đại lý: " + effectiveAgentUsername)
                    .createdBy(actor)
                    .build());

            log.info("[DEALER_CREATED] Dealer created: id={}, code='{}', name='{}', agentUser='{}' (id={}), maxAccounts={}, totalDays={}, actor='{}'",
                    saved.getId(), saved.getCode(), saved.getName(), savedAgent.getUsername(), savedAgent.getId(), maxAccounts, totalAllocatedDays, actor);

        } catch (Exception e) {
            log.error("[DEALER_CREATE_FAILED] Error persisting dealer and agent in database, rolling back Keycloak user: {}", keycloakUserId, e);
            try {
                keycloakUserService.deleteUser(keycloakUserId);
            } catch (Exception kcEx) {
                log.error("[KEYCLOAK_ROLLBACK_ERROR] Failed to delete Keycloak user {} during compensation", keycloakUserId, kcEx);
            }
            throw e;
        }

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

        // --- Cập nhật gói account và kiểm tra giới hạn đang sử dụng (chỉ tính role USER) ---
        long currentCreatedAccounts = dealerRepository.countActiveUserAccountsByDealerId(id);
        int currentUsedDays = dealer.getUsedAllocatedDays() != null ? dealer.getUsedAllocatedDays() : 0;
        int prevMaxAccounts = dealer.getMaxAccounts();
        int prevTotalDays = dealer.getTotalAllocatedDays();
        boolean packageChanged = false;

        if (request.getMaxAccounts() != null) {
            if (request.getMaxAccounts() < currentCreatedAccounts) {
                throw new BadRequestException(String.format(
                        "Không thể giảm số lượng account tối đa xuống %d vì đại lý đã tạo %d account Thợ cắt",
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
                        "Không thể giảm tổng quỹ ngày xuống %d ngày vì đại lý đã phân bổ %d ngày cho Thợ cắt",
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

        // --- Handle Linked AGENT user updates ---
        User agentUser = dealer.getAgentUser();
        if (agentUser == null) {
            // Check if there is an existing unlinked AGENT user for this dealer
            agentUser = userRepository.findByDealerIdAndRoleAndDeletedFalse(dealer.getId(), Role.AGENT).orElse(null);
            if (agentUser != null) {
                dealer.setAgentUser(agentUser);
            }
        }

        if (agentUser != null) {
            boolean agentChanged = false;
            if (StringUtils.hasText(request.getAgentEmail()) && !request.getAgentEmail().equalsIgnoreCase(agentUser.getEmail())) {
                String newEmail = request.getAgentEmail().trim();
                if (!EMAIL_PATTERN.matcher(newEmail).matches()) {
                    throw new BadRequestException("Email tài khoản đại lý không hợp lệ: " + newEmail);
                }
                if (userRepository.existsByEmailAndIdNot(newEmail, agentUser.getId())) {
                    throw new ConflictException("Email '" + newEmail + "' đã được sử dụng bởi người dùng khác");
                }
                agentUser.setEmail(newEmail);
                agentChanged = true;
            }
            if (request.getAgentFullName() != null && !request.getAgentFullName().isBlank()) {
                agentUser.setFullName(request.getAgentFullName().trim());
                agentChanged = true;
            }
            if (request.getAgentPhone() != null) {
                agentUser.setPhone(request.getAgentPhone().trim());
                agentChanged = true;
            }
            if (agentChanged) {
                userRepository.save(agentUser);
                if (StringUtils.hasText(agentUser.getKeycloakUserId())) {
                    keycloakUserService.updateUser(agentUser.getKeycloakUserId(), agentUser.getEmail(), agentUser.isEnabled(), agentUser.getFullName());
                }
            }
            if (StringUtils.hasText(request.getAgentPassword())) {
                if (!PASSWORD_PATTERN.matcher(request.getAgentPassword()).matches()) {
                    throw new BadRequestException("Mật khẩu đại lý phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt");
                }
                if (StringUtils.hasText(agentUser.getKeycloakUserId())) {
                    keycloakUserService.resetPassword(agentUser.getKeycloakUserId(), request.getAgentPassword());
                }
            }
        } else if (StringUtils.hasText(request.getAgentPassword())) {
            // Reconcile legacy dealer without AGENT: create new AGENT if password is provided
            String effectiveEmail = StringUtils.hasText(request.getAgentEmail())
                    ? request.getAgentEmail().trim()
                    : (StringUtils.hasText(dealer.getEmail()) ? dealer.getEmail().trim() : null);
            if (!StringUtils.hasText(effectiveEmail)) {
                throw new BadRequestException("Email tài khoản đại lý là bắt buộc");
            }
            String effectiveUsername = StringUtils.hasText(request.getAgentUsername())
                    ? request.getAgentUsername().trim()
                    : effectiveEmail;

            if (userRepository.existsByUsername(effectiveUsername)) {
                throw new ConflictException("Tên đăng nhập '" + effectiveUsername + "' đã được sử dụng");
            }
            if (userRepository.existsByEmail(effectiveEmail)) {
                throw new ConflictException("Email '" + effectiveEmail + "' đã được sử dụng");
            }

            String keycloakUserId = keycloakUserService.createUser(
                    effectiveUsername,
                    effectiveEmail,
                    request.getAgentPassword(),
                    Role.AGENT,
                    true,
                    StringUtils.hasText(request.getAgentFullName()) ? request.getAgentFullName() : dealer.getName()
            );

            User newAgent = User.builder()
                    .keycloakUserId(keycloakUserId)
                    .username(effectiveUsername)
                    .email(effectiveEmail)
                    .fullName(StringUtils.hasText(request.getAgentFullName()) ? request.getAgentFullName() : dealer.getName())
                    .phone(request.getAgentPhone() != null ? request.getAgentPhone() : dealer.getPhone())
                    .role(Role.AGENT)
                    .dealer(dealer)
                    .enabled(true)
                    .deleted(false)
                    .allocatedDays(0)
                    .build();
            User savedAgent = userRepository.save(newAgent);
            dealer.setAgentUser(savedAgent);
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

        // 1. Unlink agentUser from dealer first so foreign key doesn't block user deletion
        User agentUser = dealer.getAgentUser();
        dealer.setAgentUser(null);

        // 2. Archive dealer code to prevent unique constraint conflicts if re-creating
        long ts = System.currentTimeMillis();
        dealer.setCode(appendDeletedSuffix(dealer.getCode(), ts, 50));
        dealer.setDeleted(true);
        dealerRepository.save(dealer);

        // 3. Delete or archive agent user (releases username & email in Keycloak and DB)
        if (agentUser != null) {
            deleteOrArchiveUser(agentUser);
        }

        // 4. Also handle any remaining users belonging to this dealer
        List<User> dealerUsers = userRepository.findAllByDealerIdAndDeletedFalseOrderByCreatedAtDesc(dealer.getId());
        for (User u : dealerUsers) {
            if (agentUser != null && u.getId().equals(agentUser.getId())) {
                continue;
            }
            deleteOrArchiveUser(u);
        }

        String actor = currentUserService.getCurrentJwt()
                .map(j -> j.getClaimAsString("preferred_username"))
                .orElse("system");
        log.info("[DEALER_DELETED] Dealer deleted: id={}, code='{}', name='{}', actor='{}'",
                dealer.getId(), dealer.getCode(), dealer.getName(), actor);
    }

    private void deleteOrArchiveUser(User user) {
        if (user == null) return;
        String originalUsername = user.getUsername();
        String keycloakUserId = user.getKeycloakUserId();

        // 1. Delete from Keycloak to release login username and email
        if (StringUtils.hasText(keycloakUserId)) {
            try {
                keycloakUserService.deleteUser(keycloakUserId);
            } catch (Exception e) {
                log.error("Failed to delete Keycloak user {} during dealer cleanup", keycloakUserId, e);
            }
        }

        // 2. Check if user has business references (SVGs, child users)
        boolean hasSvg = svgFileRepository != null && svgFileRepository.existsByUploadedBy(user);
        boolean hasChildUsers = userRepository != null && userRepository.existsByAgent(user);

        if (!hasSvg && !hasChildUsers) {
            try {
                if (userPartCategoryPermissionRepository != null) {
                    userPartCategoryPermissionRepository.deleteByUserId(user.getId());
                }
                userRepository.delete(user);
                log.info("[DEALER_USER_DELETED] Hard deleted user: id={}, username='{}'", user.getId(), originalUsername);
                return;
            } catch (Exception e) {
                log.warn("Failed to hard delete user {}, falling back to archive and soft delete", user.getId(), e);
            }
        }

        // 3. Fallback: archive username and email to release unique constraints
        long ts = System.currentTimeMillis();
        user.setEnabled(false);
        user.setDeleted(true);
        user.setUsername(appendDeletedSuffix(user.getUsername(), ts, 255));
        user.setEmail(appendDeletedSuffix(user.getEmail(), ts, 255));
        userRepository.save(user);
        log.info("[DEALER_USER_ARCHIVED] Soft deleted and archived user: id={}, originalUsername='{}'", user.getId(), originalUsername);
    }

    private static String appendDeletedSuffix(String value, long ts, int maxLength) {
        if (value == null) return null;
        String suffix = "_deleted_" + ts;
        if (value.length() + suffix.length() > maxLength) {
            return value.substring(0, maxLength - suffix.length()) + suffix;
        }
        return value + suffix;
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

        // Sync linked AGENT user enable/disable
        User agentUser = dealer.getAgentUser();
        if (agentUser != null) {
            boolean shouldEnable = "ACTIVE".equalsIgnoreCase(cleanStatus);
            agentUser.setEnabled(shouldEnable);
            userRepository.save(agentUser);
            if (StringUtils.hasText(agentUser.getKeycloakUserId())) {
                try {
                    if (shouldEnable) {
                        keycloakUserService.enableUser(agentUser.getKeycloakUserId());
                    } else {
                        keycloakUserService.disableUser(agentUser.getKeycloakUserId());
                    }
                } catch (Exception e) {
                    log.error("Failed to sync status to Keycloak user {}", agentUser.getKeycloakUserId(), e);
                }
            }
        }

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

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getDealerUsers(Long id) {
        Dealer dealer = dealerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đại lý không tồn tại với ID: " + id));

        if (!currentUserService.isAdmin()) {
            User currentUser = currentUserService.getCurrentUser();
            if (currentUser.getDealer() == null || !currentUser.getDealer().getId().equals(id)) {
                throw new ForbiddenException("Không có quyền xem danh sách người dùng của đại lý khác");
            }
        }

        List<User> users = userRepository.findAllByDealerIdAndDeletedFalseOrderByCreatedAtDesc(id);
        return users.stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
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

        User agent = dealer.getAgentUser();
        if (agent == null) {
            agent = userRepository.findByDealerIdAndRoleAndDeletedFalse(dealer.getId(), Role.AGENT).orElse(null);
        }
        if (agent != null) {
            res.setAgentUserId(agent.getId());
            res.setAgentUsername(agent.getUsername());
            res.setAgentFullName(agent.getFullName());
            res.setAgentEmail(agent.getEmail());
            res.setAgentPhone(agent.getPhone());
            res.setAgentEnabled(agent.isEnabled());
        }

        return res;
    }
}
