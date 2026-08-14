package com.spms.parking.exception;

public class ParkingSpaceNotAvailableException extends RuntimeException {

    public ParkingSpaceNotAvailableException(Long id) {
        super("Parking space is not available: " + id);
    }
}
