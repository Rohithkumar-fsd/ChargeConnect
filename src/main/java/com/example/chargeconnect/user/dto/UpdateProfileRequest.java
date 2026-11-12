package com.example.chargeconnect.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone
) {}

