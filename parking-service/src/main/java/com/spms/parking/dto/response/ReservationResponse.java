package com.spms.parking.dto.response;

import java.time.LocalDateTime;

import com.spms.parking.entity.Reservation;
import com.spms.parking.entity.ReservationStatus;

public record ReservationResponse(
        Long id,
        Long userId,
        Long vehicleId,
        Long parkingSpaceId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        ReservationStatus status,
        LocalDateTime createdAt) {

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getVehicleId(),
                reservation.getParkingSpaceId(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                reservation.getCreatedAt());
    }
}
