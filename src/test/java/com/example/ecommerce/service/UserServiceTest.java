package com.example.ecommerce.service;

import com.example.ecommerce.dto.UserDTOs;
import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoderService passwordEncoderService;
    @Mock private AuditLogService auditLogService;

    private UserService userService;
    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoderService, auditLogService);
        user = User.builder()
            .id(1L)
            .fullName("Demo Buyer")
            .accountStatus(User.AccountStatus.ACTIVE)
            .isBuyer(true)
            .isSeller(false)
            .build();
    }

    @Test
    @DisplayName("revokeSellerRole turns off the seller flag without touching account status")
    void revokeSellerRoleClearsFlag() {
        user.setIsSeller(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.revokeSellerRole(1L);

        assertThat(user.getIsSeller()).isFalse();
        assertThat(user.getAccountStatus()).isEqualTo(User.AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("reactivateUser restores ACTIVE status and audit-logs the admin action")
    void reactivateUserRestoresActiveStatus() {
        user.setAccountStatus(User.AccountStatus.SUSPENDED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.reactivateUser(1L, 99L);

        assertThat(user.getAccountStatus()).isEqualTo(User.AccountStatus.ACTIVE);
        verify(auditLogService).log(99L, "REACTIVATE_USER", "USER", 1L, Map.of());
    }

    @Test
    @DisplayName("resetPassword stores the encoded password, not the raw one, and audit-logs the admin action")
    void resetPasswordStoresEncodedValue() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoderService.encode("newpassword123")).thenReturn("salt:hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.resetPassword(1L, new UserDTOs.PasswordResetRequest("newpassword123"), 99L);

        assertThat(user.getPasswordHash()).isEqualTo("salt:hash");
        verify(auditLogService).log(99L, "RESET_PASSWORD", "USER", 1L, Map.of());
    }
}
