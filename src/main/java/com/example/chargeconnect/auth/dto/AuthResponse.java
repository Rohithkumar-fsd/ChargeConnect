package com.example.chargeconnect.auth.dto;

public record AuthResponse(
        String token,
        String tokenType,
        String email
) {}

