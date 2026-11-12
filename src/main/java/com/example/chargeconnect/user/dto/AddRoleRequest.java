package com.example.chargeconnect.user.dto;

import jakarta.validation.constraints.NotNull;

public record AddRoleRequest(
        @NotNull(message = "Role is required")
        SelfAssignableRole role
) {
    public enum SelfAssignableRole {
        DRIVER,
        HOST
    }
}

