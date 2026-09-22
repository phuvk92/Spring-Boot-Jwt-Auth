package com.example.svgmanager.service;

import com.example.svgmanager.dto.request.ChangePasswordRequest;
import com.example.svgmanager.dto.request.LoginRequest;
import com.example.svgmanager.dto.request.RefreshTokenRequest;
import com.example.svgmanager.dto.response.AuthResponse;
import com.example.svgmanager.dto.response.MessageResponse;
import com.example.svgmanager.dto.response.UserResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    UserResponse getCurrentUser();
    MessageResponse changePassword(ChangePasswordRequest request);
}
