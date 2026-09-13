package com.example.svgmanager.service.impl;

import com.example.svgmanager.entity.RefreshToken;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.exception.UnauthorizedException;
import com.example.svgmanager.repository.RefreshTokenRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final long refreshTokenExpirationMillis;

    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            @Value("${app.jwt.refresh-token-expiration:604800000}") long refreshTokenExpirationMillis
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenExpirationMillis = refreshTokenExpirationMillis;
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        refreshTokenRepository.revokeAllUserTokens(user);

        long seconds = refreshTokenExpirationMillis / 1000;
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusSeconds(seconds))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked()) {
            throw new UnauthorizedException("Refresh token was revoked. Please log in again.");
        }
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new UnauthorizedException("Refresh token was expired. Please make a new signin request.");
        }
        return token;
    }

    @Override
    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllUserTokens(user);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        refreshTokenRepository.deleteByUser(user);
    }
}
