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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User driver;
    private User admin;

    @BeforeEach
    void setUp() {
        driver = new User("Asha Rao", "asha@example.com", "9999999999", "hashedOld");
        driver.addRole(Role.DRIVER);
        ReflectionTestUtils.setField(driver, "id", 1L);
        ReflectionTestUtils.invokeMethod(driver, "onCreate");

        admin = new User("Admin User", "admin@example.com", null, "hashedAdmin");
        admin.addRole(Role.ADMIN);
        ReflectionTestUtils.setField(admin, "id", 2L);
        ReflectionTestUtils.invokeMethod(admin, "onCreate");
    }

    @Test
    void getMyProfileReturnsProfileWithSortedRoles() {
        driver.addRole(Role.HOST);
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(driver));

        UserProfileResponse response = userService.getMyProfile("asha@example.com");

        assertEquals(1L, response.id());
        assertEquals("Asha Rao", response.fullName());
        assertEquals(AccountStatus.ACTIVE, response.accountStatus());
        assertEquals(List.of(Role.DRIVER, Role.HOST), response.roles());
    }

    @Test
    void updateMyProfileTrimsFullNameAndClearsBlankPhone() {
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(driver));
        when(userRepository.saveAndFlush(driver)).thenAnswer(invocation -> {
            ReflectionTestUtils.invokeMethod(driver, "onUpdate");
            return driver;
        });

        UserProfileResponse response = userService.updateMyProfile(
                "asha@example.com",
                new UpdateProfileRequest("  Asha K Rao ", "   ")
        );

        assertEquals("Asha K Rao", response.fullName());
        assertNull(response.phone());
        assertNotNull(response.updatedAt());
    }

    @Test
    void updateMyProfileRejectsBlankFullName() {
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(driver));

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateMyProfile("asha@example.com", new UpdateProfileRequest("   ", null))
        );
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void changePasswordVerifiesCurrentPasswordAndEncodesNewPassword() {
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(driver));
        when(passwordEncoder.matches("oldPass123", "hashedOld")).thenReturn(true);
        when(passwordEncoder.matches("newPass123", "hashedOld")).thenReturn(false);
        when(passwordEncoder.encode("newPass123")).thenReturn("hashedNew");

        userService.changePassword(
                "asha@example.com",
                new ChangePasswordRequest("oldPass123", "newPass123")
        );

        assertEquals("hashedNew", driver.getPasswordHash());
        verify(userRepository).save(driver);
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() {
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(driver));
        when(passwordEncoder.matches("wrongPass", "hashedOld")).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.changePassword(
                        "asha@example.com",
                        new ChangePasswordRequest("wrongPass", "newPass123")
                )
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    void addRoleAddsNewRoleAndRejectsDuplicateRole() {
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(driver));
        when(userRepository.saveAndFlush(driver)).thenReturn(driver);

        UserProfileResponse response = userService.addRole(
                "asha@example.com",
                new AddRoleRequest(AddRoleRequest.SelfAssignableRole.HOST)
        );

        assertEquals(List.of(Role.DRIVER, Role.HOST), response.roles());

        assertThrows(
                DuplicateResourceException.class,
                () -> userService.addRole(
                        "asha@example.com",
                        new AddRoleRequest(AddRoleRequest.SelfAssignableRole.HOST)
                )
        );
    }

    @Test
    void updateAccountStatusSuspendsTargetUserAndPreventsSelfSuspension() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.saveAndFlush(driver)).thenReturn(driver);

        UserProfileResponse updated = userService.updateAccountStatus(
                "admin@example.com",
                1L,
                new UpdateAccountStatusRequest(AccountStatus.SUSPENDED)
        );
        assertEquals(AccountStatus.SUSPENDED, updated.accountStatus());

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateAccountStatus(
                        "admin@example.com",
                        2L,
                        new UpdateAccountStatusRequest(AccountStatus.SUSPENDED)
                )
        );
    }

    @Test
    void getUserByIdThrowsNotFoundWhenMissing() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(999L));
    }
}

