package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.request.LoginRequest;
import com.example.svgmanager.dto.request.RefreshTokenRequest;
import com.example.svgmanager.dto.request.RegisterRequest;
import com.example.svgmanager.dto.response.AuthResponse;
import com.example.svgmanager.dto.response.UserResponse;
import com.example.svgmanager.dto.response.UserSummaryResponse;
import com.example.svgmanager.entity.RefreshToken;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.ConflictException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.exception.UnauthorizedException;
import com.example.svgmanager.mapper.UserMapper;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.security.JwtTokenProvider;
import com.example.svgmanager.security.UserPrincipal;
import com.example.svgmanager.service.AuthService;
import com.example.svgmanager.service.RefreshTokenService;
import com.example.svgmanager.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
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
                .role(Role.USER)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("[USER_CREATED] New user registered successfully: username='{}', id={}", savedUser.getUsername(), savedUser.getId());

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (!user.isEnabled()) {
                log.warn("[LOGIN_FAILED] User '{}' is disabled", request.getUsername());
                throw new DisabledException("User account is disabled");
            }

            String accessToken = jwtTokenProvider.generateTokenFromUser(userPrincipal);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            log.info("[LOGIN_SUCCESS] User '{}' logged in successfully", request.getUsername());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getExpirationInSeconds())
                    .user(UserSummaryResponse.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .role(user.getRole())
                            .build())
                    .build();

        } catch (BadCredentialsException ex) {
            log.warn("[LOGIN_FAILED] Invalid credentials for username '{}'", request.getUsername());
            throw ex;
        }
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenString = request.getRefreshToken();
        RefreshToken refreshToken = refreshTokenService.findByToken(tokenString)
                .map(refreshTokenService::verifyExpiration)
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found or invalid"));

        User user = refreshToken.getUser();
        if (!user.isEnabled()) {
            throw new UnauthorizedException("User account is disabled");
        }

        UserPrincipal principal = UserPrincipal.create(user);
        String newAccessToken = jwtTokenProvider.generateTokenFromUser(principal);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationInSeconds())
                .user(UserSummaryResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found with id: " + currentUserId));
        return userMapper.toUserResponse(user);
    }
}
