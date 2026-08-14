package com.spms.vehicle.exception;

public class VehicleNotInsideException extends RuntimeException {

    public VehicleNotInsideException(Long id) {
        super("Vehicle is not currently inside the parking facility: " + id);
    }
}
