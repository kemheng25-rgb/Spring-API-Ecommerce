package com.example.ecommerce.service;

import com.example.ecommerce.dto.AddressDTOs;
import com.example.ecommerce.model.Address;
import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.AddressRepository;
import com.example.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddressService")
class AddressServiceTest {

    @Mock private AddressRepository addressRepository;
    @Mock private UserRepository userRepository;

    private AddressService addressService;
    private User user;

    @BeforeEach
    void setUp() {
        addressService = new AddressService(addressRepository, userRepository);
        user = User.builder().id(1L).fullName("Buyer").build();
    }

    private AddressDTOs.AddressCreateRequest request(Boolean isDefault) {
        return new AddressDTOs.AddressCreateRequest(
            "Buyer", "+15551234567", "1 Main St", "Springfield", "IL", "62701", "USA", isDefault, "HOME");
    }

    @Test
    @DisplayName("the first address a user adds becomes the default even if not requested")
    void firstAddressIsDefault() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.countByUserId(1L)).thenReturn(0L);
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        AddressDTOs.AddressResponse response = addressService.createAddress(1L, request(false));

        assertThat(response.isDefault()).isTrue();
    }

    @Test
    @DisplayName("[FIX] adding a second default address unsets the previous default instead of relying on a broken DB constraint")
    void secondDefaultUnsetsFirst() {
        Address existingDefault = Address.builder().id(10L).user(user).isDefault(true).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.countByUserId(1L)).thenReturn(1L);
        when(addressRepository.findByUserIdAndIsDefault(1L, true)).thenReturn(Optional.of(existingDefault));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        addressService.createAddress(1L, request(true));

        assertThat(existingDefault.getIsDefault()).isFalse();
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(Address::getIsDefault);
    }

    @Test
    @DisplayName("deleting the default address promotes another one instead of leaving zero defaults")
    void deletingDefaultPromotesAnother() {
        Address toDelete = Address.builder().id(10L).user(user).isDefault(true).build();
        Address other = Address.builder().id(11L).user(user).isDefault(false).build();

        when(addressRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(toDelete));
        when(addressRepository.findByUserIdOrderByIsDefaultDesc(1L)).thenReturn(List.of(other));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        addressService.deleteAddress(1L, 10L);

        verify(addressRepository).delete(toDelete);
        assertThat(other.getIsDefault()).isTrue();
    }
}
