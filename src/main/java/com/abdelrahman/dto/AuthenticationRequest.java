package com.abdelrahman.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationRequest {

    @NotBlank(message = "Email Required")
    @Email(message = "Invalid Email")
    private String email;

    @NotBlank(message = "Password Required")
    @Size(min = 6, message = "The password must be at least 6 characters long.")
    private String password;
}