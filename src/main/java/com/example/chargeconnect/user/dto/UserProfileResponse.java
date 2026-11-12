package com.example.chargeconnect.user.dto;

import com.example.chargeconnect.user.model.Role;
import com.example.chargeconnect.user.model.User;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record UserProfileResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        AccountStatus accountStatus,
        List<Role> roles,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserProfileResponse from(User user) {
        List<Role> sortedRoles = user.getRoles().stream()
                .sorted(Comparator.comparing(Enum::name))
                .toList();

        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getAccountStatus(),
                sortedRoles,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

