package com.example.chargeconnect.user.controller;

import com.example.chargeconnect.user.dto.AddRoleRequest;
import com.example.chargeconnect.user.dto.ChangePasswordRequest;
import com.example.chargeconnect.user.dto.UpdateProfileRequest;
import com.example.chargeconnect.user.dto.UserProfileResponse;
import com.example.chargeconnect.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserProfileResponse getMyProfile(Authentication authentication) {
        return userService.getMyProfile(authentication.getName());
    }

    @PatchMapping("/me")
    public UserProfileResponse updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return userService.updateMyProfile(authentication.getName(), request);
    }

    @PostMapping("/me/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(authentication.getName(), request);
    }

    @PostMapping("/me/roles")
    public UserProfileResponse addRole(
            Authentication authentication,
            @Valid @RequestBody AddRoleRequest request
    ) {
        return userService.addRole(authentication.getName(), request);
    }
}

