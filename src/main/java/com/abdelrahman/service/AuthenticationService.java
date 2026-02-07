package com.abdelrahman.service;

import com.abdelrahman.dto.AuthenticationRequest;
import com.abdelrahman.dto.AuthenticationResponse;
import com.abdelrahman.dto.RegisterRequest;
import com.abdelrahman.model.User;
import com.abdelrahman.model.UserPrincipal;
import com.abdelrahman.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationService {

    private final UserRepo userRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;


    public AuthenticationResponse register(RegisterRequest request) {

        if (userRepo.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists.");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .age(request.getAge())
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .status(User.UserStatus.ACTIVE)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        userRepo.save(user);

        String token = jwtService.generateToken(new UserPrincipal(user));

        return buildAuthResponse(user, token);
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));


        String token = jwtService.generateToken(new UserPrincipal(user));

        return buildAuthResponse(user, token);
    }

    public AuthenticationResponse refreshToken(String token) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        if (!jwtService.validateToken(token, userPrincipal)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String email = jwtService.extractUsername(token);

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String newToken = jwtService.generateToken(new UserPrincipal(user));

        return buildAuthResponse(user, newToken);
    }

    /// Helper Method
    private AuthenticationResponse buildAuthResponse(User user, String token) {
        return AuthenticationResponse.builder()
                .token(token)
                .type("Bearer")
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .expiresIn(jwtService.getExpirationTime())
                .build();
    }
}