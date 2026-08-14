package com.spms.payment.exception;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(Long reservationId) {
        super("Reservation not found with id: " + reservationId);
    }
}
