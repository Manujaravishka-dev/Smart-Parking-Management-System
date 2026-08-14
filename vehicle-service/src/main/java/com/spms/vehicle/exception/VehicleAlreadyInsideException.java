package com.spms.vehicle.exception;

public class VehicleAlreadyInsideException extends RuntimeException {

    public VehicleAlreadyInsideException(Long id) {
        super("Vehicle is already inside the parking facility: " + id);
    }
}
