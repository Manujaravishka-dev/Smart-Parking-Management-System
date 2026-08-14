package com.spms.parking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.spms.parking.entity.ParkingSpace;
import com.spms.parking.entity.ParkingSpaceStatus;

public record ParkingSpaceResponse(
        Long id,
        Long ownerId,
        String spaceNumber,
        String location,
        String city,
        String zone,
        BigDecimal pricePerHour,
        ParkingSpaceStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ParkingSpaceResponse from(ParkingSpace space) {
        return new ParkingSpaceResponse(
                space.getId(),
                space.getOwnerId(),
                space.getSpaceNumber(),
                space.getLocation(),
                space.getCity(),
                space.getZone(),
                space.getPricePerHour(),
                space.getStatus(),
                space.getCreatedAt(),
                space.getUpdatedAt());
    }
}
