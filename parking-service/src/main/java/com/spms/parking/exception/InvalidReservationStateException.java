package com.spms.parking.exception;

public class InvalidReservationStateException extends RuntimeException {

    public InvalidReservationStateException(Long id) {
        super("Reservation cannot be modified in its current state: " + id);
    }
}
