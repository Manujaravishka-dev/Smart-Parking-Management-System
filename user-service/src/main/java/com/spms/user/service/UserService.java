package com.spms.user.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spms.user.dto.request.LoginRequest;
import com.spms.user.dto.request.RegisterRequest;
import com.spms.user.dto.request.UpdateUserRequest;
import com.spms.user.dto.response.BookingResponse;
import com.spms.user.dto.response.LoginResponse;
import com.spms.user.dto.response.UserResponse;
import com.spms.user.entity.Role;
import com.spms.user.entity.User;
import com.spms.user.exception.DuplicateEmailException;
import com.spms.user.exception.InvalidCredentialsException;
import com.spms.user.exception.UserNotFoundException;
import com.spms.user.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(request.role() != null ? request.role() : Role.DRIVER);

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return new LoginResponse("Login successful", UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return UserResponse.from(findUser(id));
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findUser(id);

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }
        if (request.email() != null && !request.email().isBlank()) {
            String email = normalize(request.email());
            if (!email.equalsIgnoreCase(user.getEmail())) {
                if (userRepository.existsByEmail(email)) {
                    throw new DuplicateEmailException(email);
                }
                user.setEmail(email);
            }
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            user.setPhone(request.phone().trim());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookings(Long userId) {
        findUser(userId);
        return List.of();
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
