package com.example.demo.service;

import com.example.demo.dto.UserRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.User;
import com.example.demo.model.User.UserStatus;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .age(request.age())
                .status(request.status())
                .build();

        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return toResponse(findUserOrThrow(id));
    }

    public UserResponse update(Long id, UserRequest request) {
        User user = findUserOrThrow(id);

        // If email changed, check uniqueness
        if (!user.getEmail().equals(request.email())
                && userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setAge(request.age());
        user.setStatus(request.status());

        return toResponse(userRepository.save(user));
    }

    public void delete(Long id) {
        User user = findUserOrThrow(id);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findByStatus(UserStatus status) {
        return userRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .toList();
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getAge(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
