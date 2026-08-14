package com.spms.parking.exception;

public class ParkingSpaceNotFoundException extends RuntimeException {

    public ParkingSpaceNotFoundException(Long id) {
        super("Parking space not found with id: " + id);
    }
}
