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
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.mapper.UserMapper;
import com.example.svgmanager.repository.RefreshTokenRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.service.UserService;
import com.example.svgmanager.util.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SvgFileRepository svgFileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            SvgFileRepository svgFileRepository,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.svgFileRepository = svgFileRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
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
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String validSortBy = StringUtils.hasText(sortBy) ? sortBy : "createdAt";
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, validSortBy));

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
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
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username is already taken: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already in use: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : Role.USER)
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("[USER_CREATED] Admin created user: username='{}', id={}, role={}", savedUser.getUsername(), savedUser.getId(), savedUser.getRole());

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new ConflictException("Email is already in use by another user: " + request.getEmail());
        }

        Role oldRole = user.getRole();
        boolean oldEnabled = user.isEnabled();

        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setEnabled(request.getEnabled());

        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        log.info("[USER_UPDATED] User updated: id={}, username='{}'", updatedUser.getId(), updatedUser.getUsername());

        if (oldRole != updatedUser.getRole()) {
            log.info("[ROLE_CHANGED] User id={} role changed from {} to {}", updatedUser.getId(), oldRole, updatedUser.getRole());
        }
        if (oldEnabled && !updatedUser.isEnabled()) {
            log.info("[USER_DISABLED] User id={} has been disabled", updatedUser.getId());
            refreshTokenRepository.revokeAllUserTokens(updatedUser);
        }

        return userMapper.toUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        boolean oldStatus = user.isEnabled();
        user.setEnabled(request.getEnabled());
        User updatedUser = userRepository.save(user);

        if (oldStatus && !request.getEnabled()) {
            log.info("[USER_DISABLED] User id={} ({}) has been disabled", user.getId(), user.getUsername());
            refreshTokenRepository.revokeAllUserTokens(user);
        } else {
            log.info("[USER_UPDATED] User id={} enabled status set to {}", user.getId(), request.getEnabled());
        }

        return userMapper.toUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUserRole(Long id, UpdateUserRoleRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        Role oldRole = user.getRole();
        user.setRole(request.getRole());
        User updatedUser = userRepository.save(user);

        log.info("[ROLE_CHANGED] User id={} ({}) role changed from {} to {}", user.getId(), user.getUsername(), oldRole, request.getRole());
        return userMapper.toUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId.equals(id)) {
            throw new BadRequestException("Admin cannot delete their own account");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check if user has uploaded SVG files
        if (svgFileRepository.existsByUploadedBy(user)) {
            long svgCount = svgFileRepository.countByUploadedBy(user);
            throw new ConflictException("Cannot delete user who owns " + svgCount + " SVG file(s). Please delete or reassign files first.");
        }

        refreshTokenRepository.deleteByUser(user);
        userRepository.delete(user);
        log.info("[USER_DELETED] User deleted successfully: id={}, username='{}'", id, user.getUsername());
    }
}
