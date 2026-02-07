package com.abdelrahman.dto;

import com.abdelrahman.model.User;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Name required")
    @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;

    @NotBlank(message = "Password required")
    @Size(min = 6, message = "The password must be at least 6 characters long.")
    private String password;

    @Min(value = 18, message = "The age must be at least 18 years old.")
    @Max(value = 100, message = "Age must be less than 100")
    private Integer age;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number")
    private String phoneNumber;

    private User.Role role = User.Role.USER;
}