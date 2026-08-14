package com.spms.parking.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateReservationRequest(
        @NotNull(message = "userId is required")
        @Positive(message = "userId must be positive")
        Long userId,

        @NotNull(message = "vehicleId is required")
        @Positive(message = "vehicleId must be positive")
        Long vehicleId,

        @NotNull(message = "parkingSpaceId is required")
        @Positive(message = "parkingSpaceId must be positive")
        Long parkingSpaceId,

        @NotNull(message = "startTime is required")
        LocalDateTime startTime,

        @NotNull(message = "endTime is required")
        LocalDateTime endTime) {
}
