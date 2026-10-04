package com.example.svgmanager.service;

import com.example.svgmanager.dto.request.CreateUserRequest;
import com.example.svgmanager.dto.request.UpdateUserRequest;
import com.example.svgmanager.dto.request.UpdateUserRoleRequest;
import com.example.svgmanager.dto.request.UpdateUserStatusRequest;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.UserResponse;
import com.example.svgmanager.entity.Role;

public interface UserService {

    default PageResponse<UserResponse> getUsers(
            String username,
            String email,
            Role role,
            Boolean enabled,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        return getUsers(username, email, role, enabled, null, page, size, sortBy, sortDirection);
    }

    PageResponse<UserResponse> getUsers(
            String username,
            String email,
            Role role,
            Boolean enabled,
            String expirationStatus,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    UserResponse getUserById(Long id);

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(Long id, UpdateUserRequest request);

    UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request);

    UserResponse updateUserRole(Long id, UpdateUserRoleRequest request);

    void deleteUser(Long id);

    /** F-57 — máy đã đăng ký của một user trong phạm vi người gọi (ADMIN: mọi user; AGENT: user của mình). */
    java.util.List<com.example.svgmanager.dto.response.UserDeviceResponse> getUserDevices(Long id);

    /** F-57 — gỡ máy: nhả chỗ + chấm dứt phiên trên máy đó. */
    com.example.svgmanager.dto.response.UserDeviceResponse revokeUserDevice(Long id, Long deviceRegistrationId);
}
