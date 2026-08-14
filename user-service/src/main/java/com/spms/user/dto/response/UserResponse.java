package com.spms.user.dto.response;

import java.time.LocalDateTime;

import com.spms.user.entity.Role;
import com.spms.user.entity.User;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
