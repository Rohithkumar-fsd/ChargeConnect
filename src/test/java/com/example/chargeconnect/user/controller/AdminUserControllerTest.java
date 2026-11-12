package com.example.chargeconnect.user.controller;

import com.example.chargeconnect.exception.GlobalExceptionHandler;
import com.example.chargeconnect.user.AccountStatus;
import com.example.chargeconnect.user.Role;
import com.example.chargeconnect.user.dto.UserProfileResponse;
import com.example.chargeconnect.user.service.UserService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AdminUserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        adminAuth = new UsernamePasswordAuthenticationToken("admin@example.com", null);
    }

    @Test
    void getAllUsersReturnsList() throws Exception {
        UserProfileResponse user = new UserProfileResponse(
                1L,
                "Asha Rao",
                "asha@example.com",
                null,
                AccountStatus.ACTIVE,
                List.of(Role.DRIVER),
                Instant.parse("2026-11-12T10:00:00Z"),
                Instant.parse("2026-11-12T10:00:00Z")
        );

        when(userService.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/admin/users").principal(adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("asha@example.com"));
    }

    @Test
    void updateAccountStatusReturnsUpdatedProfile() throws Exception {
        UserProfileResponse suspended = new UserProfileResponse(
                1L,
                "Asha Rao",
                "asha@example.com",
                null,
                AccountStatus.SUSPENDED,
                List.of(Role.DRIVER),
                Instant.parse("2026-11-12T10:00:00Z"),
                Instant.parse("2026-11-12T11:00:00Z")
        );

        when(userService.updateAccountStatus(eq("admin@example.com"), eq(1L), any()))
                .thenReturn(suspended);

        mockMvc.perform(patch("/api/admin/users/1/status")
                        .principal(adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("SUSPENDED"));
    }
}

