package com.spms.parking.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateParkingSpaceRequest(
        @Positive(message = "ownerId must be positive")
        Long ownerId,

        @Size(max = 50, message = "spaceNumber must be at most 50 characters")
        String spaceNumber,

        @Size(max = 255, message = "location must be at most 255 characters")
        String location,

        @Size(max = 100, message = "city must be at most 100 characters")
        String city,

        @Size(max = 100, message = "zone must be at most 100 characters")
        String zone,

        @DecimalMin(value = "0.0", message = "pricePerHour must be zero or positive")
        BigDecimal pricePerHour) {
}
