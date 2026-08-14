package com.spms.vehicle.dto.response;

import java.time.LocalDateTime;

import com.spms.vehicle.entity.Vehicle;
import com.spms.vehicle.entity.VehicleStatus;
import com.spms.vehicle.entity.VehicleType;

public record VehicleResponse(
        Long id,
        Long userId,
        String vehicleNumber,
        VehicleType vehicleType,
        String brand,
        String model,
        VehicleStatus status,
        LocalDateTime entryTime,
        LocalDateTime exitTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getUserId(),
                vehicle.getVehicleNumber(),
                vehicle.getVehicleType(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getStatus(),
                vehicle.getEntryTime(),
                vehicle.getExitTime(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt());
    }
}
