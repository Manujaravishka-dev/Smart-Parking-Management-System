package com.spms.parking.dto.request;

import com.spms.parking.entity.ParkingSpaceStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateParkingSpaceStatusRequest(
        @NotNull(message = "status is required")
        ParkingSpaceStatus status) {
}
