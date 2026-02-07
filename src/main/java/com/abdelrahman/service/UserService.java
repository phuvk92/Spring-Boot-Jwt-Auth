package com.abdelrahman.service;

import com.abdelrahman.dto.UserRequest;
import com.abdelrahman.dto.UserResponse;
import com.abdelrahman.exception.ResourceNotFoundException;
import com.abdelrahman.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.abdelrahman.model.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepo userRepo;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserResponse createUser(UserRequest dto) {

        if (userRepo.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .age(dto.getAge())
                .phoneNumber(dto.getPhoneNumber())
                .status(User.UserStatus.ACTIVE)
                .role(User.Role.USER)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        User savedUser = userRepo.save(user);

        return UserResponse.fromEntity(savedUser);
    }


    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepo.findAll()
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User does not exist with ID: " + id));

        return UserResponse.fromEntity(user);
    }


    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found in email: " + email));

        return UserResponse.fromEntity(user);
    }


    @Transactional(readOnly = true)
    public List<UserResponse> searchUsersByName(String name) {
        return userRepo.findByNameContainingIgnoreCase(name)
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }


    public UserResponse updateUser(Long id, UserRequest dto) {

        User user = userRepo.findById(id)
                .orElseThrow(() ->new ResourceNotFoundException("User does not exist with ID: " + id));

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setAge(dto.getAge());
        user.setPhoneNumber(dto.getPhoneNumber());

        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        User updatedUser = userRepo.save(user);

        return UserResponse.fromEntity(updatedUser);
    }


    public UserResponse partialUpdateUser(Long id, UserRequest dto) {

        User user = userRepo.findById(id)
                .orElseThrow(() ->new ResourceNotFoundException("User does not exist with ID: " + id));

        if (dto.getName() != null) user.setName(dto.getName());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getAge() != null) user.setAge(dto.getAge());
        if (dto.getPhoneNumber() != null) user.setPhoneNumber(dto.getPhoneNumber());

        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        User updatedUser = userRepo.save(user);

        return UserResponse.fromEntity(updatedUser);
    }


    public void deleteUser(Long id) {

        if (!userRepo.existsById(id)) {
            throw new ResourceNotFoundException("User does not exist with ID: " + id);
        }

        userRepo.deleteById(id);
    }


    public UserResponse activateUser(Long id) {

        User user = userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User does not exist with ID: " + id));

        user.setStatus(User.UserStatus.ACTIVE);
        user.setEnabled(true);
        User activatedUser = userRepo.save(user);

        return UserResponse.fromEntity(activatedUser);
    }


    public UserResponse deactivateUser(Long id) {

        User user = userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User does not exist with ID: " + id));

        user.setStatus(User.UserStatus.INACTIVE);
        user.setEnabled(false);
        User deactivatedUser = userRepo.save(user);

        return UserResponse.fromEntity(deactivatedUser);
    }


    @Transactional(readOnly = true)
    public Long countActiveUsers() {
        return userRepo.countActiveUsers();
    }
}