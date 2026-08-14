package com.spms.user.dto.response;

import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long userId,
        String status,
        LocalDateTime createdAt) {
}
