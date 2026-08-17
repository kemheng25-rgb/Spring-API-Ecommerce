package com.example.ecommerce.service;

import com.example.ecommerce.dto.AddressDTOs;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.model.Address;
import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.AddressRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressDTOs.AddressResponse createAddress(Long userId, AddressDTOs.AddressCreateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        boolean noAddressesYet = addressRepository.countByUserId(userId) == 0;
        boolean wantsDefault = Boolean.TRUE.equals(request.isDefault()) || noAddressesYet;

        if (wantsDefault) {
            clearExistingDefault(userId);
        }

        Address.AddressType type = request.addressType() != null
            ? Address.AddressType.valueOf(request.addressType())
            : Address.AddressType.HOME;

        Address address = Address.builder()
            .user(user)
            .fullName(request.fullName())
            .phone(request.phone())
            .streetAddress(request.streetAddress())
            .city(request.city())
            .stateProvince(request.stateProvince())
            .postalCode(request.postalCode())
            .country(request.country())
            .isDefault(wantsDefault)
            .addressType(type)
            .build();

        return mapToResponse(addressRepository.save(address));
    }

    @Transactional(readOnly = true)
    public List<AddressDTOs.AddressResponse> getAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDesc(userId).stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public AddressDTOs.AddressResponse getAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
        return mapToResponse(address);
    }

    public AddressDTOs.AddressResponse updateAddress(Long userId, Long addressId, AddressDTOs.AddressUpdateRequest request) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));

        if (Boolean.TRUE.equals(request.isDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            clearExistingDefault(userId);
            address.setIsDefault(true);
        } else if (Boolean.FALSE.equals(request.isDefault())) {
            address.setIsDefault(false);
        }

        address.setFullName(request.fullName());
        address.setPhone(request.phone());
        address.setStreetAddress(request.streetAddress());
        address.setCity(request.city());
        address.setStateProvince(request.stateProvince());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country());

        return mapToResponse(addressRepository.save(address));
    }

    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));

        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        addressRepository.delete(address);

        // Promote another address to default so checkout never has zero eligible defaults.
        if (wasDefault) {
            addressRepository.findByUserIdOrderByIsDefaultDesc(userId).stream()
                .findFirst()
                .ifPresent(next -> {
                    next.setIsDefault(true);
                    addressRepository.save(next);
                });
        }
    }

    /**
     * "One default per user" is enforced here, not by a DB constraint - see the note on
     * Address.java. Read-then-write is safe because AddressService methods are @Transactional
     * and addresses are a low-write-contention entity (one user editing their own addresses).
     */
    private void clearExistingDefault(Long userId) {
        addressRepository.findByUserIdAndIsDefault(userId, true)
            .ifPresent(current -> {
                current.setIsDefault(false);
                addressRepository.save(current);
            });
    }

    private AddressDTOs.AddressResponse mapToResponse(Address address) {
        return new AddressDTOs.AddressResponse(
            address.getId(),
            address.getFullName(),
            address.getPhone(),
            address.getStreetAddress(),
            address.getCity(),
            address.getStateProvince(),
            address.getPostalCode(),
            address.getCountry(),
            address.getIsDefault(),
            address.getAddressType().toString()
        );
    }
}
