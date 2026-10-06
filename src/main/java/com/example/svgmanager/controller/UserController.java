package com.example.svgmanager.controller;

import com.example.svgmanager.dto.request.CreateUserRequest;
import com.example.svgmanager.dto.request.UpdateUserRequest;
import com.example.svgmanager.dto.request.UpdateUserRoleRequest;
import com.example.svgmanager.dto.request.UpdateUserStatusRequest;
import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.UserResponse;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Management", description = "Operations for managing user accounts, scoped by Agent or unrestricted for Admin")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List users with filters and pagination", description = "Returns a paginated list of users. ADMIN sees all, AGENT sees own users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or AGENT role", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<UserResponse>> getUsers(
            @Parameter(description = "Filter by username (partial match)") @RequestParam(required = false) String username,
            @Parameter(description = "Filter by search query (username/displayName)") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by email (partial match)") @RequestParam(required = false) String email,
            @Parameter(description = "Filter by exact role (ADMIN, AGENT, USER)") @RequestParam(required = false) Role role,
            @Parameter(description = "Filter by enabled status") @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "Filter by expiration status (ALL, VALID, EXPIRED, NO_EXPIRATION)") @RequestParam(required = false) String expirationStatus,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC, DESC)") @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        String querySearch = org.springframework.util.StringUtils.hasText(search) ? search : username;
        PageResponse<UserResponse> response = userService.getUsers(querySearch, email, role, enabled, expirationStatus, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user details by ID", description = "Retrieves specific user information within role scope.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create a new user", description = "Creates a new user. AGENT creates USER under own scope; ADMIN creates any role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Username or email already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user details", description = "Updates email, enabled status, and optionally password within allowed scope.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email conflict with another user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Enable/disable user account", description = "Updates user enabled status within allowed scope.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User status updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        UserResponse response = userService.updateUserStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change user role", description = "Assigns a new role to the user. ADMIN only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User role updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        UserResponse response = userService.updateUserRole(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user account", description = "Deletes a user account. Cannot delete self or user with uploaded SVGs.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Attempting to delete self",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "User owns SVG files that must be removed first",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/devices")
    @Operation(summary = "List registered devices of a user (F-57)",
            description = "Máy đã đăng ký của tài khoản, máy đang hoạt động trước. ADMIN: mọi user; AGENT: user trong phạm vi của mình.")
    public ResponseEntity<java.util.List<com.example.svgmanager.dto.response.UserDeviceResponse>> getUserDevices(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserDevices(id));
    }

    @DeleteMapping("/{id}/devices/{deviceId}")
    @Operation(summary = "Revoke a registered device (F-57)",
            description = "Gỡ máy: nhả chỗ trong giới hạn và chấm dứt phiên đang chạy trên máy đó — app cắt nhận 401 SESSION_REVOKED ở request kế tiếp.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device revoked (idempotent)"),
            @ApiResponse(responseCode = "404", description = "User or device not found in caller's scope",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<com.example.svgmanager.dto.response.UserDeviceResponse> revokeUserDevice(
            @PathVariable Long id,
            @PathVariable Long deviceId
    ) {
        return ResponseEntity.ok(userService.revokeUserDevice(id, deviceId));
    }
}
