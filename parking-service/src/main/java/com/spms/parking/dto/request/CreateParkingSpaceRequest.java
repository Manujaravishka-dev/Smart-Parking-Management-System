package com.spms.parking.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateParkingSpaceRequest(
        @NotNull(message = "ownerId is required")
        @Positive(message = "ownerId must be positive")
        Long ownerId,

        @NotBlank(message = "spaceNumber is required")
        @Size(max = 50, message = "spaceNumber must be at most 50 characters")
        String spaceNumber,

        @NotBlank(message = "location is required")
        @Size(max = 255, message = "location must be at most 255 characters")
        String location,

        @NotBlank(message = "city is required")
        @Size(max = 100, message = "city must be at most 100 characters")
        String city,

        @NotBlank(message = "zone is required")
        @Size(max = 100, message = "zone must be at most 100 characters")
        String zone,

        @NotNull(message = "pricePerHour is required")
        @DecimalMin(value = "0.0", message = "pricePerHour must be zero or positive")
        BigDecimal pricePerHour) {
}
