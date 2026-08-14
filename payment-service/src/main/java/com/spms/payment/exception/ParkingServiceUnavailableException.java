package com.spms.payment.exception;

public class ParkingServiceUnavailableException extends RuntimeException {

    public ParkingServiceUnavailableException() {
        super("Parking Service is currently unavailable");
    }
}
