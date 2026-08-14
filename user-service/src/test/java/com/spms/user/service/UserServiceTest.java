package com.spms.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.spms.user.dto.request.LoginRequest;
import com.spms.user.dto.request.RegisterRequest;
import com.spms.user.dto.request.UpdateUserRequest;
import com.spms.user.dto.response.LoginResponse;
import com.spms.user.dto.response.UserResponse;
import com.spms.user.entity.Role;
import com.spms.user.entity.User;
import com.spms.user.exception.DuplicateEmailException;
import com.spms.user.exception.InvalidCredentialsException;
import com.spms.user.exception.UserNotFoundException;
import com.spms.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void register_createsUserWithDefaults() {
        RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "password123", null, null);

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserResponse response = userService.register(request);

        assertEquals(1L, response.id());
        assertEquals("john@example.com", response.email());
        assertEquals(Role.DRIVER, response.role());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "password123", null, null);

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_normalizesEmailAndUsesProvidedRole() {
        RegisterRequest request = new RegisterRequest("Jane", "  JANE@Example.COM ", "password123", null, Role.OWNER);

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.register(request);

        assertEquals("jane@example.com", response.email());
        assertEquals(Role.OWNER, response.role());
    }

    @Test
    void login_success() {
        User user = user(1L, "john@example.com", "hashed", Role.DRIVER);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);

        LoginResponse response = userService.login(new LoginRequest("john@example.com", "password123"));

        assertTrue(response.message().contains("success"));
        assertEquals(1L, response.user().id());
    }

    @Test
    void login_wrongPassword_throws() {
        User user = user(1L, "john@example.com", "hashed", Role.DRIVER);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> userService.login(new LoginRequest("john@example.com", "wrong")));
    }

    @Test
    void login_unknownEmail_throws() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> userService.login(new LoginRequest("ghost@example.com", "password123")));
    }

    @Test
    void getUser_found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "john@example.com", "hashed", Role.DRIVER)));

        UserResponse response = userService.getUser(1L);

        assertEquals("john@example.com", response.email());
    }

    @Test
    void getUser_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUser(99L));
    }

    @Test
    void updateUser_updatesFields() {
        User user = user(1L, "john@example.com", "old-hash", Role.DRIVER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest("John Updated", "john@example.com", "newpassword", "1234567890", Role.OWNER);
        UserResponse response = userService.updateUser(1L, request);

        assertEquals("John Updated", response.name());
        assertEquals(Role.OWNER, response.role());
        assertEquals("1234567890", response.phone());
        assertEquals("new-hash", user.getPassword());
    }

    @Test
    void updateUser_changeEmail_conflictThrows() {
        User user = user(1L, "john@example.com", "hashed", Role.DRIVER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        UpdateUserRequest request = new UpdateUserRequest(null, "taken@example.com", null, null, null);

        assertThrows(DuplicateEmailException.class, () -> userService.updateUser(1L, request));
    }

    @Test
    void updateUser_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.updateUser(99L, new UpdateUserRequest("X", null, null, null, null)));
    }

    @Test
    void getBookings_returnsEmptyListForExistingUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "john@example.com", "hashed", Role.DRIVER)));

        assertTrue(userService.getBookings(1L).isEmpty());
    }

    @Test
    void getBookings_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getBookings(99L));
    }

    private User user(Long id, String email, String password, Role role) {
        User user = new User();
        user.setId(id);
        user.setName("John Doe");
        user.setEmail(email);
        user.setPassword(password);
        user.setPhone("1234567890");
        user.setRole(role);
        return user;
    }
}
