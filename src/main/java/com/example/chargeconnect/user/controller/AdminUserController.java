package com.example.chargeconnect.user.controller;

import com.example.chargeconnect.user.dto.UpdateAccountStatusRequest;
import com.example.chargeconnect.user.dto.UserProfileResponse;
import com.example.chargeconnect.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserProfileResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{userId}")
    public UserProfileResponse getUserById(@PathVariable Long userId) {
        return userService.getUserById(userId);
    }

    @PatchMapping("/{userId}/status")
    public UserProfileResponse updateAccountStatus(
            Authentication authentication,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateAccountStatusRequest request
    ) {
        return userService.updateAccountStatus(authentication.getName(), userId, request);
    }
}

