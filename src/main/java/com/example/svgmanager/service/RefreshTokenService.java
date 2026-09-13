package com.example.svgmanager.service;

import com.example.svgmanager.entity.RefreshToken;
import com.example.svgmanager.entity.User;

import java.util.Optional;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user);

    RefreshToken verifyExpiration(RefreshToken token);

    Optional<RefreshToken> findByToken(String token);

    void revokeToken(String token);

    void revokeAllUserTokens(User user);

    void deleteByUserId(Long userId);
}
