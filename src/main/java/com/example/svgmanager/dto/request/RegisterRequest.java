package com.example.svgmanager.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request object for user registration")
public class RegisterRequest {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    @Schema(example = "user01", description = "Unique username for the account")
    private String username;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    @Schema(example = "user01@example.com", description = "Unique email address")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
            message = "Password must be at least 8 characters long, contain at least one uppercase letter, one lowercase letter, one number, and one special character"
    )
    @Schema(example = "Password123!", description = "Secure password complying with complexity rules")
    private String password;

    public RegisterRequest() {
    }

    public RegisterRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String username;
        private String email;
        private String password;

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public RegisterRequest build() {
            return new RegisterRequest(username, email, password);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
