package com.example.ecommerce.service;

import com.example.ecommerce.dto.SellerProfileDTOs;
import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.exception.InvalidOperationException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.model.SellerProfile;
import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.SellerProfileRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class SellerProfileService {

    private final SellerProfileRepository sellerProfileRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    /** Workflow 1 (Phase 1): user applies to become a seller; starts UNVERIFIED until admin review. */
    public SellerProfileDTOs.SellerProfileResponse applyAsSeller(Long userId, SellerProfileDTOs.SellerProfileCreateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (sellerProfileRepository.findByUserId(userId).isPresent()) {
            throw new DuplicateResourceException("SellerProfile already exists for user " + userId);
        }
        if (sellerProfileRepository.findByShopName(request.shopName()).isPresent()) {
            throw new DuplicateResourceException("SellerProfile", "shopName", request.shopName());
        }

        SellerProfile profile = SellerProfile.builder()
            .user(user)
            .shopName(request.shopName())
            .shopDescription(request.shopDescription())
            .shopLogoUrl(request.shopLogoUrl())
            .sellerRating(BigDecimal.ZERO)
            .totalProductsSold(0)
            .totalRevenue(BigDecimal.ZERO)
            .verificationStatus(SellerProfile.VerificationStatus.UNVERIFIED)
            .bankAccountEncrypted(request.bankAccountEncrypted())
            .commissionRate(new BigDecimal("10.00"))
            .build();

        SellerProfile saved = sellerProfileRepository.save(profile);

        user.setIsSeller(true);
        userRepository.save(user);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public SellerProfileDTOs.SellerProfileResponse getSellerProfile(Long sellerId) {
        SellerProfile profile = sellerProfileRepository.findById(sellerId)
            .orElseThrow(() -> new ResourceNotFoundException("SellerProfile", sellerId));
        return mapToResponse(profile);
    }

    @Transactional(readOnly = true)
    public SellerProfileDTOs.SellerProfileResponse getSellerProfileByUserId(Long userId) {
        SellerProfile profile = sellerProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("SellerProfile not found for user", userId));
        return mapToResponse(profile);
    }

    @Transactional(readOnly = true)
    public Page<SellerProfileDTOs.SellerProfileResponse> getPendingVerification(Pageable pageable) {
        return sellerProfileRepository.findByVerificationStatus(SellerProfile.VerificationStatus.UNVERIFIED, pageable)
            .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<SellerProfileDTOs.SellerProfileResponse> getTopRated(Pageable pageable) {
        return sellerProfileRepository.findByOrderBySellerRatingDesc(pageable)
            .map(this::mapToResponse);
    }

    public SellerProfileDTOs.SellerProfileResponse updateProfile(Long sellerId, SellerProfileDTOs.SellerProfileUpdateRequest request) {
        SellerProfile profile = sellerProfileRepository.findById(sellerId)
            .orElseThrow(() -> new ResourceNotFoundException("SellerProfile", sellerId));

        profile.setShopDescription(request.shopDescription());
        profile.setShopLogoUrl(request.shopLogoUrl());

        return mapToResponse(sellerProfileRepository.save(profile));
    }

    /** Admin action (Workflow 1 step 3): approve a pending seller application. */
    public SellerProfileDTOs.SellerProfileResponse verifySeller(Long sellerId, Long adminUserId) {
        SellerProfile profile = sellerProfileRepository.findById(sellerId)
            .orElseThrow(() -> new ResourceNotFoundException("SellerProfile", sellerId));

        if (profile.getVerificationStatus() == SellerProfile.VerificationStatus.VERIFIED) {
            throw new InvalidOperationException("Seller is already verified");
        }
        profile.setVerificationStatus(SellerProfile.VerificationStatus.VERIFIED);
        SellerProfile saved = sellerProfileRepository.save(profile);
        auditLogService.log(adminUserId, "VERIFY_SELLER", "SELLER_PROFILE", saved.getId(), Map.of());
        return mapToResponse(saved);
    }

    /** Admin action: reject a pending seller application. */
    public SellerProfileDTOs.SellerProfileResponse rejectSeller(Long sellerId, Long adminUserId) {
        SellerProfile profile = sellerProfileRepository.findById(sellerId)
            .orElseThrow(() -> new ResourceNotFoundException("SellerProfile", sellerId));

        profile.setVerificationStatus(SellerProfile.VerificationStatus.REJECTED);
        SellerProfile saved = sellerProfileRepository.save(profile);
        auditLogService.log(adminUserId, "REJECT_SELLER", "SELLER_PROFILE", saved.getId(), Map.of());
        return mapToResponse(saved);
    }

    /** Admin action (Rule 9): commission can vary by seller. */
    public SellerProfileDTOs.SellerProfileResponse setCommissionRate(Long sellerId, BigDecimal commissionRate) {
        if (commissionRate == null || commissionRate.signum() < 0 || commissionRate.compareTo(new BigDecimal("100")) > 0) {
            throw new InvalidOperationException("Commission rate must be between 0 and 100");
        }
        SellerProfile profile = sellerProfileRepository.findById(sellerId)
            .orElseThrow(() -> new ResourceNotFoundException("SellerProfile", sellerId));

        profile.setCommissionRate(commissionRate);
        return mapToResponse(sellerProfileRepository.save(profile));
    }

    private SellerProfileDTOs.SellerProfileResponse mapToResponse(SellerProfile profile) {
        return new SellerProfileDTOs.SellerProfileResponse(
            profile.getId(),
            profile.getShopName(),
            profile.getShopDescription(),
            profile.getShopLogoUrl(),
            profile.getSellerRating(),
            profile.getTotalProductsSold(),
            profile.getTotalRevenue(),
            profile.getVerificationStatus().toString(),
            profile.getCommissionRate(),
            profile.getShopCreatedAt().toString()
        );
    }
}
