package com.abdelrahman.controller;

import com.abdelrahman.dto.ApiResponse;
import com.abdelrahman.dto.UserRequest;
import com.abdelrahman.dto.UserResponse;
import com.abdelrahman.model.User;
import com.abdelrahman.model.UserPrincipal;
import com.abdelrahman.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;


    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        UserResponse user = userService.getUserByEmail(email);

        return ResponseEntity.ok(
                ApiResponse.success(user, "The current user was successfully fetched")
        );
    }


    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();

        return ResponseEntity.ok(
                ApiResponse.success(users, "Users successfully retrieved")
        );
    }


    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(@RequestParam(required = false) String name) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers(), "No search keyword provided. Returning all users."));
        }

        List<UserResponse> users = userService.searchUsersByName(name);
        return ResponseEntity.ok(ApiResponse.success(users, "Search completed successfully," + users.size() + " users found."));
    }


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserRequest UserRequest) {
        UserResponse createdUser = userService.createUser(UserRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdUser, "User created successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();

        /**
         *  Allow access if:
         * 1) The requested user ID matches the authenticated user's ID (user accessing his own data)
         * OR
         * 2) The authenticated user has ADMIN role
         */
        if (currentUser.getUser().getId().equals(id) || currentUser.hasRole(User.Role.ADMIN)) {
            UserResponse user = userService.getUserById(id);
            return ResponseEntity.ok(ApiResponse.success(user, "User successfully fetched"));
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("You are not authorized to view this user."));
    }

    /**
     * Best Practice:
     * - Authorization is handled declaratively using @PreAuthorize
     * - Access is granted if:
     * 1) The user has ADMIN role
     * 2) The requested user ID matches the authenticated user's ID
     * Benefits: - Cleaner controller - Better separation of concerns - Easier to read, test, and maintain
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.user.id")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest UserRequest) {
        UserResponse updatedUser = userService.updateUser(id, UserRequest);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "User successfully updated"));

    }


    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> partialUpdateUser(@PathVariable Long id, @RequestBody UserRequest UserRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();

        if (currentUser.getUser().getId().equals(id) || currentUser.hasRole(User.Role.ADMIN)) {
            UserResponse updatedUser = userService.partialUpdateUser(id, UserRequest);

            return ResponseEntity.ok(ApiResponse.success(updatedUser, "User partially updated successfully"));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("You are not authorized to update this user"));
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);

        return ResponseEntity.ok(ApiResponse.success(null, "User successfully deleted"));
    }


    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(@PathVariable Long id) {
        UserResponse activatedUser = userService.activateUser(id);

        return ResponseEntity.ok(
                ApiResponse.success(activatedUser, "The user has been successfully activated.")
        );
    }


    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(@PathVariable Long id) {
        UserResponse deactivatedUser = userService.deactivateUser(id);

        return ResponseEntity.ok(ApiResponse.success(deactivatedUser, "User successfully deactivated"));
    }


    @GetMapping("/stats/active-count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getActiveUsersCount() {
        Long count = userService.countActiveUsers();

        return ResponseEntity.ok(ApiResponse.success(count, "Number of active users"));
    }
}