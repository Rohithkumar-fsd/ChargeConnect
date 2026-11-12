package com.example.chargeconnect.user.controller;

import com.example.chargeconnect.exception.DuplicateResourceException;
import com.example.chargeconnect.exception.GlobalExceptionHandler;
import com.example.chargeconnect.user.dto.AccountStatus;
import com.example.chargeconnect.user.dto.UserProfileResponse;
import com.example.chargeconnect.user.model.Role;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        auth = new UsernamePasswordAuthenticationToken("asha@example.com", null);
    }

    @Test
    void getMyProfileReturnsOk() throws Exception {
        UserProfileResponse response = new UserProfileResponse(
                1L,
                "Asha Rao",
                "asha@example.com",
                "9999999999",
                AccountStatus.ACTIVE,
                List.of(Role.DRIVER),
                Instant.parse("2026-11-12T10:00:00Z"),
                Instant.parse("2026-11-12T10:00:00Z")
        );

        when(userService.getMyProfile("asha@example.com")).thenReturn(response);

        mockMvc.perform(get("/api/users/me").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("asha@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("DRIVER"));
    }

    @Test
    void changePasswordReturnsNoContent() throws Exception {
        doNothing().when(userService).changePassword(eq("asha@example.com"), any());

        String body = """
                {
                  "currentPassword": "OldPassword1!",
                  "newPassword": "NewPassword1!"
                }
                """;

        mockMvc.perform(post("/api/users/me/change-password")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void addRoleRejectsAdminRoleWithBadRequest() throws Exception {
        String body = """
                {
                  "role": "ADMIN"
                }
                """;

        mockMvc.perform(post("/api/users/me/roles")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addRoleReturnsConflictWhenAlreadyAssigned() throws Exception {
        when(userService.addRole(eq("asha@example.com"), any()))
                .thenThrow(new DuplicateResourceException("User already has role HOST"));

        String body = """
                {
                  "role": "HOST"
                }
                """;

        mockMvc.perform(post("/api/users/me/roles")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}

