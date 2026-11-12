package com.example.chargeconnect.auth.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank String fullName,
        @Email @NotBlank String email,
        String phone,
        @Size(min = 8, max = 72) String password,
        @NotNull UserRole role
) {
    public enum UserRole {
        DRIVER, HOST
    }
}

