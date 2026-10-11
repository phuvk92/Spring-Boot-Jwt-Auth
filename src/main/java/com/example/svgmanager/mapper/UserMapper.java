package com.example.svgmanager.mapper;

import com.example.svgmanager.dto.response.UserResponse;
import com.example.svgmanager.dto.response.UserSummaryResponse;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.repository.UserPartCategoryPermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class UserMapper {

    @Value("${app.device.max-per-user:1}")
    private int defaultMaxDevices = 1;

    private UserPartCategoryPermissionRepository userPartCategoryPermissionRepository;

    public UserMapper() {
    }

    @Autowired(required = false)
    public UserMapper(UserPartCategoryPermissionRepository userPartCategoryPermissionRepository) {
        this.userPartCategoryPermissionRepository = userPartCategoryPermissionRepository;
    }

    public void setUserPartCategoryPermissionRepository(UserPartCategoryPermissionRepository repo) {
        this.userPartCategoryPermissionRepository = repo;
    }

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
        response.setExpirationDate(user.getExpirationDate());
        response.setAllocatedDays(user.getAllocatedDays());
        response.setExpired(user.isExpired());
        response.setMaxDevices(user.getMaxDevices());
        response.setEffectiveMaxDevices(user.getMaxDevices() != null && user.getMaxDevices() > 0
                ? user.getMaxDevices()
                : Math.max(1, defaultMaxDevices));

        if (userPartCategoryPermissionRepository != null && user.getId() != null) {
            List<Long> categoryIds = userPartCategoryPermissionRepository.findPartCategoryIdsByUserId(user.getId());
            response.setPartCategoryIds(categoryIds != null ? categoryIds : Collections.emptyList());
        }

        return response;
    }

    public UserSummaryResponse toUserSummaryResponse(User user) {
        if (user == null) {
            return null;
        }

        Long dealerId = user.getDealer() != null ? user.getDealer().getId() : null;
        String dealerName = user.getDealer() != null ? user.getDealer().getName() : null;
        String dealerCode = user.getDealer() != null ? user.getDealer().getCode() : null;

        return UserSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .dealerId(dealerId)
                .dealerName(dealerName)
                .dealerCode(dealerCode)
                .build();
    }
}
