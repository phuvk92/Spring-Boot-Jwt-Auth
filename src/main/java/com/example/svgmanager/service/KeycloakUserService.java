package com.example.svgmanager.service;

import com.example.svgmanager.entity.Role;

public interface KeycloakUserService {

    String createUser(String username, String email, String password, Role role, boolean enabled);

    String createUser(String username, String email, String password, Role role, boolean enabled, String fullName);

    void updateUser(String keycloakUserId, String email, Boolean enabled);

    void deleteUser(String keycloakUserId);

    void enableUser(String keycloakUserId);

    void disableUser(String keycloakUserId);

    void assignRole(String keycloakUserId, Role role);

    void updateRole(String keycloakUserId, Role oldRole, Role newRole);

    void resetPassword(String keycloakUserId, String newPassword);
}
