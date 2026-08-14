package com.spms.payment.exception;

public class DuplicatePaymentException extends RuntimeException {

    public DuplicatePaymentException(Long reservationId) {
        super("A successful payment already exists for reservation with id: " + reservationId);
    }
}
