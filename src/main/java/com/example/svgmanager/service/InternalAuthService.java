package com.example.svgmanager.service;

import com.example.svgmanager.dto.internal.InternalChangePasswordRequest;
import com.example.svgmanager.dto.internal.InternalChangePasswordResponse;
import com.example.svgmanager.dto.internal.InternalLoginRequest;
import com.example.svgmanager.dto.internal.InternalLoginResponse;
import com.example.svgmanager.dto.internal.InternalLogoutRequest;
import com.example.svgmanager.dto.internal.InternalLogoutResponse;
import com.example.svgmanager.dto.internal.InternalRefreshTokenRequest;

/**
 * Service defining authentication and token management operations for the cutting machine client application.
 */
public interface InternalAuthService {

    InternalLoginResponse login(InternalLoginRequest request);

    InternalLoginResponse refreshToken(InternalRefreshTokenRequest request);

    InternalLogoutResponse logout(InternalLogoutRequest request);

    InternalChangePasswordResponse changePassword(InternalChangePasswordRequest request);
}
