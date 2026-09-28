package com.example.svgmanager.mapper;

import com.example.svgmanager.dto.response.UserResponse;
import com.example.svgmanager.dto.response.UserSummaryResponse;
import com.example.svgmanager.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    @Value("${app.device.max-per-user:1}")
    private int defaultMaxDevices = 1;

    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        Long agentId = user.getAgent() != null ? user.getAgent().getId() : null;
        String agentUsername = user.getAgent() != null ? user.getAgent().getUsername() : null;

        Long dealerId = user.getDealer() != null ? user.getDealer().getId() : null;
        String dealerName = user.getDealer() != null ? user.getDealer().getName() : null;
        String dealerCode = user.getDealer() != null ? user.getDealer().getCode() : null;

        UserResponse response = UserResponse.builder()
                .id(user.getId())
                .keycloakUserId(user.getKeycloakUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .agentId(agentId)
                .agentUsername(agentUsername)
                .dealerId(dealerId)
                .dealerName(dealerName)
                .dealerCode(dealerCode)
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
        response.setMaxDevices(user.getMaxDevices());
        response.setEffectiveMaxDevices(user.getMaxDevices() != null && user.getMaxDevices() > 0
                ? user.getMaxDevices()
                : Math.max(1, defaultMaxDevices));
        return response;
    }

    public UserSummaryResponse toUserSummaryResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
