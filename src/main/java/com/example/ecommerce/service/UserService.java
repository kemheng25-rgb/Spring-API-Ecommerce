package com.example.ecommerce.service;

import com.example.ecommerce.dto.UserDTOs;
import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.exception.UnauthorizedException;
import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoderService passwordEncoderService;
    private final AuditLogService auditLogService;

    public UserDTOs.UserResponse registerUser(UserDTOs.UserRegistrationRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        User user = User.builder()
            .email(request.email())
            .passwordHash(passwordEncoderService.encode(request.password()))
            .fullName(request.fullName())
            .phone(request.phone())
            .accountStatus(User.AccountStatus.ACTIVE)
            .emailVerified(false)
            .isBuyer(true)
            .isSeller(false)
            .build();
        
        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }
    
    @Transactional(readOnly = true)
    public UserDTOs.UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        return mapToUserResponse(user);
    }
    
    public UserDTOs.UserResponse authenticate(UserDTOs.UserLoginRequest request) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
            .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoderService.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
            throw new UnauthorizedException("Account is " + user.getAccountStatus().toString().toLowerCase());
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        return mapToUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserDTOs.UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        
        return mapToUserResponse(user);
    }
    
    public UserDTOs.UserResponse updateUserProfile(Long userId, UserDTOs.UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        
        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }
    
    @Transactional(readOnly = true)
    public Page<UserDTOs.UserResponse> getSellers(Pageable pageable) {
        return userRepository.findByIsSeller(true, pageable)
            .map(this::mapToUserResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<UserDTOs.UserResponse> getBuyers(Pageable pageable) {
        return userRepository.findByIsBuyer(true, pageable)
            .map(this::mapToUserResponse);
    }
    
    public void enableSellerRole(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        user.setIsSeller(true);
        userRepository.save(user);
    }
    
    public void suspendUser(Long userId, Long adminUserId, String reason) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setAccountStatus(User.AccountStatus.SUSPENDED);
        userRepository.save(user);
        auditLogService.log(adminUserId, "SUSPEND_USER", "USER", userId, Map.of("reason", reason != null ? reason : ""));
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setAccountStatus(User.AccountStatus.DELETED);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        auditLogService.log(userId, "DELETE_ACCOUNT", "USER", userId, Map.of());
    }
    
    public void updateLastLogin(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    private UserDTOs.UserResponse mapToUserResponse(User user) {
        return new UserDTOs.UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getPhone(),
            user.getAccountStatus().toString(),
            user.getEmailVerified(),
            user.getIsBuyer(),
            user.getIsSeller(),
            user.getCreatedAt().toString()
        );
    }
}
