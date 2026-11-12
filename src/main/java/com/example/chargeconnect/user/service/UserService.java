package com.example.chargeconnect.user.service;

import com.example.chargeconnect.exception.DuplicateResourceException;
import com.example.chargeconnect.exception.ResourceNotFoundException;
import com.example.chargeconnect.user.AccountStatus;
import com.example.chargeconnect.user.Role;
import com.example.chargeconnect.user.User;
import com.example.chargeconnect.user.UserRepository;
import com.example.chargeconnect.user.dto.AddRoleRequest;
import com.example.chargeconnect.user.dto.ChangePasswordRequest;
import com.example.chargeconnect.user.dto.UpdateAccountStatusRequest;
import com.example.chargeconnect.user.dto.UpdateProfileRequest;
import com.example.chargeconnect.user.dto.UserProfileResponse;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(String userEmail) {
        User user = getUserByEmail(userEmail);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(String userEmail, UpdateProfileRequest request) {
        User user = getUserByEmail(userEmail);

        if (request.fullName() != null) {
            String trimmedName = request.fullName().trim();
            if (trimmedName.isEmpty()) {
                throw new IllegalArgumentException("Full name cannot be blank");
            }
            user.setFullName(trimmedName);
        }

        if (request.phone() != null) {
            String trimmedPhone = request.phone().trim();
            user.setPhone(trimmedPhone.isEmpty() ? null : trimmedPhone);
        }

        User updated = userRepository.saveAndFlush(user);
        return UserProfileResponse.from(updated);
    }

    @Transactional
    public void changePassword(String userEmail, ChangePasswordRequest request) {
        User user = getUserByEmail(userEmail);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserProfileResponse addRole(String userEmail, AddRoleRequest request) {
        User user = getUserByEmail(userEmail);
        Role roleToAdd = Role.valueOf(request.role().name());

        if (user.getRoles().contains(roleToAdd)) {
            throw new DuplicateResourceException("User already has role " + roleToAdd.name());
        }

        user.addRole(roleToAdd);
        User updated = userRepository.saveAndFlush(user);
        return UserProfileResponse.from(updated);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(UserProfileResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateAccountStatus(
            String adminEmail,
            Long userId,
            UpdateAccountStatusRequest request
    ) {
        User admin = getUserByEmail(adminEmail);
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (target.getId().equals(admin.getId()) && request.status() == AccountStatus.SUSPENDED) {
            throw new IllegalArgumentException("Admins cannot suspend their own account");
        }

        target.setAccountStatus(request.status());
        User updated = userRepository.saveAndFlush(target);
        return UserProfileResponse.from(updated);
    }

    private User getUserByEmail(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

