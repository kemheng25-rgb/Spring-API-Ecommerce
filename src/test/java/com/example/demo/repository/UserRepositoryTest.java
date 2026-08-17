package com.example.demo.repository;

import com.example.demo.model.User;
import com.example.demo.model.User.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("should save and find user by id")
    void saveAndFindById() {
        User user = User.builder()
                .name("Alice")
                .email("alice@example.com")
                .phone("+123456789")
                .age(30)
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);
        Optional<User> found = userRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("should find user by email")
    void findByEmail() {
        User user = User.builder()
                .name("Bob")
                .email("bob@example.com")
                .phone("+987654321")
                .age(25)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("bob@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Bob");
    }

    @Test
    @DisplayName("should check if email exists")
    void existsByEmail() {
        User user = User.builder()
                .name("Charlie")
                .email("charlie@example.com")
                .phone("+111222333")
                .age(35)
                .status(UserStatus.INACTIVE)
                .build();
        userRepository.save(user);

        assertThat(userRepository.existsByEmail("charlie@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }

    @Test
    @DisplayName("should find users by status")
    void findByStatus() {
        User active = User.builder().name("A").email("a@e.com").phone("+1").age(20).status(UserStatus.ACTIVE).build();
        User inactive = User.builder().name("B").email("b@e.com").phone("+2").age(21).status(UserStatus.INACTIVE).build();
        userRepository.saveAll(List.of(active, inactive));

        List<User> activeUsers = userRepository.findByStatus(UserStatus.ACTIVE);
        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).getName()).isEqualTo("A");
    }

    @Test
    @DisplayName("should search users by keyword")
    void search() {
        User user1 = User.builder().name("John Doe").email("john@e.com").phone("+111").age(28).status(UserStatus.ACTIVE).build();
        User user2 = User.builder().name("Jane Doe").email("jane@e.com").phone("+222").age(32).status(UserStatus.ACTIVE).build();
        userRepository.saveAll(List.of(user1, user2));

        List<User> results = userRepository.search("Doe");
        assertThat(results).hasSize(2);
    }
}
