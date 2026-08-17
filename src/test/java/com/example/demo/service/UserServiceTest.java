package com.example.demo.service;

import com.example.demo.dto.UserRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.User;
import com.example.demo.model.User.UserStatus;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    @DisplayName("should create user successfully")
    void createSuccess() {
        UserRequest request = new UserRequest("Alice", "alice@e.com", "+123456789", 30, UserStatus.ACTIVE);

        when(userRepository.existsByEmail("alice@e.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            return User.builder()
                    .id(1L)
                    .name(u.getName())
                    .email(u.getEmail())
                    .phone(u.getPhone())
                    .age(u.getAge())
                    .status(u.getStatus())
                    .build();
        });

        UserResponse response = userService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Alice");
        assertThat(response.email()).isEqualTo("alice@e.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("should throw when email already exists")
    void createDuplicateEmail() {
        UserRequest request = new UserRequest("Alice", "alice@e.com", "+123456789", 30, UserStatus.ACTIVE);
        when(userRepository.existsByEmail("alice@e.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("should find all users")
    void findAll() {
        List<User> users = List.of(
                User.builder().id(1L).name("A").email("a@e.com").phone("+1").age(20).status(UserStatus.ACTIVE).build(),
                User.builder().id(2L).name("B").email("b@e.com").phone("+2").age(25).status(UserStatus.INACTIVE).build()
        );
        when(userRepository.findAll()).thenReturn(users);

        List<UserResponse> responses = userService.findAll();

        assertThat(responses).hasSize(2);
    }

    @Test
    @DisplayName("should find user by id")
    void findById() {
        User user = User.builder().id(1L).name("Alice").email("a@e.com").phone("+1").age(30).status(UserStatus.ACTIVE).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.findById(1L);

        assertThat(response.name()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("should throw when user not found by id")
    void findByIdNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("should update user")
    void updateSuccess() {
        User existing = User.builder()
                .id(1L).name("Old").email("old@e.com").phone("+1").age(20).status(UserStatus.ACTIVE)
                .build();
        UserRequest updateReq = new UserRequest("New", "new@e.com", "+2", 25, UserStatus.INACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("new@e.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse response = userService.update(1L, updateReq);

        assertThat(response.name()).isEqualTo("New");
        assertThat(response.status()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    @DisplayName("should delete user")
    void deleteSuccess() {
        User user = User.builder().id(1L).name("A").email("a@e.com").phone("+1").age(20).status(UserStatus.ACTIVE).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("should throw when deleting non-existent user")
    void deleteNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
