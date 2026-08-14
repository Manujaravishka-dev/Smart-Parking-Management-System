package com.spms.vehicle.exception;

public class DuplicateVehicleNumberException extends RuntimeException {

    public DuplicateVehicleNumberException(String vehicleNumber) {
        super("Vehicle number is already registered: " + vehicleNumber);
    }
}
