package com.spms.vehicle.dto.request;

import com.spms.vehicle.entity.VehicleType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegisterVehicleRequest(
        @NotNull(message = "userId is required")
        @Positive(message = "userId must be positive")
        Long userId,

        @NotBlank(message = "vehicleNumber is required")
        @Pattern(regexp = "^[A-Za-z0-9\\s-]{2,20}$", message = "vehicleNumber must be 2-20 characters of letters, digits, spaces or hyphens")
        String vehicleNumber,

        @NotNull(message = "vehicleType is required")
        VehicleType vehicleType,

        @NotBlank(message = "brand is required")
        @Size(max = 100, message = "brand must be at most 100 characters")
        String brand,

        @NotBlank(message = "model is required")
        @Size(max = 100, message = "model must be at most 100 characters")
        String model) {
}
