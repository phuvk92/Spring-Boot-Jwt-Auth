package com.abdelrahman.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationResponse {

    private String token;
    private String type = "Bearer";
    private String email;
    private String name;
    private String role;
    private Long expiresIn;
}
