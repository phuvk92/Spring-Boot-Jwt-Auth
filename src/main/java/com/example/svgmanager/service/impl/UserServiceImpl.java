package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.request.CreateUserRequest;
import com.example.svgmanager.dto.request.UpdateUserRequest;
import com.example.svgmanager.dto.request.UpdateUserRoleRequest;
import com.example.svgmanager.dto.request.UpdateUserStatusRequest;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.UserResponse;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ConflictException;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.mapper.UserMapper;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.KeycloakUserService;
import com.example.svgmanager.service.UserService;
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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final SvgFileRepository svgFileRepository;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;
    private final KeycloakUserService keycloakUserService;

    public UserServiceImpl(
            UserRepository userRepository,
            SvgFileRepository svgFileRepository,
            UserMapper userMapper,
            CurrentUserService currentUserService,
            KeycloakUserService keycloakUserService
    ) {
        this.userRepository = userRepository;
        this.svgFileRepository = svgFileRepository;
        this.userMapper = userMapper;
        this.currentUserService = currentUserService;
        this.keycloakUserService = keycloakUserService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(
            String username,
            String email,
            Role role,
            Boolean enabled,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        User currentUser = currentUserService.getCurrentUser();
        boolean isAdmin = currentUserService.isAdmin();
        boolean isAgent = currentUserService.isAgent();

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String validSortBy = StringUtils.hasText(sortBy) ? sortBy : "createdAt";
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, validSortBy));

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter out soft-deleted users
            predicates.add(cb.or(cb.isNull(root.get("deleted")), cb.isFalse(root.get("deleted"))));

            // Data scope isolation: AGENT only sees users where agent_id = currentAgentId
            if (!isAdmin && isAgent) {
                predicates.add(cb.equal(root.get("agent").get("id"), currentUser.getId()));
            }

            if (StringUtils.hasText(username)) {
                predicates.add(cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(email)) {
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

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

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username is already taken: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already in use: " + request.getEmail());
        }

        Role assignedRole;
        User assignedAgent = null;

        if (isAgent && !isAdmin) {
            // AGENT can only create USER under own agent scope
            if (request.getRole() != null && request.getRole() != Role.USER) {
                throw new ForbiddenException("Agent cannot create user with role: " + request.getRole());
            }
            assignedRole = Role.USER;
            assignedAgent = currentUser;
        } else {
            // ADMIN can create any role
            assignedRole = request.getRole() != null ? request.getRole() : Role.USER;
            if (request.getAgentId() != null) {
                assignedAgent = userRepository.findById(request.getAgentId())
                        .filter(u -> u.getRole() == Role.AGENT && !u.isDeleted())
                        .orElseThrow(() -> new BadRequestException("Assigned agent not found with id: " + request.getAgentId()));
            }
        }

        boolean enabled = request.getEnabled() != null ? request.getEnabled() : true;

        // Step 1: Create user in Keycloak (Identity Source of Truth)
        String keycloakUserId = keycloakUserService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                assignedRole,
                enabled,
                request.getFullName()
        );

        // Step 2: Create application user in PostgreSQL (Business Profile Source of Truth)
        // With rollback compensation if DB operation fails
        try {
            User user = User.builder()
                    .keycloakUserId(keycloakUserId)
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .fullName(request.getFullName())
                    .phone(request.getPhone())
                    .role(assignedRole)
                    .agent(assignedAgent)
                    .enabled(enabled)
                    .deleted(false)
                    .build();

            User savedUser = userRepository.save(user);
            log.info("[USER_CREATED] Created user: username='{}', id={}, keycloakId='{}', role={}, agentId={}",
                    savedUser.getUsername(), savedUser.getId(), keycloakUserId, savedUser.getRole(),
                    assignedAgent != null ? assignedAgent.getId() : null);

            return userMapper.toUserResponse(savedUser);
        } catch (Exception e) {
            log.error("[COMPENSATION] PostgreSQL save failed for user '{}'. Rolling back Keycloak user: {}",
                    request.getUsername(), keycloakUserId, e);
            try {
                keycloakUserService.deleteUser(keycloakUserId);
                log.info("[COMPENSATION_SUCCESS] Deleted orphaned Keycloak user: {}", keycloakUserId);
            } catch (Exception rollbackEx) {
                log.error("[COMPENSATION_FAILED] Failed to delete orphaned Keycloak user {}: {}",
                        keycloakUserId, rollbackEx.getMessage());
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findScopedUserById(id);

        if (userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new ConflictException("Email is already in use by another user: " + request.getEmail());
        }

        boolean isAdmin = currentUserService.isAdmin();
        boolean isAgent = currentUserService.isAgent();

        Role oldRole = user.getRole();
        boolean oldEnabled = user.isEnabled();

        user.setEmail(request.getEmail());
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        if (isAdmin && request.getRole() != null) {
            user.setRole(request.getRole());
        } else if (isAgent && request.getRole() != null && request.getRole() != Role.USER) {
            throw new ForbiddenException("Agent cannot promote user to role: " + request.getRole());
        }

        if (isAdmin && request.getAgentId() != null) {
            User assignedAgent = userRepository.findById(request.getAgentId())
                    .filter(u -> u.getRole() == Role.AGENT && !u.isDeleted())
                    .orElseThrow(() -> new BadRequestException("Assigned agent not found with id: " + request.getAgentId()));
            user.setAgent(assignedAgent);
        }

        // Sync with Keycloak
        if (StringUtils.hasText(user.getKeycloakUserId())) {
            keycloakUserService.updateUser(user.getKeycloakUserId(), request.getEmail(), request.getEnabled());

            if (oldRole != user.getRole()) {
                keycloakUserService.updateRole(user.getKeycloakUserId(), oldRole, user.getRole());
            }
            if (StringUtils.hasText(request.getPassword())) {
                keycloakUserService.resetPassword(user.getKeycloakUserId(), request.getPassword());
            }
        }

        User updatedUser = userRepository.save(user);
        log.info("[USER_UPDATED] User updated: id={}, username='{}'", updatedUser.getId(), updatedUser.getUsername());

        if (oldRole != updatedUser.getRole()) {
            log.info("[ROLE_CHANGED] User id={} role changed from {} to {}", updatedUser.getId(), oldRole, updatedUser.getRole());
        }
        if (oldEnabled && !updatedUser.isEnabled()) {
            log.info("[USER_DISABLED] User id={} has been disabled", updatedUser.getId());
        }

        return userMapper.toUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
        User user = findScopedUserById(id);

        boolean oldStatus = user.isEnabled();
        user.setEnabled(request.getEnabled());
        User updatedUser = userRepository.save(user);

        if (StringUtils.hasText(user.getKeycloakUserId())) {
            if (request.getEnabled()) {
                keycloakUserService.enableUser(user.getKeycloakUserId());
            } else {
                keycloakUserService.disableUser(user.getKeycloakUserId());
            }
        }

        if (oldStatus && !request.getEnabled()) {
            log.info("[USER_DISABLED] User id={} ({}) has been disabled", user.getId(), user.getUsername());
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

        boolean hasSvg = svgFileRepository.existsByUploadedBy(user);
        boolean hasChildUsers = userRepository.existsByAgent(user);

        // Soft-delete if user has business references (SVGs, child users) to preserve audit/history
        if (hasSvg || hasChildUsers) {
            user.setDeleted(true);
            user.setEnabled(false);
            userRepository.save(user);

            if (StringUtils.hasText(user.getKeycloakUserId())) {
                keycloakUserService.disableUser(user.getKeycloakUserId());
            }
            log.info("[USER_SOFT_DELETED] User soft-deleted to maintain business history: id={}, username='{}', hasSvg={}, hasChildUsers={}",
                    user.getId(), user.getUsername(), hasSvg, hasChildUsers);
            return;
        }

        // Hard-delete if no business references exist
        userRepository.delete(user);
        if (StringUtils.hasText(user.getKeycloakUserId())) {
            keycloakUserService.deleteUser(user.getKeycloakUserId());
        }
        log.info("[USER_DELETED] User hard-deleted successfully: id={}, username='{}'", id, user.getUsername());
    }

    private User findScopedUserById(Long id) {
        User user;
        if (currentUserService.isAdmin()) {
            user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        } else if (currentUserService.isAgent()) {
            Long currentAgentId = currentUserService.getCurrentUser().getId();
            user = userRepository.findByIdAndAgentId(id, currentAgentId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        } else {
            throw new ForbiddenException("Access denied: insufficient privileges");
        }

        if (user.isDeleted()) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }

        return user;
    }
}
