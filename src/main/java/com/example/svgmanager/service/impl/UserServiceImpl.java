package com.example.svgmanager.service.impl;

import com.example.svgmanager.util.PasswordUtils;
import com.example.svgmanager.util.UsernameUtils;

import com.example.svgmanager.dto.request.CreateUserRequest;
import com.example.svgmanager.dto.request.ResetPasswordRequest;
import com.example.svgmanager.dto.request.UpdateUserRequest;
import com.example.svgmanager.dto.request.UpdateUserRoleRequest;
import com.example.svgmanager.dto.request.UpdateUserStatusRequest;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.UserDeviceResponse;
import com.example.svgmanager.dto.response.UserResponse;
import com.example.svgmanager.dto.response.UserStatsResponse;
import com.example.svgmanager.entity.PartLibraryCategory;
import com.example.svgmanager.entity.UserPartCategoryPermission;
import com.example.svgmanager.repository.PartLibraryCategoryRepository;
import com.example.svgmanager.repository.UserPartCategoryPermissionRepository;
import com.example.svgmanager.entity.Dealer;
import com.example.svgmanager.entity.DealerPackageHistory;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ConflictException;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.mapper.UserMapper;
import com.example.svgmanager.repository.DealerPackageHistoryRepository;
import com.example.svgmanager.repository.DealerRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.KeycloakUserService;
import com.example.svgmanager.service.UserDeviceService;
import com.example.svgmanager.service.UserService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class UserServiceImpl implements UserService {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final DealerRepository dealerRepository;
    private final SvgFileRepository svgFileRepository;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;
    private final KeycloakUserService keycloakUserService;
    private final UserDeviceService userDeviceService;
    private final AuditLogService auditLogService;
    private final DealerPackageHistoryRepository dealerPackageHistoryRepository;
    private final UserPartCategoryPermissionRepository userPartCategoryPermissionRepository;
    private final PartLibraryCategoryRepository partLibraryCategoryRepository;

    public UserServiceImpl(
            UserRepository userRepository,
            DealerRepository dealerRepository,
            SvgFileRepository svgFileRepository,
            UserMapper userMapper,
            CurrentUserService currentUserService,
            KeycloakUserService keycloakUserService,
            UserDeviceService userDeviceService
    ) {
        this(userRepository, dealerRepository, svgFileRepository, userMapper, currentUserService, keycloakUserService, userDeviceService, null, null);
    }

    public UserServiceImpl(
            UserRepository userRepository,
            DealerRepository dealerRepository,
            SvgFileRepository svgFileRepository,
            UserMapper userMapper,
            CurrentUserService currentUserService,
            KeycloakUserService keycloakUserService,
            UserDeviceService userDeviceService,
            AuditLogService auditLogService
    ) {
        this(userRepository, dealerRepository, svgFileRepository, userMapper, currentUserService, keycloakUserService, userDeviceService, auditLogService, null);
    }

    public UserServiceImpl(
            UserRepository userRepository,
            DealerRepository dealerRepository,
            SvgFileRepository svgFileRepository,
            UserMapper userMapper,
            CurrentUserService currentUserService,
            KeycloakUserService keycloakUserService,
            UserDeviceService userDeviceService,
            AuditLogService auditLogService,
            DealerPackageHistoryRepository dealerPackageHistoryRepository
    ) {
        this(userRepository, dealerRepository, svgFileRepository, userMapper, currentUserService, keycloakUserService, userDeviceService, auditLogService, dealerPackageHistoryRepository, null, null);
    }

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            DealerRepository dealerRepository,
            SvgFileRepository svgFileRepository,
            UserMapper userMapper,
            CurrentUserService currentUserService,
            KeycloakUserService keycloakUserService,
            UserDeviceService userDeviceService,
            @Autowired(required = false) AuditLogService auditLogService,
            @Autowired(required = false) DealerPackageHistoryRepository dealerPackageHistoryRepository,
            @Autowired(required = false) UserPartCategoryPermissionRepository userPartCategoryPermissionRepository,
            @Autowired(required = false) PartLibraryCategoryRepository partLibraryCategoryRepository
    ) {
        this.userRepository = userRepository;
        this.dealerRepository = dealerRepository;
        this.svgFileRepository = svgFileRepository;
        this.userMapper = userMapper;
        this.currentUserService = currentUserService;
        this.keycloakUserService = keycloakUserService;
        this.userDeviceService = userDeviceService;
        this.auditLogService = auditLogService;
        this.dealerPackageHistoryRepository = dealerPackageHistoryRepository;
        this.userPartCategoryPermissionRepository = userPartCategoryPermissionRepository;
        this.partLibraryCategoryRepository = partLibraryCategoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats(
            String username,
            String email,
            Role role,
            Boolean enabled,
            String expirationStatus,
            Long dealerId
    ) {
        User currentUser = currentUserService.getCurrentUser();
        boolean isAdmin = currentUserService.isAdmin();
        boolean isAgent = currentUserService.isAgent();

        boolean hasFilters = org.springframework.util.StringUtils.hasText(username)
                || org.springframework.util.StringUtils.hasText(email)
                || enabled != null
                || org.springframework.util.StringUtils.hasText(expirationStatus)
                || dealerId != null;

        long adminCount;
        long agentCount;
        long userCount;

        if (!hasFilters) {
            if (isAdmin) {
                adminCount = (role == null || role == Role.ADMIN) ? userRepository.countByRoleAndDeletedFalse(Role.ADMIN) : 0;
                agentCount = (role == null || role == Role.AGENT) ? userRepository.countByRoleAndDeletedFalse(Role.AGENT) : 0;
                userCount = (role == null || role == Role.USER) ? userRepository.countByRoleAndDeletedFalse(Role.USER) : 0;
                return new UserStatsResponse(adminCount, agentCount, userCount);
            } else if (isAgent) {
                Long agentDealerId = currentUser.getDealer() != null ? currentUser.getDealer().getId() : null;
                if (agentDealerId != null) {
                    adminCount = 0;
                    agentCount = (role == null || role == Role.AGENT)
                            ? userRepository.countByRoleAndDeletedFalseAndAgentScope(Role.AGENT, currentUser.getId(), agentDealerId) : 0;
                    userCount = (role == null || role == Role.USER)
                            ? userRepository.countByRoleAndDeletedFalseAndAgentScope(Role.USER, currentUser.getId(), agentDealerId) : 0;
                    return new UserStatsResponse(adminCount, agentCount, userCount);
                } else {
                    adminCount = 0;
                    agentCount = (role == null || role == Role.AGENT)
                            ? userRepository.countByRoleAndDeletedFalseAndAgentId(Role.AGENT, currentUser.getId()) : 0;
                    userCount = (role == null || role == Role.USER)
                            ? userRepository.countByRoleAndDeletedFalseAndAgentId(Role.USER, currentUser.getId()) : 0;
                    return new UserStatsResponse(adminCount, agentCount, userCount);
                }
            }
        }

        Specification<User> baseSpec = buildUserSpecification(
                username,
                email,
                null,
                enabled,
                expirationStatus,
                dealerId,
                isAdmin,
                isAgent,
                currentUser
        );

        adminCount = (role == null || role == Role.ADMIN)
                ? userRepository.count(Specification.where(baseSpec).and((root, query, cb) -> cb.equal(root.get("role"), Role.ADMIN)))
                : 0;
        agentCount = (role == null || role == Role.AGENT)
                ? userRepository.count(Specification.where(baseSpec).and((root, query, cb) -> cb.equal(root.get("role"), Role.AGENT)))
                : 0;
        userCount = (role == null || role == Role.USER)
                ? userRepository.count(Specification.where(baseSpec).and((root, query, cb) -> cb.equal(root.get("role"), Role.USER)))
                : 0;

        return new UserStatsResponse(adminCount, agentCount, userCount);
    }

    private Specification<User> buildUserSpecification(
            String username,
            String email,
            Role role,
            Boolean enabled,
            String expirationStatus,
            Long dealerId,
            boolean isAdmin,
            boolean isAgent,
            User currentUser
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter out soft-deleted users
            predicates.add(cb.or(cb.isNull(root.get("deleted")), cb.isFalse(root.get("deleted"))));

            // Data scope isolation: AGENT sees users where agent_id = currentAgentId OR dealer_id = currentDealerId
            if (!isAdmin && isAgent) {
                Long agentDealerId = currentUser.getDealer() != null ? currentUser.getDealer().getId() : null;
                if (agentDealerId != null) {
                    predicates.add(cb.or(
                            cb.equal(root.get("agent").get("id"), currentUser.getId()),
                            cb.equal(root.get("dealer").get("id"), agentDealerId)
                    ));
                } else {
                    predicates.add(cb.equal(root.get("agent").get("id"), currentUser.getId()));
                }
            }

            Join<User, Dealer> dealerJoin = null;
            Join<User, User> agentJoin = null;

            if (dealerId != null) {
                dealerJoin = root.join("dealer", JoinType.LEFT);
                agentJoin = root.join("agent", JoinType.LEFT);
                Join<User, Dealer> agentDealerJoin = agentJoin.join("dealer", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.equal(dealerJoin.get("id"), dealerId),
                        cb.equal(agentDealerJoin.get("id"), dealerId)
                ));
            }

            if (org.springframework.util.StringUtils.hasText(username)) {
                String pattern = "%" + username.trim().toLowerCase() + "%";
                if (dealerJoin == null) {
                    dealerJoin = root.join("dealer", JoinType.LEFT);
                }
                if (agentJoin == null) {
                    agentJoin = root.join("agent", JoinType.LEFT);
                }
                Join<Dealer, User> dealerAgentJoin = dealerJoin.join("agentUser", JoinType.LEFT);

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(agentJoin.get("username")), pattern),
                        cb.like(cb.lower(agentJoin.get("fullName")), pattern),
                        cb.like(cb.lower(dealerJoin.get("name")), pattern),
                        cb.like(cb.lower(dealerJoin.get("code")), pattern),
                        cb.like(cb.lower(dealerAgentJoin.get("username")), pattern),
                        cb.like(cb.lower(dealerAgentJoin.get("fullName")), pattern)
                ));
            }
            if (org.springframework.util.StringUtils.hasText(email)) {
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }
            if (org.springframework.util.StringUtils.hasText(expirationStatus)) {
                LocalDate today = LocalDate.now();
                switch (expirationStatus.toUpperCase()) {
                    case "EXPIRED" -> predicates.add(cb.and(
                            cb.notEqual(root.get("role"), Role.ADMIN),
                            cb.isNotNull(root.get("expirationDate")),
                            cb.lessThan(root.get("expirationDate"), today)
                    ));
                    case "VALID", "ACTIVE", "NOT_EXPIRED" -> predicates.add(cb.or(
                            cb.equal(root.get("role"), Role.ADMIN),
                            cb.isNull(root.get("expirationDate")),
                            cb.greaterThanOrEqualTo(root.get("expirationDate"), today)
                    ));
                    case "NO_EXPIRATION" -> predicates.add(cb.isNull(root.get("expirationDate")));
                    default -> {}
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(
            String username,
            String email,
            Role role,
            Boolean enabled,
            String expirationStatus,
            Long dealerId,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        User currentUser = currentUserService.getCurrentUser();
        boolean isAdmin = currentUserService.isAdmin();
        boolean isAgent = currentUserService.isAgent();

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String validSortBy = org.springframework.util.StringUtils.hasText(sortBy) ? sortBy : "createdAt";
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, validSortBy));

        Specification<User> spec = buildUserSpecification(
                username,
                email,
                role,
                enabled,
                expirationStatus,
                dealerId,
                isAdmin,
                isAgent,
                currentUser
        );

        Page<User> usersPage = userRepository.findAll(spec, pageable);
        return PageResponse.of(usersPage.map(userMapper::toUserResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = findScopedUserById(id);
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        boolean isAdmin = currentUserService.isAdmin();
        boolean isAgent = currentUserService.isAgent();

        if (!isAdmin && !isAgent) {
            throw new ForbiddenException("Access denied: only ADMIN or AGENT can create users");
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Địa chỉ email/gmail là bắt buộc");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // Username is mandatory and must follow strict standard rules
        String effectiveUsername = UsernameUtils.validateUsername(request.getUsername());

        if (userRepository.existsByUsername(effectiveUsername)) {
            throw new ConflictException("Tên đăng nhập đã tồn tại: " + effectiveUsername, ErrorCodes.USERNAME_ALREADY_EXISTS, "username");
        }

        String existingKcUser = keycloakUserService.findUserIdByUsername(effectiveUsername);
        if (existingKcUser != null) {
            throw new ConflictException("Tên đăng nhập đã tồn tại trên Keycloak: " + effectiveUsername, ErrorCodes.USERNAME_ALREADY_EXISTS, "username");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email/Gmail đã được sử dụng: " + normalizedEmail, ErrorCodes.EMAIL_ALREADY_EXISTS, "email");
        }

        Role assignedRole;
        User assignedAgent = null;
        Dealer targetDealer = null;
        Integer allocatedDays = request.getAllocatedDays();
        LocalDate effectiveExpirationDate = request.getExpirationDate();

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (effectiveExpirationDate != null && effectiveExpirationDate.isBefore(today)) {
            throw new BadRequestException("Không được chọn ngày trong quá khứ", "PAST_DATE_NOT_ALLOWED");
        }

        if (isAgent && !isAdmin) {
            // AGENT can only create USER under own agent scope
            if (request.getRole() != null && request.getRole() != Role.USER) {
                throw new ForbiddenException("Agent cannot create user with role: " + request.getRole());
            }
            if (request.getAgentId() != null && !request.getAgentId().equals(currentUser.getId())) {
                throw new ForbiddenException("Đại lý không được phép gán người dùng cho đại lý khác");
            }
            assignedRole = Role.USER;
            assignedAgent = currentUser;

            if (currentUser.getDealer() != null) {
                if (request.getDealerId() != null && !request.getDealerId().equals(currentUser.getDealer().getId())) {
                    throw new ForbiddenException("Đại lý không được phép thêm người dùng thuộc đại lý khác");
                }

                // Pessimistic write lock dealer for quota validation and atomic deduction
                targetDealer = dealerRepository.findByIdForUpdate(currentUser.getDealer().getId())
                        .orElseThrow(() -> new BadRequestException("Đại lý không tồn tại"));

                // Package validations
                if (!"ACTIVE".equalsIgnoreCase(targetDealer.getPackageStatus())) {
                    throw new BadRequestException("Gói đại lý đang ở trạng thái không hoạt động (" + targetDealer.getPackageStatus() + ")");
                }
                if (targetDealer.isPackageExpired()) {
                    throw new BadRequestException("Gói đại lý đã hết hạn vào ngày " + targetDealer.getPackageExpiryDate() + ", không thể tạo tài khoản mới");
                }

                long currentAccounts = dealerRepository.countActiveUserAccountsByDealerId(targetDealer.getId());
                if (currentAccounts >= targetDealer.getMaxAccounts()) {
                    throw new BadRequestException("Đại lý đã đạt giới hạn tối đa " + targetDealer.getMaxAccounts() + " tài khoản Thợ cắt");
                }

                if (allocatedDays != null && allocatedDays <= 0) {
                    throw new BadRequestException("Số ngày phân bổ phải lớn hơn 0");
                }

                if (allocatedDays == null) {
                    if (request.getExpirationDate() != null) {
                        allocatedDays = Math.max(1, (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), request.getExpirationDate()));
                    } else {
                        allocatedDays = 360; // Gói mặc định quy ước chuẩn 360 ngày / 1 năm
                    }
                }

                int remainingDays = targetDealer.getRemainingAllocatedDays();
                if (allocatedDays > remainingDays) {
                    throw new BadRequestException("Quỹ ngày còn lại của đại lý không đủ (còn lại: " + remainingDays + " ngày, yêu cầu: " + allocatedDays + " ngày)");
                }

                effectiveExpirationDate = LocalDate.now().plusDays(allocatedDays);
                if (targetDealer.getPackageExpiryDate() != null && effectiveExpirationDate.isAfter(targetDealer.getPackageExpiryDate())) {
                    throw new BadRequestException("Thời hạn tài khoản vượt quá ngày hết hạn của gói đại lý (" + targetDealer.getPackageExpiryDate() + ")");
                }
            } else {
                if (request.getDealerId() != null) {
                    throw new ForbiddenException("Tài khoản đại lý chưa được gán vào đại lý nào, không thể gán đại lý khác");
                }
                targetDealer = null;
            }
        } else {
            // ADMIN can create any role
            assignedRole = request.getRole() != null ? request.getRole() : Role.USER;

            if (assignedRole == Role.AGENT) {
                // Rule: Mỗi đại lý chỉ có duy nhất một tài khoản role AGENT
                if (request.getDealerId() != null && request.getDealerId() > 0) {
                    targetDealer = dealerRepository.findByIdForUpdate(request.getDealerId())
                            .orElseThrow(() -> new BadRequestException("Đại lý không tồn tại với ID: " + request.getDealerId()));
                    if (targetDealer.getAgentUser() != null || userRepository.existsByDealerIdAndRoleAndDeletedFalse(targetDealer.getId(), Role.AGENT)) {
                        throw new ConflictException("Đại lý '" + targetDealer.getName() + "' đã có tài khoản AGENT liên kết. Mỗi đại lý chỉ có duy nhất một tài khoản role AGENT.");
                    }
                } else {
                    targetDealer = null;
                }
                // Tài khoản AGENT không tính vào gói account và không tiêu thụ Quỹ ngày
                allocatedDays = 0;
                effectiveExpirationDate = null;
                assignedAgent = null;
            } else if (assignedRole == Role.USER) {
                if (request.getDealerId() != null && request.getDealerId() > 0) {
                    targetDealer = dealerRepository.findByIdForUpdate(request.getDealerId())
                            .orElseThrow(() -> new BadRequestException("Đại lý không tồn tại với ID: " + request.getDealerId()));
                    if (!"ACTIVE".equalsIgnoreCase(targetDealer.getPackageStatus())) {
                        throw new BadRequestException("Gói đại lý đang ở trạng thái không hoạt động (" + targetDealer.getPackageStatus() + ")");
                    }
                    if (targetDealer.isPackageExpired()) {
                        throw new BadRequestException("Gói đại lý đã hết hạn vào ngày " + targetDealer.getPackageExpiryDate() + ", không thể tạo tài khoản mới");
                    }
                    long currentAccounts = dealerRepository.countActiveUserAccountsByDealerId(targetDealer.getId());
                    if (currentAccounts >= targetDealer.getMaxAccounts()) {
                        throw new BadRequestException("Đại lý đã đạt giới hạn tối đa " + targetDealer.getMaxAccounts() + " tài khoản Thợ cắt");
                    }
                    if (allocatedDays != null && allocatedDays <= 0) {
                        throw new BadRequestException("Số ngày phân bổ cho Thợ cắt phải lớn hơn 0");
                    }
                    if (allocatedDays == null) {
                        if (request.getExpirationDate() != null) {
                            allocatedDays = Math.max(1, (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), request.getExpirationDate()));
                        } else {
                            allocatedDays = 360; // Gói mặc định quy ước chuẩn 360 ngày / 1 năm
                        }
                    }
                    int remainingDays = targetDealer.getRemainingAllocatedDays();
                    if (allocatedDays > remainingDays) {
                        throw new BadRequestException("Quỹ ngày còn lại của đại lý không đủ (còn lại: " + remainingDays + " ngày, yêu cầu: " + allocatedDays + " ngày)");
                    }
                    effectiveExpirationDate = LocalDate.now().plusDays(allocatedDays);
                    if (targetDealer.getPackageExpiryDate() != null && effectiveExpirationDate.isAfter(targetDealer.getPackageExpiryDate())) {
                        throw new BadRequestException("Thời hạn tài khoản vượt quá ngày hết hạn của gói đại lý (" + targetDealer.getPackageExpiryDate() + ")");
                    }
                    assignedAgent = resolveDealerAgentUser(targetDealer);
                } else {
                    targetDealer = null;
                    if (request.getAgentId() != null && request.getAgentId() > 0) {
                        assignedAgent = userRepository.findById(request.getAgentId())
                                .filter(u -> u.getRole() == Role.AGENT && !u.isDeleted())
                                .orElse(null);
                    } else {
                        assignedAgent = null;
                    }
                }
            } else {
                // ADMIN role
                targetDealer = null;
                assignedAgent = null;
                allocatedDays = 0;
                effectiveExpirationDate = null;
            }
        }

        boolean enabled = request.getEnabled() != null ? request.getEnabled() : true;

        // Step 1: Create user in Keycloak (Identity Source of Truth)
        String keycloakUserId = keycloakUserService.createUser(
                effectiveUsername,
                normalizedEmail,
                PasswordUtils.trimPassword(request.getPassword()),
                assignedRole,
                enabled,
                request.getFullName()
        );

        // Step 2: Create application user in PostgreSQL (Business Profile Source of Truth)
        // With rollback compensation if DB operation fails
        try {
            if (targetDealer != null && assignedRole == Role.USER && allocatedDays != null && allocatedDays > 0) {
                targetDealer.setUsedAllocatedDays(targetDealer.getUsedAllocatedDays() + allocatedDays);
                dealerRepository.save(targetDealer);
            }

            User user = User.builder()
                    .keycloakUserId(keycloakUserId)
                    .username(effectiveUsername)
                    .email(normalizedEmail)
                    .fullName(request.getFullName())
                    .phone(request.getPhone())
                    .role(assignedRole)
                    .agent(assignedAgent)
                    .dealer(targetDealer)
                    .enabled(enabled)
                    .deleted(false)
                    .allocatedDays(allocatedDays != null ? allocatedDays : 0)
                    .expirationDate(effectiveExpirationDate)
                    .build();

            User savedUser = userRepository.save(user);

            // If creating an AGENT, link agent_user_id on Dealer
            if (targetDealer != null && assignedRole == Role.AGENT) {
                targetDealer.setAgentUser(savedUser);
                dealerRepository.save(targetDealer);
            }

            if (request.getPartCategoryIds() != null && !request.getPartCategoryIds().isEmpty()
                    && userPartCategoryPermissionRepository != null && partLibraryCategoryRepository != null) {
                List<PartLibraryCategory> activeCategories = partLibraryCategoryRepository.findByIdInAndStatus(request.getPartCategoryIds(), "ACTIVE");
                if (activeCategories.size() != request.getPartCategoryIds().size()) {
                    throw new BadRequestException("Một hoặc nhiều danh mục không tồn tại hoặc không ở trạng thái ACTIVE");
                }
                for (PartLibraryCategory cat : activeCategories) {
                    UserPartCategoryPermission perm = new UserPartCategoryPermission(savedUser, cat, currentUser.getUsername());
                    userPartCategoryPermissionRepository.save(perm);
                }
            }

            if (targetDealer != null && assignedRole == Role.USER && allocatedDays != null && allocatedDays > 0 && dealerPackageHistoryRepository != null) {
                DealerPackageHistory history = DealerPackageHistory.builder()
                        .dealer(targetDealer)
                        .action("ALLOCATE_USER")
                        .previousMaxAccounts(targetDealer.getMaxAccounts())
                        .newMaxAccounts(targetDealer.getMaxAccounts())
                        .previousTotalDays(targetDealer.getTotalAllocatedDays())
                        .newTotalDays(targetDealer.getTotalAllocatedDays())
                        .usedDays(targetDealer.getUsedAllocatedDays())
                        .changeDays(allocatedDays)
                        .user(savedUser)
                        .notes("Phân bổ " + allocatedDays + " ngày cho tài khoản " + savedUser.getUsername())
                        .createdBy(currentUser.getUsername())
                        .build();
                dealerPackageHistoryRepository.save(history);
            }

            log.info("[USER_CREATED] Created user: username='{}', email='{}', id={}, keycloakId='{}', role={}, agentId={}, dealerId={}, allocatedDays={}, expirationDate={}",
                    savedUser.getUsername(), savedUser.getEmail(), savedUser.getId(), keycloakUserId, savedUser.getRole(),
                    assignedAgent != null ? assignedAgent.getId() : null,
                    targetDealer != null ? targetDealer.getId() : null,
                    savedUser.getAllocatedDays(), savedUser.getExpirationDate());

            if (savedUser.getExpirationDate() != null && auditLogService != null) {
                String actorUsername = currentUser.getUsername();
                String actorRole = currentUser.getRole() != null ? currentUser.getRole().name() : "ADMIN";
                String details = String.format("targetUserId=%d, operatorUserId=%d, oldExpirationDate=null, newExpirationDate=%s",
                        savedUser.getId(), currentUser.getId(), savedUser.getExpirationDate());
                auditLogService.log(actorUsername, actorRole, "USER_EXPIRATION_DATE_CREATED", "User", savedUser.getId(), details);
            }

            return userMapper.toUserResponse(savedUser);
        } catch (Exception ex) {
            log.error("[COMPENSATION_REQUIRED] Database save failed for user '{}' after Keycloak creation. Initiating rollback in Keycloak...",
                    effectiveUsername, ex);
            try {
                keycloakUserService.deleteUser(keycloakUserId);
                log.info("[COMPENSATION_SUCCESS] Cleaned up orphaned Keycloak user: '{}' (ID: {})",
                        effectiveUsername, keycloakUserId);
            } catch (Exception kcEx) {
                log.error("[COMPENSATION_FAILED] Failed to clean up Keycloak user '{}' (ID: {}). Manual cleanup may be required: {}",
                        effectiveUsername, keycloakUserId, kcEx.getMessage(), kcEx);
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findScopedUserById(id);
        User currentUser = currentUserService.getCurrentUser();
        boolean isAdmin = currentUserService.isAdmin();
        boolean isAgent = currentUserService.isAgent();

        String oldEmail = user.getEmail();
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
            throw new ConflictException("Email/Gmail đã được sử dụng bởi người dùng khác: " + normalizedEmail, ErrorCodes.EMAIL_ALREADY_EXISTS, "email");
        }

        // Handle username update if provided and changed
        String requestedUsername = UsernameUtils.validateOptionalUsername(request.getUsername());
        if (requestedUsername != null && !requestedUsername.equals(user.getUsername())) {
            if (userRepository.existsByUsernameAndIdNot(requestedUsername, id)) {
                throw new ConflictException("Tên đăng nhập đã tồn tại: " + requestedUsername, ErrorCodes.USERNAME_ALREADY_EXISTS, "username");
            }
            String existingKcId = keycloakUserService.findUserIdByUsername(requestedUsername);
            if (existingKcId != null && !existingKcId.equals(user.getKeycloakUserId())) {
                throw new ConflictException("Tên đăng nhập đã tồn tại trên Keycloak: " + requestedUsername, ErrorCodes.USERNAME_ALREADY_EXISTS, "username");
            }
            if (StringUtils.hasText(user.getKeycloakUserId())) {
                keycloakUserService.updateUsername(user.getKeycloakUserId(), requestedUsername);
            }
            user.setUsername(requestedUsername);
        }

        user.setEmail(normalizedEmail);
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());

        if (request.getEnabled() != null) {
            if (currentUser.getId().equals(id) && !request.getEnabled()) {
                throw new BadRequestException("Không thể tự vô hiệu hóa tài khoản của chính mình");
            }
            user.setEnabled(request.getEnabled());
        }

        LocalDate oldExpirationDate = user.getExpirationDate();
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (request.getExpirationDate() != null
                && !Objects.equals(request.getExpirationDate(), oldExpirationDate)
                && request.getExpirationDate().isBefore(today)) {
            throw new BadRequestException("Không được chọn ngày trong quá khứ", "PAST_DATE_NOT_ALLOWED");
        }
        Role oldRole = user.getRole();
        Role newRole = (isAdmin && request.getRole() != null) ? request.getRole() : oldRole;
        Dealer oldDealer = user.getDealer();
        Dealer targetDealer = oldDealer;

        if (isAdmin) {
            if (request.getDealerId() != null) {
                if (request.getDealerId() > 0) {
                    targetDealer = dealerRepository.findByIdForUpdate(request.getDealerId())
                            .orElseThrow(() -> new BadRequestException("Đại lý không tồn tại với ID: " + request.getDealerId()));
                } else {
                    targetDealer = null;
                }
            } else if (targetDealer != null && targetDealer.isDeleted()) {
                targetDealer = null;
            }
        }

        boolean dealerChanged = (oldDealer == null && targetDealer != null)
                || (oldDealer != null && targetDealer == null)
                || (oldDealer != null && targetDealer != null && !Objects.equals(oldDealer.getId(), targetDealer.getId()));

        if (newRole == Role.ADMIN) {
            if (dealerChanged && oldDealer != null && !oldDealer.isDeleted()) {
                int oldDays = user.getAllocatedDays() != null ? user.getAllocatedDays() : 0;
                if (oldDays > 0) {
                    dealerRepository.findByIdForUpdate(oldDealer.getId()).ifPresent(d -> {
                        d.setUsedAllocatedDays(Math.max(0, d.getUsedAllocatedDays() - oldDays));
                        dealerRepository.save(d);
                    });
                }
            }
            user.setDealer(null);
            user.setAgent(null);
            user.setAllocatedDays(0);
            user.setExpirationDate(null);
        } else if (newRole == Role.AGENT) {
            // Rule: Mỗi đại lý chỉ có duy nhất một tài khoản AGENT
            if (targetDealer != null) {
                if (userRepository.existsByDealerIdAndRoleAndIdNotAndDeletedFalse(targetDealer.getId(), Role.AGENT, user.getId())) {
                    throw new ConflictException("Đại lý '" + targetDealer.getName() + "' đã có tài khoản AGENT liên kết. Mỗi đại lý chỉ có duy nhất một tài khoản role AGENT.");
                }
            }
            if (dealerChanged && oldDealer != null && !oldDealer.isDeleted()) {
                int oldDays = user.getAllocatedDays() != null ? user.getAllocatedDays() : 0;
                if (oldDays > 0) {
                    dealerRepository.findByIdForUpdate(oldDealer.getId()).ifPresent(d -> {
                        d.setUsedAllocatedDays(Math.max(0, d.getUsedAllocatedDays() - oldDays));
                        dealerRepository.save(d);
                    });
                }
            }
            if (oldDealer != null && !Objects.equals(oldDealer.getId(), targetDealer != null ? targetDealer.getId() : null)) {
                if (oldDealer.getAgentUser() != null && Objects.equals(oldDealer.getAgentUser().getId(), user.getId())) {
                    oldDealer.setAgentUser(null);
                    dealerRepository.save(oldDealer);
                }
            }
            if (targetDealer != null) {
                targetDealer.setAgentUser(user);
                dealerRepository.save(targetDealer);
            }
            user.setDealer(targetDealer);
            user.setAgent(null);
            user.setAllocatedDays(0);
            user.setExpirationDate(null);
        } else {
            // Role.USER
            if (targetDealer == null) {
                if (dealerChanged && oldDealer != null && !oldDealer.isDeleted()) {
                    int oldDays = user.getAllocatedDays() != null ? user.getAllocatedDays() : 0;
                    if (oldDays > 0) {
                        dealerRepository.findByIdForUpdate(oldDealer.getId()).ifPresent(d -> {
                            d.setUsedAllocatedDays(Math.max(0, d.getUsedAllocatedDays() - oldDays));
                            dealerRepository.save(d);
                        });
                    }
                }
                user.setDealer(null);
                user.setAllocatedDays(0);
                if (isAgent && !isAdmin && request.getExpirationDate() != null && !Objects.equals(request.getExpirationDate(), oldExpirationDate)) {
                    throw new BadRequestException("Đại lý phải chọn thời hạn theo số ngày phân bổ từ quỹ gói");
                }
                user.setExpirationDate(request.getExpirationDate());
                if (request.getAgentId() != null) {
                    if (request.getAgentId() > 0) {
                        User agent = userRepository.findById(request.getAgentId())
                                .filter(u -> u.getRole() == Role.AGENT && !u.isDeleted())
                                .orElseThrow(() -> new BadRequestException("Assigned agent not found with id: " + request.getAgentId()));
                        user.setAgent(agent);
                    } else {
                        user.setAgent(null);
                    }
                }
            } else if (dealerChanged) {
                // Moving into a new dealer
                if (!"ACTIVE".equalsIgnoreCase(targetDealer.getPackageStatus())) {
                    throw new BadRequestException("Gói đại lý đang ở trạng thái không hoạt động (" + targetDealer.getPackageStatus() + ")");
                }
                if (targetDealer.isPackageExpired()) {
                    throw new BadRequestException("Gói đại lý đã hết hạn vào ngày " + targetDealer.getPackageExpiryDate() + ", không thể tạo/gán tài khoản");
                }
                long currentAccounts = dealerRepository.countActiveUserAccountsByDealerId(targetDealer.getId());
                if (currentAccounts >= targetDealer.getMaxAccounts()) {
                    throw new BadRequestException("Đại lý đã đạt giới hạn tối đa " + targetDealer.getMaxAccounts() + " tài khoản Thợ cắt");
                }

                int newDays = (request.getAllocatedDays() != null && request.getAllocatedDays() > 0)
                        ? request.getAllocatedDays()
                        : (user.getAllocatedDays() != null && user.getAllocatedDays() > 0 ? user.getAllocatedDays() : 360);

                int remainingDays = targetDealer.getRemainingAllocatedDays();
                if (newDays > remainingDays) {
                    throw new BadRequestException("Quỹ ngày còn lại của đại lý không đủ (còn lại: " + remainingDays + " ngày, yêu cầu: " + newDays + " ngày)");
                }

                LocalDate calculatedExp = LocalDate.now().plusDays(newDays);
                if (targetDealer.getPackageExpiryDate() != null && calculatedExp.isAfter(targetDealer.getPackageExpiryDate())) {
                    throw new BadRequestException("Thời hạn tài khoản vượt quá ngày hết hạn của gói đại lý (" + targetDealer.getPackageExpiryDate() + ")");
                }

                targetDealer.setUsedAllocatedDays(targetDealer.getUsedAllocatedDays() + newDays);
                dealerRepository.save(targetDealer);

                // Refund old dealer if valid and not deleted
                if (oldDealer != null && !oldDealer.isDeleted()) {
                    int oldDays = user.getAllocatedDays() != null ? user.getAllocatedDays() : 0;
                    if (oldDays > 0) {
                        dealerRepository.findByIdForUpdate(oldDealer.getId()).ifPresent(d -> {
                            d.setUsedAllocatedDays(Math.max(0, d.getUsedAllocatedDays() - oldDays));
                            dealerRepository.save(d);
                        });
                    }
                }

                user.setDealer(targetDealer);
                user.setAgent(resolveDealerAgentUser(targetDealer));
                user.setAllocatedDays(newDays);
                user.setExpirationDate(calculatedExp);

                if (dealerPackageHistoryRepository != null) {
                    DealerPackageHistory history = DealerPackageHistory.builder()
                            .dealer(targetDealer)
                            .action("ALLOCATE_USER")
                            .previousMaxAccounts(targetDealer.getMaxAccounts())
                            .newMaxAccounts(targetDealer.getMaxAccounts())
                            .previousTotalDays(targetDealer.getTotalAllocatedDays())
                            .newTotalDays(targetDealer.getTotalAllocatedDays())
                            .usedDays(targetDealer.getUsedAllocatedDays())
                            .changeDays(newDays)
                            .user(user)
                            .notes("Gán tài khoản " + user.getUsername() + " vào đại lý và phân bổ " + newDays + " ngày")
                            .createdBy(currentUser.getUsername())
                            .build();
                    dealerPackageHistoryRepository.save(history);
                }
            } else {
                // Same dealer
                user.setDealer(targetDealer);
                if (user.getAgent() == null) {
                    user.setAgent(resolveDealerAgentUser(targetDealer));
                }

                if (request.getAllocatedDays() != null) {
                    int newDays = request.getAllocatedDays();
                    if (newDays <= 0) {
                        throw new BadRequestException("Số ngày phân bổ phải lớn hơn 0");
                    }
                    int oldDays = user.getAllocatedDays() != null ? user.getAllocatedDays() : 0;
                    if (newDays != oldDays) {
                        Dealer lockedDealer = dealerRepository.findByIdForUpdate(targetDealer.getId())
                                .orElseThrow(() -> new BadRequestException("Đại lý không tồn tại"));
                        int delta = newDays - oldDays;
                        if (delta > 0) {
                            if (!"ACTIVE".equalsIgnoreCase(lockedDealer.getPackageStatus())) {
                                throw new BadRequestException("Gói đại lý đang ở trạng thái không hoạt động (" + lockedDealer.getPackageStatus() + ")");
                            }
                            if (lockedDealer.isPackageExpired()) {
                                throw new BadRequestException("Gói đại lý đã hết hạn, không thể gia hạn thêm ngày");
                            }
                            int remaining = lockedDealer.getRemainingAllocatedDays();
                            if (delta > remaining) {
                                throw new BadRequestException("Quỹ ngày còn lại của đại lý không đủ (còn lại: " + remaining + " ngày, cần thêm: " + delta + " ngày)");
                            }
                        }

                        LocalDate calculatedExp;
                        if (oldExpirationDate != null && !oldExpirationDate.isBefore(LocalDate.now())) {
                            calculatedExp = oldExpirationDate.plusDays(delta);
                        } else {
                            calculatedExp = LocalDate.now().plusDays(newDays);
                        }

                        if (lockedDealer.getPackageExpiryDate() != null && calculatedExp.isAfter(lockedDealer.getPackageExpiryDate())) {
                            throw new BadRequestException("Thời hạn tài khoản vượt quá ngày hết hạn của gói đại lý (" + lockedDealer.getPackageExpiryDate() + ")");
                        }

                        lockedDealer.setUsedAllocatedDays(lockedDealer.getUsedAllocatedDays() + delta);
                        dealerRepository.save(lockedDealer);

                        user.setAllocatedDays(newDays);
                        user.setExpirationDate(calculatedExp);

                        if (dealerPackageHistoryRepository != null) {
                            DealerPackageHistory history = DealerPackageHistory.builder()
                                    .dealer(lockedDealer)
                                    .action(delta > 0 ? "EXTEND_USER" : "REDUCE_USER")
                                    .previousMaxAccounts(lockedDealer.getMaxAccounts())
                                    .newMaxAccounts(lockedDealer.getMaxAccounts())
                                    .previousTotalDays(lockedDealer.getTotalAllocatedDays())
                                    .newTotalDays(lockedDealer.getTotalAllocatedDays())
                                    .usedDays(lockedDealer.getUsedAllocatedDays())
                                    .changeDays(delta)
                                    .user(user)
                                    .notes((delta > 0 ? "Gia hạn thêm " : "Giảm bớt ") + Math.abs(delta) + " ngày cho tài khoản " + user.getUsername())
                                    .createdBy(currentUser.getUsername())
                                    .build();
                            dealerPackageHistoryRepository.save(history);
                        }
                    }
                } else if (isAgent && !isAdmin && request.getExpirationDate() != null && !Objects.equals(request.getExpirationDate(), oldExpirationDate)) {
                    throw new BadRequestException("Đại lý phải chọn thời hạn theo số ngày phân bổ từ quỹ gói");
                } else if (isAdmin) {
                    user.setExpirationDate(request.getExpirationDate());
                }
            }
        }

        if (request.getRole() != null && request.getRole() != oldRole) {
            user.setRole(newRole);
            if (StringUtils.hasText(user.getKeycloakUserId())) {
                keycloakUserService.updateRole(user.getKeycloakUserId(), oldRole, request.getRole());
            }
        }

        if (!Objects.equals(oldExpirationDate, user.getExpirationDate()) && auditLogService != null) {
            String auditAction;
            if (oldExpirationDate == null && user.getExpirationDate() != null) {
                auditAction = "USER_EXPIRATION_DATE_CREATED";
            } else if (oldExpirationDate != null && user.getExpirationDate() == null) {
                auditAction = "USER_EXPIRATION_DATE_REMOVED";
            } else {
                auditAction = "USER_EXPIRATION_DATE_UPDATED";
            }
            String actorUsername = currentUser.getUsername();
            String actorRole = currentUser.getRole() != null ? currentUser.getRole().name() : "ADMIN";
            String details = String.format("targetUserId=%d, operatorUserId=%d, oldExpirationDate=%s, newExpirationDate=%s",
                    user.getId(), currentUser.getId(), oldExpirationDate, user.getExpirationDate());
            auditLogService.log(actorUsername, actorRole, auditAction, "User", user.getId(), details);
        }

        // F-57: số máy tối đa — chỉ ADMIN đổi được (Q3 chốt 28/09). 0 = về mặc định hệ thống.
        if (request.getMaxDevices() != null) {
            Integer wanted = request.getMaxDevices() > 0 ? request.getMaxDevices() : null;
            if (!Objects.equals(wanted, user.getMaxDevices())) {
                if (!currentUserService.isAdmin()) {
                    throw new ForbiddenException("Chỉ ADMIN được đổi số máy tối đa của tài khoản");
                }
                log.info("[USER_MAX_DEVICES] User id={} ({}) maxDevices {} -> {}",
                        user.getId(), user.getUsername(), user.getMaxDevices(), wanted);
                user.setMaxDevices(wanted);
            }
        }

        // Sync updates to Keycloak
        if (StringUtils.hasText(user.getKeycloakUserId())) {
            keycloakUserService.updateUser(user.getKeycloakUserId(), normalizedEmail, request.getEnabled(), user.getFullName());
            String rawPass = PasswordUtils.trimOptionalPassword(request.getPassword());
            if (StringUtils.hasText(rawPass)) {
                if (!currentUserService.isAdmin() && !currentUser.getId().equals(id)) {
                    throw new ForbiddenException("Chỉ ADMIN mới có quyền đặt lại mật khẩu cho người dùng khác");
                }
                keycloakUserService.resetPassword(user.getKeycloakUserId(), rawPass);
            }
        }

        if (request.getPartCategoryIds() != null && userPartCategoryPermissionRepository != null && partLibraryCategoryRepository != null) {
            if (request.getPartCategoryIds().isEmpty()) {
                userPartCategoryPermissionRepository.deleteByUserId(user.getId());
            } else {
                List<PartLibraryCategory> activeCategories = partLibraryCategoryRepository.findByIdInAndStatus(request.getPartCategoryIds(), "ACTIVE");
                if (activeCategories.size() != request.getPartCategoryIds().size()) {
                    throw new BadRequestException("Một hoặc nhiều danh mục không tồn tại hoặc không ở trạng thái ACTIVE");
                }
                userPartCategoryPermissionRepository.deleteByUserIdAndPartLibraryCategoryIdNotIn(user.getId(), request.getPartCategoryIds());
                List<Long> currentIds = userPartCategoryPermissionRepository.findPartCategoryIdsByUserId(user.getId());
                for (PartLibraryCategory cat : activeCategories) {
                    if (!currentIds.contains(cat.getId())) {
                        UserPartCategoryPermission perm = new UserPartCategoryPermission(user, cat, currentUser.getUsername());
                        userPartCategoryPermissionRepository.save(perm);
                    }
                }
            }
        }

        User updatedUser = userRepository.save(user);
        log.info("[USER_UPDATED] User id={} ({}) updated successfully. Email changed: {} -> {}, allocatedDays={}",
                user.getId(), user.getUsername(), oldEmail, normalizedEmail, user.getAllocatedDays());

        return userMapper.toUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        if (!currentUserService.isAdmin()) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền đặt lại mật khẩu cho người dùng");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (user.isDeleted()) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }

        String keycloakUserId = user.getKeycloakUserId();
        if (!StringUtils.hasText(keycloakUserId)) {
            // Fallback self-heal attempt by username lookup in Keycloak
            keycloakUserId = keycloakUserService.findUserIdByUsername(user.getUsername());
            if (StringUtils.hasText(keycloakUserId)) {
                user.setKeycloakUserId(keycloakUserId);
                userRepository.save(user);
            } else {
                throw new BadRequestException("Tài khoản chưa được liên kết với hệ thống xác thực Keycloak");
            }
        }

        keycloakUserService.resetPassword(keycloakUserId, PasswordUtils.trimPassword(request.getNewPassword()));

        if (auditLogService != null) {
            String actorUsername = currentUser.getUsername();
            String actorRole = currentUser.getRole() != null ? currentUser.getRole().name() : "ADMIN";
            String details = String.format("targetUserId=%d, targetUsername='%s', operatorUserId=%d",
                    user.getId(), user.getUsername(), currentUser.getId());
            auditLogService.log(actorUsername, actorRole, "ADMIN_RESET_PASSWORD", "User", user.getId(), details);
        }

        log.info("[ADMIN_PASSWORD_RESET] Admin id={} ({}) reset password for user id={} ({}) successfully",
                currentUser.getId(), currentUser.getUsername(), user.getId(), user.getUsername());
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
        User user = findScopedUserById(id);
        user.setEnabled(request.getEnabled());
        User updatedUser = userRepository.save(user);

        if (StringUtils.hasText(user.getKeycloakUserId())) {
            keycloakUserService.updateUser(user.getKeycloakUserId(), user.getEmail(), request.getEnabled());
        }

        if (!request.getEnabled()) {
            log.info("[USER_DISABLED] User id={} has been disabled", user.getId());
        } else {
            log.info("[USER_UPDATED] User id={} enabled status set to {}", user.getId(), request.getEnabled());
        }

        return userMapper.toUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUserRole(Long id, UpdateUserRoleRequest request) {
        if (!currentUserService.isAdmin()) {
            throw new ForbiddenException("Only ADMIN can directly change user roles");
        }

        User user = findScopedUserById(id);
        Role oldRole = user.getRole();

        if (request.getRole() == Role.AGENT && user.getDealer() != null) {
            if (userRepository.existsByDealerIdAndRoleAndIdNotAndDeletedFalse(user.getDealer().getId(), Role.AGENT, user.getId())) {
                throw new ConflictException("Đại lý '" + user.getDealer().getName() + "' đã có tài khoản AGENT liên kết. Mỗi đại lý chỉ có duy nhất một tài khoản role AGENT.");
            }
            user.setAllocatedDays(0);
            user.setExpirationDate(null);
            user.getDealer().setAgentUser(user);
            dealerRepository.save(user.getDealer());
        }

        user.setRole(request.getRole());

        if (StringUtils.hasText(user.getKeycloakUserId())) {
            keycloakUserService.updateRole(user.getKeycloakUserId(), oldRole, request.getRole());
        }

        User updatedUser = userRepository.save(user);
        log.info("[ROLE_CHANGED] User id={} ({}) role changed from {} to {}", user.getId(), user.getUsername(), oldRole, request.getRole());
        return userMapper.toUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getId().equals(id)) {
            throw new BadRequestException("Cannot delete your own account");
        }

        User user = findScopedUserById(id);

        if (user.getRole() == Role.AGENT && user.getDealer() != null) {
            Dealer d = user.getDealer();
            if (d.getAgentUser() != null && Objects.equals(d.getAgentUser().getId(), user.getId())) {
                d.setAgentUser(null);
                dealerRepository.save(d);
            }
        }

        if (user.getDealer() != null && dealerPackageHistoryRepository != null) {
            DealerPackageHistory history = DealerPackageHistory.builder()
                    .dealer(user.getDealer())
                    .action("DELETE_USER")
                    .previousMaxAccounts(user.getDealer().getMaxAccounts())
                    .newMaxAccounts(user.getDealer().getMaxAccounts())
                    .previousTotalDays(user.getDealer().getTotalAllocatedDays())
                    .newTotalDays(user.getDealer().getTotalAllocatedDays())
                    .usedDays(user.getDealer().getUsedAllocatedDays())
                    .changeDays(0)
                    .user(user)
                    .notes("Xóa tài khoản " + user.getUsername() + ", không hoàn lại " + (user.getAllocatedDays() != null ? user.getAllocatedDays() : 0) + " ngày đã phân bổ theo chính sách hiện tại")
                    .createdBy(currentUser.getUsername())
                    .build();
            dealerPackageHistoryRepository.save(history);
        }

        boolean hasSvg = svgFileRepository.existsByUploadedBy(user);
        boolean hasChildUsers = userRepository.existsByAgent(user);

        // Soft-delete if user has business references (SVGs, child users) to preserve audit/history
        if (hasSvg || hasChildUsers) {
            long ts = System.currentTimeMillis();
            String originalUsername = user.getUsername();
            user.setUsername(appendDeletedSuffix(user.getUsername(), ts, 255));
            user.setEmail(appendDeletedSuffix(user.getEmail(), ts, 255));
            user.setDeleted(true);
            user.setEnabled(false);
            userRepository.save(user);

            if (StringUtils.hasText(user.getKeycloakUserId())) {
                try {
                    keycloakUserService.deleteUser(user.getKeycloakUserId());
                } catch (Exception e) {
                    log.error("Failed to delete Keycloak user {} on soft delete", user.getKeycloakUserId(), e);
                }
            }
            log.info("[USER_SOFT_DELETED] User soft-deleted to maintain business history: id={}, originalUsername='{}', hasSvg={}, hasChildUsers={}", user.getId(), originalUsername, hasSvg, hasChildUsers);
            return;
        }

        // Hard-delete if no business references exist
        if (userPartCategoryPermissionRepository != null) {
            userPartCategoryPermissionRepository.deleteByUserId(user.getId());
        }
        userRepository.delete(user);
        if (StringUtils.hasText(user.getKeycloakUserId())) {
            keycloakUserService.deleteUser(user.getKeycloakUserId());
        }
        log.info("[USER_DELETED] User hard-deleted successfully: id={}, username='{}'", id, user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDeviceResponse> getUserDevices(Long id) {
        User user = findScopedUserById(id);
        return userDeviceService.listDevices(user, null);
    }

    @Override
    @Transactional
    public UserDeviceResponse revokeUserDevice(Long id, Long deviceRegistrationId) {
        User user = findScopedUserById(id);
        User actor = currentUserService.getCurrentUser();
        return userDeviceService.revoke(user, deviceRegistrationId, actor.getUsername(), actor.getRole().name());
    }

    private User findScopedUserById(Long id) {
        User user;
        if (currentUserService.isAdmin()) {
            user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        } else if (currentUserService.isAgent()) {
            User currentUser = currentUserService.getCurrentUser();
            Long currentAgentId = currentUser.getId();
            Long agentDealerId = currentUser.getDealer() != null ? currentUser.getDealer().getId() : null;

            user = userRepository.findById(id)
                    .filter(u -> (u.getAgent() != null && u.getAgent().getId().equals(currentAgentId))
                            || (agentDealerId != null && u.getDealer() != null && u.getDealer().getId().equals(agentDealerId)))
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        } else {
            throw new ForbiddenException("Access denied: insufficient privileges");
        }

        if (user.isDeleted()) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }

        return user;
    }

    private User resolveDealerAgentUser(Dealer dealer) {
        if (dealer == null) {
            return null;
        }
        if (dealer.getAgentUser() != null) {
            return dealer.getAgentUser();
        }
        return userRepository.findByDealerIdAndRoleAndDeletedFalse(dealer.getId(), Role.AGENT).orElse(null);
    }

    private static String appendDeletedSuffix(String value, long ts, int maxLength) {
        if (value == null) return null;
        String suffix = "_deleted_" + ts;
        if (value.length() + suffix.length() > maxLength) {
            return value.substring(0, maxLength - suffix.length()) + suffix;
        }
        return value + suffix;
    }
}
