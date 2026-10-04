package com.example.svgmanager.exception;

public class UserAccountExpiredException extends UnauthorizedException {

    public UserAccountExpiredException() {
        this("User account has expired");
    }

    public UserAccountExpiredException(String message) {
        super(message, ErrorCodes.USER_ACCOUNT_EXPIRED);
    }
}
