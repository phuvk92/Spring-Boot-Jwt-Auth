package com.example.svgmanager.service;

import com.example.svgmanager.dto.request.LoginRequest;
import com.example.svgmanager.dto.request.RefreshTokenRequest;
import com.example.svgmanager.dto.request.RegisterRequest;
import com.example.svgmanager.dto.response.AuthResponse;
import com.example.svgmanager.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    UserResponse getCurrentUser();
}
