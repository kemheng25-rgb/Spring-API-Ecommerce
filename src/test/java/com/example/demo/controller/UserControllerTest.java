package com.example.demo.controller;

import com.example.demo.dto.UserRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.model.User.UserStatus;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@DisplayName("UserController")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private final UserResponse sampleResponse = new UserResponse(
            1L, "Alice", "alice@e.com", "+123456789",
            30, UserStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now()
    );

    @Test
    @DisplayName("POST /api/v1/users → 201")
    void create() throws Exception {
        UserRequest request = new UserRequest("Alice", "alice@e.com", "+123456789", 30, UserStatus.ACTIVE);
        when(userService.create(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    @DisplayName("POST /api/v1/users with invalid body → 400")
    void createValidationFails() throws Exception {
        String invalidJson = """
                {"name": "", "email": "bad", "phone": "x", "age": -1, "status": "INVALID"}
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/users → 200")
    void findAll() throws Exception {
        when(userService.findAll()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Alice"));
    }

    @Test
    @DisplayName("GET /api/v1/users?status=ACTIVE → 200")
    void findByStatus() throws Exception {
        when(userService.findByStatus(UserStatus.ACTIVE)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/users").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/users/1 → 200")
    void findById() throws Exception {
        when(userService.findById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@e.com"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/1 → 200")
    void update() throws Exception {
        UserRequest request = new UserRequest("Bob", "bob@e.com", "+987654321", 28, UserStatus.INACTIVE);
        UserResponse updated = new UserResponse(
                1L, "Bob", "bob@e.com", "+987654321",
                28, UserStatus.INACTIVE, LocalDateTime.now(), LocalDateTime.now());
        when(userService.update(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bob"));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/1 → 204")
    void deleteUser() throws Exception {
        doNothing().when(userService).delete(1L);

        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isNoContent());
    }
}
