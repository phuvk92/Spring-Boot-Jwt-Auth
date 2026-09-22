package com.example.svgmanager.mapper;

import com.example.svgmanager.dto.response.UserResponse;
import com.example.svgmanager.dto.response.UserSummaryResponse;
import com.example.svgmanager.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        Long agentId = user.getAgent() != null ? user.getAgent().getId() : null;
        String agentUsername = user.getAgent() != null ? user.getAgent().getUsername() : null;

        return UserResponse.builder()
                .id(user.getId())
                .keycloakUserId(user.getKeycloakUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .agentId(agentId)
                .agentUsername(agentUsername)
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
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
