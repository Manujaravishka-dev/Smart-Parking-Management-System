package com.spms.parking.exception;

public class InvalidReservationTimeException extends RuntimeException {

    public InvalidReservationTimeException() {
        super("startTime must be before endTime");
    }
}
